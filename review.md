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
