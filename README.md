# Tlias-SpringAI2.0.0
在Tlias人事管理系统的基础上基于SpringAI2.0.0开发的练习项目
# Tlias 智能教务系统 — Spring AI 功能全景

> 生成日期：2026-06-18  
> 项目：tlias-web-management  
> 框架：Spring Boot 4.0.6 + Spring AI 2.0.0 (GA) + JDK 17

---

## 一、技术栈

| 层级           | 选型                                        | 版本                    |
| -------------- | ------------------------------------------- | ----------------------- |
| 基础框架       | Spring Boot                                 | 4.0.6                   |
| AI 框架        | Spring AI                                   | 2.0.0 (GA)              |
| JDK            | Java                                        | 17                      |
| 大模型         | DeepSeek V4 Pro                             | 通过 OpenAI 兼容协议    |
| Embedding 模型 | 阿里云 DashScope text-embedding-v4          | 2048 维                 |
| 向量存储       | Redis Stack (RedisVectorStore)              | initialize-schema: true |
| Redis 客户端   | Jedis                                       | 7.4.1                   |
| 数据库         | MySQL                                       | localhost:3306/tlias    |
| ORM            | MyBatis                                     | 4.0.1                   |
| 分页           | PageHelper                                  | 4.1.0                   |
| MCP 协议       | spring-ai-starter-mcp-client                | 2.0.0                   |
| 文档解析       | TikaDocumentReader + MarkdownDocumentReader | 2.0.0                   |
| 前端           | 纯 HTML + Tailwind CSS CDN + vanilla JS     | SSE 流式                |

---

## 二、架构设计：洋葱架构

```
┌──────────────────────────────────────────────────────┐
│  Controller (路由控制层)                               │
│  AiController.java — 仅 40 行，只做路由映射+参数提取     │
├──────────────────────────────────────────────────────┤
│  Service (业务逻辑层)                                   │
│  AiServiceImpl.java — 注入 ChatClient，设置请求级参数    │
├──────────────────────────────────────────────────────┤
│  Config (全局装配层)                                    │
│  ChatClientConfig — 一次性装配 RAG+Memory+Tools+Log    │
│  ChatMemoryConfig — Redis 持久化 + 20 条滑动窗口        │
│  VectorStoreConfig — Embedding → VectorStore          │
├──────────────────────────────────────────────────────┤
│  Tools (AI 工具适配层)                                  │
│  StudentManagementTools / EmpManagementTools          │
│  / DateTimeTools — 参数兜底 → 委托 Service → 自然语言战报 │
├──────────────────────────────────────────────────────┤
│  Infrastructure (基础设施层)                            │
│  RedisChatMemoryRepository — 自定义 Redis 存储引擎      │
│  TliasRulesInitializer — 启动时自动灌向量知识库          │
└──────────────────────────────────────────────────────┘
```

**核心设计原则**：

- Controller 不写业务逻辑
- Tools 不直接注入 Mapper、不手动管理事务
- ChatClient 全局装配一次，业务层只需传 conversationId

---

## 三、已实现的 8 大功能模块

### 模块 1：流式对话 (SSE)

| 维度     | 实现                                                      |
| -------- | --------------------------------------------------------- |
| 后端     | `ChatClient.prompt().stream().content()` → `Flux<String>` |
| 前端     | `fetch()` + `ReadableStream` + `TextDecoder` 逐行解析 SSE |
| 前端容错 | 跨 chunk 行缓冲、`[DONE]` 过滤、JSON 转义还原             |
| 前端渲染 | Markdown→HTML（代码块/行内代码/粗体）、打字机光标动画     |

### 模块 2：Function Calling (@Tool 注解)

**3 个 Tool 组件，共 5 个工具方法：**

| 组件                     | 方法                   | 功能                                       |
| ------------------------ | ---------------------- | ------------------------------------------ |
| `StudentManagementTools` | `updateViolationScore` | 违纪扣分（原子 SQL `violation_score + N`） |
| `EmpManagementTools`     | `save`                 | 新员工入职（主表+子表工作经历）            |
| `EmpManagementTools`     | `deleteByIds`          | 批量删除员工+级联删除工作经历              |
| `DateTimeTools`          | `getCurrentTime`       | 获取当前日期时间                           |
| `DateTimeTools`          | `setAlarm`             | 设置定时闹钟提醒                           |

**两种入参模式均已使用**：

- `record` + `@JsonPropertyDescription`（StudentManagementTools）
- 普通方法参数 + `@ToolParam(description = "...")`（EmpManagementTools、DateTimeTools）

**三层防御体系**（每个 @Tool 方法都遵循）：

1. 参数兜底校验 → 提前拦截漏参并返回纠偏提示
2. 委托 Service 层执行业务
3. catch 异常 → `log.error` 记录真实异常 + 返回自然语言提示

### 模块 3：MCP 客户端（远程工具发现）

| 维度       | 实现                                                         |
| ---------- | ------------------------------------------------------------ |
| 依赖       | `spring-ai-starter-mcp-client`                               |
| 连接方式   | SSE（`http://localhost:8888`）                               |
| 连接名     | `mcp-weather`                                                |
| 注入方式   | `SyncMcpToolCallbackProvider` → `.tools(mcpToolCallbackProvider)` |
| MCP Server | 同级目录 `mcpServer` 项目（`spring-ai-starter-mcp-server-webmvc`） |

**可用 MCP 远程工具**：`getWeatherForecast(city)` — 城市天气模拟查询

### 模块 4：RAG 检索增强生成

| 维度      | 实现                                                         |
| --------- | ------------------------------------------------------------ |
| 知识库    | `classpath:tlias-rules.txt`（教务规章制度）                  |
| 文档解析  | TikaDocumentReader（通用文档）+ TokenTextSplitter（chunkSize=200） |
| Embedding | 阿里云 DashScope text-embedding-v4（2048 维）                |
| 向量存储  | RedisVectorStore（index: `tlias-rules-index`）               |
| 检索器    | `VectorStoreDocumentRetriever`（topK=3, similarityThreshold=0.50） |
| Advisor   | `RetrievalAugmentationAdvisor`（自动将检索结果注入 Prompt）  |
| 初始化    | `TliasRulesInitializer`（启动时自动灌库，标记位防重复）      |

**RAG 链路**：

```
用户提问 → VectorStoreDocumentRetriever(topK=3) → Redis 向量检索
→ RetrievalAugmentationAdvisor 注入 Prompt
→ "请参考以下内部规章制度：{检索结果}" + 用户原问题
→ 大模型生成回答
```

### 模块 5：Chat Memory（对话记忆）

| 维度       | 实现                                                         |
| ---------- | ------------------------------------------------------------ |
| 存储引擎   | **自研** `RedisChatMemoryRepository`（实现 `ChatMemoryRepository` 接口） |
| 窗口策略   | `MessageWindowChatMemory`（maxMessages=20 条滑动窗口）       |
| Redis 结构 | `tlias:chat:memory:{conversationId}` → JSON 数组             |
| 会话索引   | `tlias:chat:memory:index` → Redis Set                        |
| Advisor    | `MessageChatMemoryAdvisor`（全局注册，请求级通过 `a.param(CONVERSATION_ID, ...)` 覆盖） |

**消息序列化**：自定义 JSON 格式 `[{mType, content}, ...]`，支持 User/System/Assistant/Tool 四种消息类型。

### 模块 6：日志 Advisor

| 维度    | 实现                                                   |
| ------- | ------------------------------------------------------ |
| Advisor | `SimpleLoggerAdvisor`                                  |
| 级别    | DEBUG（`logging.level.org.springframework.ai: debug`） |

### 模块 7：ChatClient 全局装配

**`ChatClientConfig` 一次性组装链**：

```java
chatClientBuilder
    .defaultAdvisors(ragAdvisor, memoryAdvisor, new SimpleLoggerAdvisor())
    .defaultTools(studentManagementTools, empManagementTools, dateTimeTools)
    .build();
```

**注册了 2 个 ChatClient Bean**：

- `agentChatClient` — 完整装配（RAG + Memory + Tools + Logger）
- `ChatClient111` — 裸 ChatClient（测试/对比用）

### 模块 8：JWT 鉴权白名单

`WebConfig.java` 中 `TokenInterceptor` 已排除 `/ai-chat.html`、`/ai/**`，AI 接口免登。

---

## 四、配置拓扑图

```
application.yml
├── spring.datasource         → MySQL (localhost:3306/tlias)
├── spring.data.redis         → Redis (localhost:6379, Jedis)
├── spring.ai.openai          → DeepSeek V4 Pro
│   ├── chat.model: deepseek-v4-pro
│   └── embedding: DashScope text-embedding-v4 (2048维)
├── spring.ai.mcp.client      → MCP SSE 连接 localhost:8888
└── spring.ai.vectorstore     → RedisVectorStore (tlias-rules-index)

pom.xml (Spring AI 依赖)
├── spring-ai-starter-model-openai       ← 大模型调用
├── spring-ai-starter-mcp-client         ← MCP 客户端
├── spring-ai-rag                        ← RAG 检索增强
├── spring-ai-vector-store               ← 向量存储抽象
├── spring-ai-starter-vector-store-redis ← Redis 向量存储
├── spring-ai-tika-document-reader       ← Tika 文档解析
├── spring-ai-markdown-document-reader    ← Markdown 文档解析
├── spring-boot-starter-data-redis       ← Redis 基础
└── redis.clients:jedis:7.4.1            ← Jedis 客户端
```

---

## 五、项目文件索引

```
src/main/java/com/xhz/
├── TliasApplication.java                ← 启动类
│
├── controller/
│   └── AiController.java               ← POST /ai/chat (SSE)
│
├── service/
│   ├── AiService.java                  ← 接口
│   └── impl/
│       └── AiServiceImpl.java          ← 注入 ChatClient + MCP + conversationId
│
├── aiconfig/                            ← Spring AI 配置层
│   ├── ChatClientConfig.java           ← ChatClient 全局装配 (RAG+Memory+Tools+Log)
│   ├── ChatMemoryConfig.java           ← ChatMemory Bean (Redis 持久化 + 滑动窗口)
│   ├── RedisChatMemoryRepository.java  ← 自研 Redis ChatMemory 存储引擎
│   └── VectorStoreConfig.java          ← VectorStore Bean (当前注释，待 Redis 方案)
│
├── initializer/
│   └── TliasRulesInitializer.java      ← 启动时灌 RAG 知识库到 Redis
│
├── tools/                               ← @Tool Function Calling
│   ├── StudentManagementTools.java     ← updateViolationScore
│   ├── EmpManagementTools.java         ← save + deleteByIds
│   └── DateTimeTools.java             ← getCurrentTime + setAlarm
│
├── interceptor/
│   └── WebConfig.java                  ← JWT 白名单 (/ai/**)
│
└── pojo/
    └── ChatRequest.java                ← { message } DTO

src/main/resources/
├── application.yml
├── tlias-rules.txt                      ← 教务规章制度（RAG 知识库原文）
└── static/
    └── ai-chat.html                     ← SSE 流式前端
```

---

## 六、当前能力矩阵

| 功能                  | 状态       | 备注                                          |
| --------------------- | ---------- | --------------------------------------------- |
| 流式 SSE 对话         | ✅ 完成     | 前端+后端全链路                               |
| @Tool 本地调用        | ✅ 完成     | 5 个工具方法，3 个组件                        |
| MCP 远程工具          | ✅ 完成     | 1 个天气工具，SSE 连接                        |
| RAG 检索增强          | ✅ 完成     | RedisVectorStore 全链路                       |
| Chat Memory           | ✅ 完成     | Redis 持久化，自定义存储引擎                  |
| 日志 Advisor          | ✅ 完成     | SimpleLoggerAdvisor                           |
| Default System Prompt | ⬜ 注释中   | ChatClientConfig 中已写好，按需开启           |
| 多会话隔离            | ⚠️ 硬编码   | conversationId 固定 `tlias-admin-session-001` |
| VectorStore           | ⚠️ 配置注释 | VectorStoreConfig Bean 已注释，当前走自动配置 |
| 嵌入模型              | ✅ 完成     | DashScope text-embedding-v4                   |

---

## 七、建议下一步学习方向

1. **多会话隔离** — conversationId 从固定字符串改为从 JWT Token / 请求头动态提取
2. **@Tool 扩展** — 在现有骨架类中增加请假审批、成绩查询、排课等功能
3. **Default System Prompt** — 启用全局 System Prompt，为所有对话注入教务助理角色
4. **MCP Server 扩展** — 在 mcpServer 中增加更多远程工具，验证 MCP 工具发现链路
5. **RAG 知识库扩展** — tlias-rules.txt 中增加更多规章制度，验证多文档检索效果
6. **Embedding 切换** — 尝试切换其他 Embedding 模型对比效果
7. **ChatMemory 策略** — 验证 20 条滑动窗口在长对话中的表现，调整 maxMessages
