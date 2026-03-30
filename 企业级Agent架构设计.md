# 企业级 Agent 架构设计

## 一、Agent 核心概念

### 1.1 什么是 Agent

```
Agent = LLM(大脑) + Tools(手脚) + 循环执行框架(神经系统)
```

| 组件 | 角色 | 说明 |
|------|------|------|
| **LLM** | 决策/调度 | 理解用户意图，决定调用哪个工具 |
| **Tools** | 执行能力 | 具体的执行动作（RAG、搜索、计算等） |
| **循环执行框架** | 引擎 | 连接 LLM 和 Tools，让循环执行成为可能 |

### 1.2 Workflow vs Agent

| | 预设 Workflow | Agent |
|--|--------------|-------|
| 流程 | 人预设 | LLM 动态决定 |
| 工具选择 | 无 | LLM 决定 |
| 循环支持 | 无或有限 | 可循环 |
| 结束条件 | 预设 | LLM 判断 |

```
传统 Workflow：
    A → B → C → D（固定流程）

Agent Workflow：
    LLM思考 → [LLM决定调用工具X] → 工具执行 → LLM总结 → 结束
           ↑
           这里的判断是 LLM 动态做出的
```

### 1.3 Agent 能力层次

```
┌─────────────────────────────────────────────────────┐
│                    Agent 能力金字塔                    │
├─────────────────────────────────────────────────────┤
│                                                     │
│                    ▲ 效能增强 ▲                      │
│           ┌────────────────────────────────┐        │
│           │   反思(Reflection)              │        │
│           │   记忆(Memory)                 │        │
│           │   会话管理(Session)            │        │
│           │   规划(Planning)               │        │
│           ├────────────────────────────────┤        │
│           │         ▲ 核心能力 ▲            │        │
│           │   ┌─────────────────────┐     │        │
│           │   │  LLM (智能理解)      │     │        │
│           │   │  Tools (工具丰富度)  │     │        │
│           │   │  循环执行框架        │     │        │
│           │   └─────────────────────┘     │        │
│           └────────────────────────────────┘        │
│                                                     │
└─────────────────────────────────────────────────────┘
```

| 能力 | 类别 | 说明 |
|------|------|------|
| **LLM 理解意图** | 核心 | 听不懂用户说啥，工具再多也没用 |
| **Tools 丰富度** | 核心 | 没有对应工具，LLM 再聪明也白搭 |
| **循环执行框架** | 核心 | 没有框架，LLM 和 Tools 连不起来 |
| **记忆 Memory** | 增强 | 多轮对话不丢上下文 |
| **会话 Session** | 增强 | 区分不同用户的对话 |
| **规划 Planning** | 增强 | 把复杂任务拆解成步骤 |
| **反思 Reflection** | 增强 | 自我检查做对了没 |
| **RAG** | 增强 | 让 Agent 拥有知识 |

---

## 二、Java Agent 技术栈

### 2.1 框架选择

| 框架 | 成熟度 | 特点 | 推荐度 |
|------|--------|------|--------|
| **LangChain4j** | ⭐⭐⭐⭐ | Java 版 LangChain，完整生态 | ⭐⭐⭐⭐ |
| **LangGraph4j** | ⭐⭐⭐ | 轻量，专注图执行 | ⭐⭐⭐ |
| **Spring AI** | ⭐⭐⭐ | Spring 官方，工具调用尚在完善 | ⭐⭐⭐ |

### 2.2 技术选型

| 层级 | 技术选型 | 理由 |
|------|----------|------|
| **执行框架** | LangChain4j / 自研循环引擎 | 成熟生态 |
| **LLM 接入** | Spring AI / LangChain4j | 统一抽象多厂商 |
| **Memory** | Redis / PostgreSQL + pgvector | 生产级记忆 |
| **Tools** | 自研 + Spring AI Tool | 按需扩展 |
| **RAG** | Milvus / Qdrant / pgvector | 向量数据库 |
| **网关** | Spring Cloud Gateway / Envoy | API 路由 |
| **限流** | Bucket4j / Redis Lua | 流量控制 |
| **熔断** | Resilience4j | 容错保护 |
| **会话** | Redis + Spring Session | 分布式会话 |
| **监控** | Micrometer + Prometheus | 指标采集 |
| **追踪** | OpenTelemetry + Jaeger | 链路追踪 |

---

## 三、完整企业级架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        企业级 Agent 完整架构                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                     1. 接入层 (Gateway)                    │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │   │
│  │  │  API   │  │  Web   │  │企业微信 │  │  飞书   │  ...   │   │
│  │  │ Gateway│  │Socket  │  │  Bot   │  │  Bot   │         │   │
│  │  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘       │   │
│  │       └───────────┴───────────┴───────────┘              │   │
│  │                         │                                  │   │
│  │              ┌──────────┴──────────┐                       │   │
│  │              │     Load Balancer   │                       │   │
│  │              │     Rate Limiter    │                       │   │
│  │              │     Auth / Auth     │                       │   │
│  │              │     Request ID      │                       │   │
│  │              └──────────┬──────────┘                       │   │
│  └─────────────────────────┼────────────────────────────────┘   │
│                              │                                  │
│  ┌─────────────────────────┼────────────────────────────────┐   │
│  │                    2. 路由层 (Router)                      │   │
│  │                                                          │   │
│  │    ┌─────────────────────────────────────────────┐       │   │
│  │    │              Session Manager                 │       │   │
│  │    │    (维护用户会话、路由到对应 Agent 实例)        │       │   │
│  │    └─────────────────────┬───────────────────────┘       │   │
│  │                          │                                 │   │
│  │              ┌───────────┴───────────┐                     │   │
│  │              │   Agent Router        │                     │   │
│  │              │  (意图识别 → 分发到    │                     │   │
│  │              │   不同的 Agent)       │                     │   │
│  │              └───────────┬───────────┘                     │   │
│  └──────────────────────────┼──────────────────────────────┘   │
│                              │                                  │
│  ┌──────────────────────────┼────────────────────────────────┐   │
│  │                    3. Agent 核心层                         │   │
│  │                                                          │   │
│  │    ┌─────────────────────────────────────────────┐       │   │
│  │    │              Agent Runtime                   │       │   │
│  │    │         ┌─────────────────────┐             │       │   │
│  │    │         │      LLM Brain     │             │       │   │
│  │    │         └──────────┬──────────┘             │       │   │
│  │    │                    │                        │       │   │
│  │    │    ┌───────────────┼───────────────┐        │       │   │
│  │    │    ▼               ▼               ▼        │       │   │
│  │    │ ┌──────┐     ┌────────┐     ┌──────────┐   │       │   │
│  │    │ │Tools │◀────│ Memory │────▶│ Planner  │   │       │   │
│  │    │ │(手脚)│     │(记忆)  │     │ (规划)   │   │       │   │
│  │    │ └──────┘     └────────┘     └──────────┘   │       │   │
│  │    └─────────────────────────────────────────────┘       │   │
│  │                                                          │   │
│  │    ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │   │
│  │    │ RAG    │  │Search  │  │Code    │  │ DB     │  ... │    │
│  │    │Tool    │  │Tool    │  │Executor│  │Tool    │      │    │
│  │    └─────────┘  └─────────┘  └─────────┘  └─────────┘    │   │
│  └──────────────────────────┬──────────────────────────────┘   │
│                              │                                  │
│  ┌──────────────────────────┼────────────────────────────────┐   │
│  │                    4. 知识层                               │   │
│  │                                                          │   │
│  │    ┌──────────────┐        ┌──────────────┐              │   │
│  │    │  Vector DB   │◀──────│   Embedding  │              │   │
│  │    │(Milvus/Qdrant│       │   Service    │              │   │
│  │    └──────────────┘        └──────────────┘              │   │
│  │                                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    5. 数据层                               │   │
│  │                                                          │   │
│  │    ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐       │   │
│  │    │Memory  │  │Session │  │ History│  │ Metrics│       │   │
│  │    │ Store  │  │ Store  │  │  Store │  │  Store │       │   │
│  │    │(Redis) │  │(Redis) │  │(MySQL) │  │(ES)   │       │   │
│  │    └────────┘  └────────┘  └────────┘  └────────┘       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    6. 基础设施层                           │   │
│  │                                                          │   │
│  │    ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐       │   │
│  │    │Prometh.│  │ Grafana│  │  Jaeger│  │ K8s   │       │   │
│  │    │ Metrics│  │Dashboard│  │Tracing │  │Deploy │       │   │
│  │    └────────┘  └────────┘  └────────┘  └────────┘       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、核心模块详细设计

### 4.1 核心接口定义

```java
// 1. Tool 定义（任何可执行能力的抽象）
public interface Tool {
    String getName();                              // 工具名
    String getDescription();                       // 描述（让 LLM 理解用途）
    String getParameterJsonSchema();              // 参数 Schema
    ToolResult execute(Map<String, Object> params); // 执行
}

public record ToolResult(boolean success, String output, String error) {}

// 2. Memory 定义（记忆接口）
public interface Memory {
    void add(UserMessage msg);          // 添加用户消息
    void add(AssistantMessage msg);      // 添加 AI 消息
    void add(ToolResult result);         // 添加工具结果
    List<Message> getMessages();         // 获取全部上下文
    void clear();                        // 清空
}

// 3. Agent 接口
public interface Agent {
    AgentResponse execute(AgentRequest request);
}

public record AgentRequest(String userId, String sessionId, String userInput) {}
public record AgentResponse(String content, String finishReason, List<ToolCall> toolCalls) {}
```

### 4.2 循环执行引擎

```java
public class SimpleAgentRuntime {
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;
    private final Memory memory;

    public AgentResponse execute(AgentRequest request) {
        // 1. 存入用户消息
        memory.add(new UserMessage(request.userInput()));

        // 2. 循环执行（最多 N 次，防止死循环）
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 3. LLM 思考 + 决定是否调用工具
            List<Message> context = memory.getMessages();
            List<Tool> availableTools = toolRegistry.getAllTools();

            LLMResponse llmResponse = llmClient.chat(context, availableTools);

            // 4. LLM 不需要调用工具，直接返回
            if (llmResponse.toolCalls().isEmpty()) {
                memory.add(new AssistantMessage(llmResponse.content()));
                return new AgentResponse(llmResponse.content(), "DONE", null);
            }

            // 5. 执行工具
            for (ToolCall tc : llmResponse.toolCalls()) {
                Tool tool = toolRegistry.getTool(tc.name());
                ToolResult result = tool.execute(tc.parameters());
                memory.add(new ToolMessage(tc.name(), result));
            }

            // 6. 继续循环，让 LLM 基于工具结果再思考
        }

        return new AgentResponse("执行达到最大次数限制", "MAX_ITERATIONS", null);
    }
}
```

### 4.3 网关层职责

```
用户请求
    │
    ▼
┌─────────────────────────────────────┐
│           API Gateway                │
│  ┌─────────────────────────────┐   │
│  │ 1. 鉴权 (JWT / OAuth)        │   │
│  │ 2. 限流 (Token Bucket)       │   │
│  │ 3. 熔断 (Circuit Breaker)    │   │
│  │ 4. 负载均衡 (Round Robin)    │   │
│  │ 5. 请求日志                  │   │
│  │ 6. 协议转换 (HTTP→内部RPC)   │   │
│  └─────────────────────────────┘   │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│         Session Manager              │
│  (维护用户会话上下文)                 │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│          Agent Runtime               │
│     ┌─────────────────────────┐     │
│     │     LLM + Tools + Memory│     │
│     └─────────────────────────┘     │
└─────────────────────────────────────┘
```

| 组件 | 职责 |
|------|------|
| **鉴权** | JWT / OAuth 验证 |
| **限流** | 防止系统过载 |
| **熔断** | 快速失败，保护系统 |
| **负载均衡** | 多实例分发 |
| **会话管理** | 多轮对话上下文维护 |

---

## 五、Tools 抽象能力

| Tool | 功能 | LLM 行为 |
|------|------|----------|
| **RAG** | 企业知识库检索 | "我不知道答案，让我查一下" |
| **Search** | 搜索引擎 | "这个问题需要最新信息" |
| **Calculator** | 数学计算 | "需要计算一下" |
| **Code Executor** | 执行代码 | "我写段代码来验证" |
| **API Caller** | 调外部接口 | "需要调别人系统查" |
| **Database** | 查数据库 | "需要查下业务数据" |
| **File R/W** | 文件读写 | "需要读取文件内容" |
| **TTS** | 语音合成 | "需要把结果转成语音" |

---

## 六、项目结构建议

```
agent-project/
├── pom.xml
│
├── src/main/java/com/example/agent/
│   │
│   ├── AgentApplication.java
│   │
│   ├── core/                           # 核心接口
│   │   ├── Tool.java
│   │   ├── Memory.java
│   │   ├── Agent.java
│   │   └── LLMClient.java
│   │
│   ├── runtime/                        # 执行引擎
│   │   ├── SimpleAgentRuntime.java
│   │   └── AgentRuntimeBuilder.java
│   │
│   ├── tools/                          # 内置工具
│   │   ├── ToolRegistry.java
│   │   ├── SearchTool.java
│   │   ├── CalculatorTool.java
│   │   ├── RAGTool.java               # RAG 封装
│   │   └── HttpTool.java
│   │
│   ├── memory/                         # 记忆实现
│   │   ├── SimpleMemory.java
│   │   └── VectorMemory.java          # 向量数据库记忆
│   │
│   ├── llm/                           # LLM 客户端
│   │   ├── OpenAIClient.java
│   │   └── DeepSeekClient.java
│   │
│   ├── gateway/                        # 网关层
│   │   ├── ApiGateway.java
│   │   ├── RateLimiter.java
│   │   └── CircuitBreaker.java
│   │
│   ├── router/                         # 路由层
│   │   ├── SessionManager.java
│   │   └── AgentRouter.java
│   │
│   └── config/                         # 配置
│       └── AgentConfig.java
```

---

## 七、总结

```
┌─────────────────────────────────────────────────────┐
│                   Agent 开发要点                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│  核心部分（必须做）：                                  │
│    ├── LLM（智能理解）                                │
│    ├── Tools（工具丰富度）                            │
│    └── 循环执行框架                                   │
│                                                     │
│  增强部分（拉开差距）：                                │
│    ├── Memory（记忆管理）                             │
│    ├── Session（会话管理）                            │
│    ├── Planner（任务规划）                            │
│    ├── Reflection（反思能力）                        │
│    └── RAG（知识增强）                                │
│                                                     │
│  外围保障（系统稳定性）：                              │
│    ├── API Gateway（鉴权、限流、熔断）                 │
│    ├── Session Manager（会话管理）                    │
│    ├── 监控告警（Prometheus + Grafana）               │
│    └── 链路追踪（Jaeger）                             │
│                                                     │
│  核心竞争力：Tools 的丰富度与质量                      │
│                                                     │
└─────────────────────────────────────────────────────┘
```
