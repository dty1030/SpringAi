# SpringAIDemo Handoff

更新时间: 2026-06-14

## 项目目标

把 Spring AI Demo 扩展成一个本地 AI Research Console：

- Spring Boot 负责前端页面、鉴权、RAG、Agent 编排和交易分析接口。
- Python FastAPI 负责股票数据源、指标计算、策略信号和事实型数据接口。
- Java 侧 Agent 尽量读取已经计算好的事实 JSON，少让模型自己推断数字关系。
- 最终 Trading 页面可以跑完整流水线：技术面、基本面、新闻面、多空辩论、风控、综合决策、复盘评价。

## 当前状态

### Java / Spring Boot

- 工作目录: `D:\Javalearn\Git_dty\SpringAIDemo`
- 本地 workspace 已经通过配置切换：
  - `app.workspace.base-dir: "D:\\ai-workspace"`
  - `LocalWorkspaceStrategy` 通过 `@Value("${app.workspace.base-dir:D:\\ai-workspace}")` 读取。
- Trading data 服务地址已改成配置项：
  - `app.trading-data.base-url: "http://localhost:8000"`
  - `StockDataClient` 通过 `baseUrl` 拼接 `/news`、`/indicators`、`/strategy`、`/market-snapshot` 等接口。
- `TradingController.full()` 已经接入：
  - 近期指标数据 `getIndicators(code)`
  - 实时行情快照 `getMarketSnapshot(code)`
  - 技术事实 `getTechnicalFacts(code)`，用于把均线关系明确传给 Agent。
- `TradingController.myView()` 已经接入：
  - `my-strategy.txt`
  - 实时行情快照
  - 策略信号
  - 近期指标数据

注意：如果 Java 编译失败，优先检查 `StockDataClient` 是否已经补了：

```java
public String getTechnicalFacts(String symbol) {
    return restTemplate.getForObject(
            baseUrl + "/technical-facts?symbol={symbol}",
            String.class,
            symbol
    );
}
```

### Python / FastAPI

- 工作目录: `D:\Javalearn\Git_dty\trading-data-service`
- 启动方式：

```powershell
cd D:\Javalearn\Git_dty\trading-data-service
.\.venv\Scripts\Activate.ps1
python -m uvicorn main:app --reload --port 8000
```

- 已有接口包括：
  - `/indicators?symbol=sh600519`
  - `/strategy?symbol=sh600519`
  - `/market-snapshot?symbol=sh600519`
  - `/technical-facts?symbol=sh600519`
- `market_snapshot.py` 使用 `ak.stock_zh_a_spot()`，并且在 `import akshare` 前设置 `NO_PROXY`。
- `technical_facts.py` 已经计算：
  - MA5 / MA10 / MA20 / MA30 / MA60 / MA120 / MA240
  - `above_maX`
  - `close_maX_gap_pct`

### 前端

- Trading UI 已升级成工作台样式。
- 重要 DOM id / 函数名保持不变：
  - `trading-code`
  - `trading-name`
  - `trading-full`
  - `trading-debate`
  - `trading-output`
  - `showTradingResult`
- `showTradingResult(data)` 已经把 JSON 结果拆成多个 section 渲染，不再只是纯文本拼接。

## 我们之间的约定

- 默认不要直接改代码，先讲清楚，让我自己写。
- 如果我明确说“帮我改”“直接帮我改”，才可以直接动文件。
- 前端函数名、DOM id、后端接口参数、字段名不要随便改。
- 如果必须改和后端对应的名字，先确认。
- 讲 Python / pandas 时要解释清楚，但不要太啰嗦。
- 学习重点是：我能理解为什么这样写，并且以后能自己写出来。

## 已踩坑 / Failed Attempts

- `ak.stock_zh_a_spot_em()` 请求东方财富时出现过代理连接错误：
  - `ProxyError`
  - `RemoteDisconnected`
  - 后来改用 `ak.stock_zh_a_spot()`，并设置 `NO_PROXY`。
- Java 里 `StockDataClient.getMarketSnapshot()` 曾经把参数写成 `sumbol`，应为 `symbol`。
- JS 控制台测试字符串时，单引号字符串不能直接跨行：
  - 错误：`'第一行
第二行'`
  - 正确：`'第一行\n第二行'` 或使用模板字符串。
- Agent 曾经把技术事实读反：
  - `above_ma10: true` 却说低于 MA10。
  - 下一步需要继续强化 prompt，让模型逐项遵守 `above_maX` 字段。
- 风控 Agent 有时会凭空估算未来最大回撤。
  - 后续最好做一个事实型 `/drawdown-facts`，让风控基于计算结果，而不是自由发挥。

## 下一步

当前优先级：

1. 继续修正技术面 prompt，让 Agent 明确遵守：
   - `above_maX = true` 表示收盘价高于该均线。
   - `above_maX = false` 表示收盘价低于该均线。
   - 每一条均线结论必须和对应字段一致。

2. 重新编译 Java：

```powershell
.\mvnw.cmd compile
```

3. 重新测试完整流水线：

```text
http://localhost:8989/api/trading/full?code=sh600519&name=贵州茅台
```

4. 如果技术面仍然读错，考虑让 Java 返回 `technicalFacts` 原文到前端，方便调试。

5. 下一个事实型接口建议二选一：
   - `/drawdown-facts`：给风控 Agent 用，减少最大回撤幻觉。
   - `/fund-flow`：补充资金流信息，改善复盘里常说的“缺少成交量/资金流分析”。

## 常用测试命令

Python 语法检查：

```powershell
cd D:\Javalearn\Git_dty\trading-data-service
.\.venv\Scripts\Activate.ps1
python -m py_compile main.py market_snapshot.py technical_facts.py backtest.py strategy.py
```

Python 单函数测试：

```powershell
python -c "from technical_facts import technical_facts; print(technical_facts('sh600519'))"
python -c "from market_snapshot import market_snapshot; print(market_snapshot('sh600519'))"
```

Java 编译：

```powershell
cd D:\Javalearn\Git_dty\SpringAIDemo
.\mvnw.cmd compile
```

前端 JS 语法检查：

```powershell
node --check src\main\resources\static\app.js
```
