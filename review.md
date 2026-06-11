# 通关复盘 · 可复用心法

每过一关追加一节。只记**能复用一辈子的认知**——不是这次怎么改的,而是下次遇到同类问题能直接套的判断。
更细的实操/踩坑流水账在记忆 `project-springai-backlog.md`,这里只留精华。

---

## L2 · 运行时动态工具(行为外置成数据 + 外部 JSON 加载)

> 一句话:把工具的"行为"从编译进字节码的 lambda,变成一句能被解释器当场执行的字符串,再把整张表搬出源码。名字/描述/行为三样全变数据,加工具只改 JSON 不重编译。

1. **SpEL:`#a` 是变量,裸 `a` 是根对象的属性**
   `setVariable("a", ..)` 设的是变量,公式里必须写 `#a` 去取;裸写 `a` 会被当成"根对象身上的属性 a",没给根对象时根是 null → `EL1007E: Property or field 'a' cannot be found on null`。
   👉 以后写 `@Cacheable(key=...)`、`@PreAuthorize`、`@Value` 里的 SpEL 全是这套规则。

2. **SpEL 的 `^` 就是乘方运算符**
   不是位运算异或。`#a ^ #b` = a 的 b 次方(栈里 `OperatorPower` 实证)。

3. **`TypeReference` 存在,是因为泛型擦除**
   运行时 `List<MathOp>` 和 `List<String>` 长得一样(泛型信息被擦掉),所以写不出 `List<MathOp>.class`。用匿名子类 `new TypeReference<List<MathOp>>(){}` 把泛型类型"冻"进 class 信息,Jackson 才知道数组元素要造成 `MathOp`。
   👉 凡是要把 JSON 反序列化成"带泛型的集合"(List/Map<K,V>),都用 TypeReference,不能用 `.class`。

4. **stream 是惰性管道:开龙头 ≠ 喝水**
   `getInputStream()` 只是接好一根通到文件的管子,一个字节都还没读。真正读取+解析发生在 `readValue`。而且 Jackson 按 JSON 语法符号 `[ { } , ]` 解析,**不按行**——把整个 JSON 压成一行,结果一模一样。"换行/缩进/空格"是给人看的装饰,机器无视。
   👉 "定位文件 → 给字节流 → 解析成对象"是三件分开的事;读文件/读网络/读数据库全是这套惰性管道思想。

5. **无状态可复用 / 有状态必须每次 new**
   `SpelExpressionParser` 无状态(谁来都一样翻译)→ 做成字段,造一次复用。
   `StandardEvaluationContext` 有状态(会被写上 `a=3,b=5`)→ 每次调用 new 一个,共用会"串味"。
   👉 这是判断"该共享一个实例还是每次新建"的通用模板:看它有没有会被改写的内部状态。

---

## L2+ · 动态工具热更新(外部活文件 + /reload 端点)

> 一句话:把数据源从"构建时的 classpath 快照"换成"磁盘上的活文件",再加个手动重读开关。改 JSON → 打 /reload → 新工具即时生效,进程不重启。

1. **classpath 资源是"构建时快照",不是"活文件"**
   `src/main/resources` 下的文件编译时被复制进 `target/classes`(打包后进 jar)。`ClassPathResource` 读的是那个**副本**,改源码原件不重编译不生效;而且 `@PostConstruct` 只在启动那一刻读一次。想运行时可变,数据必须放在**进程外的磁盘文件**上,读那个 File。
   👉 "配置/数据要能热改" ⟹ 别塞 classpath,放外部目录读 File。

2. **接口加一个方法 = 强制所有实现类兑现**
   往 `interface` 加一个抽象方法,所有 `implements` 它的类立刻编译报错("does not override abstract method"),逼你每个实现都补上。这是接口"契约"的保护力,不是麻烦。配合 `@Override`(拼错方法名时编译期报警)双保险。
   👉 想确保"一组并列实现都不漏某能力",就把那能力提到接口里当抽象方法。

3. **外部覆盖 + 内置兜底(配置层叠模式)**
   `reload()` 先看外部活文件在不在:在就读它(可改的覆盖层),不在就回退读 classpath 那份种子(打底默认)。好处:哪怕运维忘了放外部文件,app 靠种子也能起来,绝不让缺一个文件搞崩启动。
   👉 跟 `spring.config.import` 列表"专属覆盖打底"、CSS 后规则盖前规则同一个脑回路。

4. **一个 @PostConstruct 方法,启动时调一次 + 端点复用**
   把加载逻辑抽成 `reload()`,`@PostConstruct` 挂它(启动自动读一次),controller 端点也调它(想刷新时再读)。**同一方法两处复用**。⚠️ **一个类只该有一个 @PostConstruct**:多个都会被 Spring 全调一遍,若都写 `ops=...` 则谁后跑谁覆盖、顺序不保证 → 随机 bug。旧方法别留着,删干净(同"同名重载/死代码并存"的病根)。

5. **build(组装) ≠ call(执行)**
   `buildTools()` 只是遍历数据表、把每个工具**造成 ToolCallback 对象摆上货架**,一次都没执行;真正执行(跑 lambda/SpEL)发生在**以后聊天时模型选中某工具**那一刻。端点里 `buildTools()` 纯粹是借造好的对象拿名字(`getToolDefinition().name()`)当回执,造完就扔。
   👉 "造出来待命"和"被调用一次"是两件事,看到 build/construct/register 别脑补成"跑了"。

---

## L3 · MCP(跨进程,工具代码住在别的程序里)

> 一句话:工具的真实代码不在你的进程里,而在另一个独立程序(MCP Server)。你的 Agent(MCP Client)启动时把它当子进程拉起、握手、发现它的工具,聊天时模型调用 → 实际执行发生在那个外部进程。行为彻底外置,你只配了个"用什么命令启动它"。

1. **判断哪层该自己写、哪层该用库(比 MCP 本身更值钱)**
   成熟的标准协议(MCP 的 JSON-RPC/STDIO 握手)= 别手写,用库,等于手写 HTTP 客户端;**理解协议 + 写集成胶水 + 能 debug = 高 ROI**。用库 ≠ 当黑箱:可以 `javap`/翻 jar 看它造了哪些 bean、吃哪些配置,把黑箱拆成白箱再用。
   👉 遇到任何"要不要自己实现 X":X 是行业标准协议/基础设施→用库并读懂;X 是你的业务逻辑→自己写。

2. **MCP 两端:Client(消费方)vs Server(暴露方)**
   Client = 我去用别人的工具(我配置"启动谁、连谁");Server = 我把自己的工具暴露给别人。要"我的 Agent 用外部工具"就是 Client 方向。传输有 STDIO(把对方当子进程,管道通信)和 SSE/HTTP(对方是个网络服务)两种。

3. **统一插头 ToolCallback 的回报**
   外部 MCP 工具,经 `ToolCallbackProvider.getToolCallbacks()` 拿到的还是 `ToolCallback[]`——跟你自己 `buildTools()` 造的、跟 @Tool 方法的,**到 ChatClient 这层完全平权**,都用 `.toolCallbacks(...)` 挂。前面每关死磕 ToolCallback 这层抽象,在这里兑现:自己的工具和别人进程的工具无差别。

4. **外部进程有它自己的安全边界**
   filesystem server 自带白名单沙箱(只让访问启动时指定的目录),模型猜了个目录外路径 → server 回 `Access denied`。这跟你给 LocalFileTools 写的白名单是同一个 default-deny 思想,只不过这次守门的是别人的进程。**调外部工具失败,先分清是"集成坏了"还是"对方的规则拒了你"**——后者不是 bug。

5. **Agent 自主纠错链(亲眼见证)**
   模型猜路径→被拒→自己调 `list_allowed_directories` 问规则→学到允许目录→重调 `list_directory` 成功。多个外部工具被模型串成一条自我修正链,无需你编排。这就是 agentic loop 在真实外部工具上的样子。

6. **Windows 跑 npx 的坑**:`command: npx` 直接喂 ProcessBuilder 会 `CreateProcess error=2`(npx 其实是 npx.cmd 脚本);用 `command: cmd` + `args: [/c, npx, ...]` 让真 exe `cmd` 去解释执行。

---

## 多 Agent · 双 Agent 协作(起草员 + 审查员)

> 一句话:一个 Agent = 一个 ChatClient + 一句 system 角色提示词。多 Agent = 造多个不同角色的 ChatClient;编排 = 纯 Java 把 A 的输出喂进 B 的 user 输入。

1. **Agent 的本质 = ChatClient + system 角色**
   `builder.defaultSystem("你是…")` 那句就是给 Agent 定人格。要多 Agent 就多造几个 ChatClient bean,每个不同 `defaultSystem`。编排不需要新框架,就是普通 Java:`String draft = A.prompt(q).call().content();` 然后把 draft 拼进 B 的输入。

2. **多个同类型 bean 共存:变量名 = bean 名**
   加了 drafterAgent/reviewerAgent 后容器有 3 个 ChatClient。`@Autowired` 先按类型找,多个时**按变量名匹配 bean 名**:字段叫 `chatClient` 就命中名为 `chatClient` 的 bean。所以新 bean 起不同名 + 注入时变量名对齐,就不会撞。(autoconfigured `ChatClient.Builder` 是 prototype 作用域,每个 @Bean 方法拿到全新的,各设各的 system 不互相覆盖。)

3. **`.call()` vs `.stream()`**
   `.stream().content()` → `Flux<String>` 流式逐字(聊天 SSE 用);`.call().content()` → 一次性完整 `String`(编排用,要把整段喂下一个 Agent 必须等齐)。`prompt(x)` = `prompt().user(x)` 的快捷版;要再挂 advisors/tools/改 system 才用长版 `prompt().user(x).xxx()`。`.user()` 是库给的方法(设"用户那句话"),不用自己声明,跟 `.call()`/`.content()` 同源。

4. **提示词 = 程序;歧义 = bug**
   起草员初版提示 "write a draft of user's question" 有歧义→模型把问题重写了一遍而非作答;改成"直接写出一份简洁的**答案**初稿"→正常。**多 Agent 质量几乎全押在每个角色提示词清不清楚**,不押在中英文。语言上:大模型中英都行;你的 qwen2.5:7b 是中文母语级,中文不输甚至更顺;**一条提示词别混语言(7B 小模型对语言跳变敏感),提示词语言对齐想要的输出语言。**

5. **多 Agent 的价值不是恒定的——看"第一遍有多容易错"**
   初稿已经很好时,审查员加的价值有限(只润色),反而多花一倍时间。简单任务单 Agent 就够;**高风险/复杂任务(执行代码、多步推理、对外发布内容)第一遍易翻车,审查员才真救命**。判断力同"不是所有工具都该结构化返回":别无脑上,在"第一遍不可靠"处上。

---

## 多 Agent · 编排(主管自己挑专家:agent-as-tool)

> 一句话:把每个专家 Agent 包装成一个 @Tool,交给一个"主管 Agent";主管的 agentic loop 自己根据任务决定调哪个专家。控制权从你的代码转移到模型。

1. **流水线 vs 编排:谁决定流程**
   流水线(双 Agent)= 你的 Java 代码写死"先 A 后 B",控制权在你。编排 = 主管 Agent 拿到任务自己判断派给谁,控制权在模型。区别全在一行 `.tools(agentTools)`:有它,调不调/调哪个/调几个由模型定;没它就是你手动 `.call()`。

2. **agent-as-tool:专家 = ChatClient,包进 @Tool 的方法体**
   `AgentTools` 里 `@Tool String translateToEnglish(...)` 的方法体就一行 `return translatorAgent.prompt(text).call().content()`——对主管是个普通工具,工具内部藏着一个专家 Agent。这是把"子 Agent"接入"主 Agent"的标准手法,复用了 @Tool + agentic loop 全部旧知识。

3. **构造器注入 > 字段注入(能 final 的依赖优先)**
   `AgentTools` 用构造器收两个 ChatClient:能 `private final`(造好即不可变)、依赖摆在签名一眼看全、可 `new AgentTools(a,b)` 脱离 Spring 测试、不存在半成品 null 态。单构造器时 Spring 自动注入,连 @Autowired 都不用写。**多个同类型 bean(这里 5 个 ChatClient)靠构造器参数名 = bean 名 精准认领**,同字段注入的"变量名=bean名"规则。

4. **编排也有自己的"动态阶梯"(下一步)**
   现在专家写死在源码 @Tool=多 Agent 的"L1"。同 L2 思路可把专家清单外置成 JSON(角色/描述都是字符串=数据)、用 FunctionToolCallback 动态造、/reload 热加、甚至专家是远程服务/MCP(=L3)。**Agent 的 system 提示词就是数据,整条 L1→L3 阶梯对 Agent 同样适用。**

---

## 多 Agent · 交易研究流水线 + 多空辩论(大目标·阶段三)

> 一句话:把"分析→辩论→决策"做成一条代码编排的串行流水线。每层 Agent 的输出拼成下一层的输入;用两个立场对立的 Agent 逼出更全面的判断。

1. **流水线 = drafter→reviewer 的放大,层数不是问题**
   6 个角色(技术/基本/新闻分析师 + 多/空研究员 + 决策)就是 `.call().content()` 串起来:symbol→三分析→拼成 analyses→喂多空→拼成 context→喂决策→返回 record。控制权在代码(同流水线,非 orchestrate 路由)。多个同类型 ChatClient(这里 ~12 个)靠变量名=bean名认领。

2. **对立辩论:同一输入 + 相反角色 = 两套对立论述**
   bull/bear 拿**完全相同的** analyses,但 system 提示词一个"只找利好、绝不提利空"、一个"只找利空、绝不提利好",逼出针锋相对的两面。这是"用结构逼出更好判断"的具体手段——不是堆人头,是制造对抗减少偏见。提示词越偏激对立,效果越明显。

3. **决策层综合,不投票不平均**
   把"分析+多空"全拼进 context 交给决策 Agent 自己权衡(LLM-native),它会引用两方、给方向+理由+风险+置信度。比硬写投票/打分逻辑简单且更像人类判断。

4. **诚实认清边界:现在是"脑补"不是"真数据"**
   纯 Java 阶段,分析师的"2022营收XXX亿"是 LLM 从训练知识回忆的,没接实时行情——先把**协作结构**跑通,真数据是后续阶段(Python 取数→HTTP→喂 Agent)。**永远分清"机制对了"和"数据真了"是两件事。**

5. **V1 是"一次性辩论",真辩论要多轮**
   当前多空各看分析说一遍、互不反驳。进阶=让两方来回几轮互相拆台(或竞争打分只采纳更优的)。先 V1 跑通再加轮次——别一口吃成胖子。

---

## Java ↔ Python 打通(大目标·阶段六)

> 一句话:Java 用裸 RestTemplate 直连 Python 的 HTTP 接口,把 JSON 文本喂给 Agent,让它基于真数据分析。数据跨语言跨进程:akshare→pandas→FastAPI→HTTP→RestTemplate→Agent prompt。

1. **RestTemplate = "我去调别人";@RestController = "别人来调我"**
   两个相反方向。`restTemplate.getForObject(url, String.class, arg)` = 发 GET、把响应体当 String 拿回。占位符 `{x}` 自动 URL 编码(别裸拼用户输入进 URL)。

2. **非 Nacos 的外部 URL 必须用裸 `new RestTemplate()`,不能 Feign/@LoadBalanced**
   Python 服务没注册 Nacos,只是 `localhost:8000`。@LoadBalanced/Feign 会拿 host 当服务名去 Nacos 查→炸。规则同天气工具调 wttr.in:外部真实 URL 用裸 RestTemplate。

3. **JSON 在网线上永远是文本;"变成 String" vs "解析成对象"是你的选择**
   `getForObject(url, String.class)`=原样拿 JSON 文本(Java 不解析);`getForObject(url, Dto[].class)`=Jackson 把文本解析成对象。**最终读者是 LLM 时,直接拿 String 喂它最省事——LLM 自己会读 JSON,省掉建 DTO**。只有 Java 自己要算这些数字时才需解析成对象。

4. **"喂真数据"= 把外部数据拼进 prompt,让 Agent 落地到事实**
   阶段三技术面 Agent 只收股票名→脑补假数字(茅台说成 200~500 元);阶段六把真实 MA5/涨跌幅 JSON 拼进 prompt→它引用 1267.14、+3.92% 等真值。**RAG/工具/外部服务本质都是"给 LLM 喂它没有的事实",减少幻觉。**

---

## RAG 接入多 Agent(大目标·阶段七)

> 一句话:RAG 在多 Agent 体系里 = "某个分析师 Agent 的查资料能力"。给那个 Agent 挂上 `QuestionAnswerAdvisor`,它就会先检索知识库、再基于检索结果分析。

1. **挂 advisor 一行就接上 RAG,复用已有向量库**
   `agent.prompt(q).advisors(QuestionAnswerAdvisor.builder(vectorStore).build()).call()`。同一个 advisor,挂通用聊天 = rag 模式;挂专门的基本面分析师 = "研报知识库分析师"。RAG 不是新东西,是给某个 Agent 配的能力。

2. **QuestionAnswerAdvisor 的机制(检索增强)**
   发给 LLM 前自动:把 query 转向量 → 去向量库搜相似文档块 → 把搜到的内容塞进 prompt 当"参考资料" → 再生成。所以 LLM 是带着检索到的研报回答的。

3. **验证 RAG"真检索了"的手法:埋独特事实**
   知识库放一份带**训练数据里绝不可能有的独特信息**的文档(如编造的"目标价1888元/数字酒证计划/海外占比8%"),若 Agent 复述了这些 = 铁证它真检索了知识库、非脑补。这是判断 RAG 是否生效的可靠测法。

4. **语义检索:查询语言要和文档语言对齐**
   向量检索靠"语义相似"。研报写"贵州茅台"就用中文名查(别用 sh600519),否则 embedding 不相似、检索不到。

5. **多 Agent 里给不同 Agent 配不同能力**
   技术面 Agent 配"真行情数据"(阶段六),基本面/新闻面 Agent 配"研报 RAG"(阶段七)——每个分析师的"信息来源"按角色定制。这就是生产级多 Agent 系统的样子:角色 + 专属工具/数据源。

---

## 完整流水线组装 + 风控 + 复盘(大目标·阶段八)

> 一句话:把各阶段积木串成一条端到端链——三分析师(各用真数据源)→ 多空辩论 → 风控 → 决策 → 复盘,输出结构化可追溯的完整报告。

1. **"系统组装"= 拼积木,不是写新东西**
   阶段八没引入新框架,就是把阶段六(技术面真行情)、阶段七(基本面 RAG)、阶段三(辩论决策)串进一条 `full` 流水线,再补风控/复盘两个角色。每段都写过,价值在"端到端跑通"。一个端点收 `code`(给 Python)+`name`(给 RAG)两参数,因为两数据源要的格式不同。

2. **复盘 Agent = 对"过程"做元评判,系统获得"反思"能力**
   复盘官不重做决策,而是评判整条决策链的逻辑质量(分析严不严谨、辩论有无遗漏、证据撑不撑结论)。这是高级多 Agent 的标志:**有 Agent 专门审视其他 Agent 的协作质量**。实测它真挑出了真问题(缺成交量数据、看空论证单一)——元层 Agent 确有价值,不是摆设。

3. **风控/复盘也是"角色提示词"——同一套机制延展**
   加任何新角色(风控、复盘、合规…)= 加一个 ChatClient bean + 一句 system 角色 + 接进流水线。多 Agent 系统的扩展成本极低,这正是"用结构组织能力"的威力。

4. **诚实边界(路径反复强调)**
   ① 全链仍是 LLM 对给定信息的推理,新闻面还没接真新闻源(留 TODO);② 这是研究脚手架,不是能交易的策略;③ 回测/paper trading 又是另一回事(阶段九),且回测不代表未来(数据质量/幸存者偏差)。**始终分清"机制跑通"和"能赚钱"是两码事。**

---
