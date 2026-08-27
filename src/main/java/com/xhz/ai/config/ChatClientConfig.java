package com.xhz.ai.config;

import com.xhz.ai.tool.DateTimeTools;
import com.xhz.ai.tool.DeptManagementTools;
import com.xhz.ai.tool.EmpManagementTools;
import com.xhz.ai.tool.StudentManagementTools;
import com.xhz.ai.tool.ClazzQueryTools;
import com.xhz.ai.tool.ReportQueryTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 全局装配配置 — Spring AI 2.0.0
 *
 * 一次性完成 ChatClient 的全部全局组装：
 * 1. RAG Advisor — 教务规章检索增强（VectorStoreDocumentRetriever + RetrievalAugmentationAdvisor）
 * 2. Chat Memory Advisor — 对话记忆滑动窗口（MessageChatMemoryAdvisor）
     * 3. Tool Calling — 查询直读 / 写操作 HITL（学员、员工、部门、班级、报表、日期时间）
 * 4. Logger Advisor — 请求/响应日志（SimpleLoggerAdvisor）
 *
 * 业务层 {@link com.xhz.ai.service.impl.AgentChatServiceImpl} 注入后只需关心 conversationId 等请求级参数。
 */
@Configuration
public class ChatClientConfig {
    /**
     * 全局 ChatClient Bean
     *
     * RAG 参数：topK=3, 相似度阈值=0.50
     */
    @Bean
    public ChatClient agentChatClient(ChatClient.Builder chatClientBuilder,
                                      VectorStore vectorStore,
                                      ChatMemory chatMemory,
                                      StudentManagementTools studentManagementTools,
                                      EmpManagementTools empManagementTools,
                                      DeptManagementTools deptManagementTools,
                                      DateTimeTools dateTimeTools,
                                      ClazzQueryTools clazzQueryTools,
                                      ReportQueryTools reportQueryTools,
                                      ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {

        // ==================== RAG 链式构建 ====================

        // 1. 文档检索器：从 VectorStore 中检索最相关的 topK 条规章
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(3)
                .similarityThreshold(0.50)
                .build();

        // 2. RAG 增强 Advisor：将检索结果自动注入到 Prompt 上下文
        RetrievalAugmentationAdvisor ragAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();

        // ==================== Chat Memory Advisor ====================

        // 3. 全局注册 Memory Advisor，请求级通过 a.param(CONVERSATION_ID, ...) 覆盖
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        // ==================== 组装 ChatClient ====================

        ChatClient.Builder builder = chatClientBuilder
                .defaultSystem("""
                        你是 Tlias 智能教务系统的「班主任 Agent」。
                        你协助班主任处理日常教务：规章制度问答、学员违纪、员工与部门管理、提醒闹钟等。
                        当用户询问教务制度、迟到早退、请假扣分等规则时，请优先参考检索到的「内部规章制度上下文」严谨回答。
                        所有写操作（入职、删员工、部门增删改、违纪扣分、设闹钟）都只生成待确认指令，不会立刻改库；
                        请明确告知班主任去界面点击确认，确认前不要声称操作已成功。
                        当用户询问城市天气、气温、天气预报时，必须调用远程工具 getWeatherForecast，根据工具返回结果回答；不要声称自己不知道天气。
                        当用户询问网上新闻、公开政策、百科知识、需要上网查证的事实时，必须调用远程工具 webSearch（Web Search），根据检索结果回答并带上出处链接；不要编造网上信息。校内规章仍优先用内部规章制度检索。
                        当用户询问员工名单、某位员工、班级名单、学员名单、各班人数、职位人数、学历分布、男女比例等数据时，必须调用对应只读查询 Tool，不要编造名单或统计数字。
                        当用户消息包含违纪证据图的视觉识别结果时：必须先调用 findStudents 定位学员，再按规章调用 updateViolationScore；
                        禁止凭空编造 studentId；多人违纪可分别发起确认；看不清或重名时先问班主任。
                        """)
                .defaultAdvisors(
                        ragAdvisor,
                        memoryAdvisor,
                        new SimpleLoggerAdvisor()
                );

        SyncMcpToolCallbackProvider mcp = mcpToolCallbackProvider.getIfAvailable();
        if (mcp != null) {
            builder.defaultTools(mcp, studentManagementTools, empManagementTools, deptManagementTools, dateTimeTools,
                    clazzQueryTools, reportQueryTools);
        } else {
            builder.defaultTools(studentManagementTools, empManagementTools, deptManagementTools, dateTimeTools,
                    clazzQueryTools, reportQueryTools);
        }
        return builder.build();
    }

    /**
     * 启动时打印 MCP 已发现的远程工具，便于确认 Client 是否连上 Server。
     */
    @Bean
    public org.springframework.boot.CommandLineRunner logMcpTools(
            ObjectProvider<SyncMcpToolCallbackProvider> mcpToolCallbackProvider) {
        return args -> {
            try {
                SyncMcpToolCallbackProvider provider = mcpToolCallbackProvider.getIfAvailable();
                if (provider == null) {
                    org.slf4j.LoggerFactory.getLogger(ChatClientConfig.class)
                            .warn("未装配 MCP Client，跳过远程工具。测天气/联网搜索请先启动 mcp-server:8888 再打开 spring.ai.mcp 配置并重启 tlias。");
                    return;
                }
                var callbacks = provider.getToolCallbacks();
                if (callbacks == null || callbacks.length == 0) {
                    org.slf4j.LoggerFactory.getLogger(ChatClientConfig.class)
                            .warn("MCP 未发现任何远程工具。请确认 mcp-server 已在 localhost:8888 启动，并重启本应用。");
                    return;
                }
                java.util.List<String> names = java.util.Arrays.stream(callbacks)
                        .map(cb -> cb.getToolDefinition().name())
                        .toList();
                org.slf4j.LoggerFactory.getLogger(ChatClientConfig.class)
                        .info("MCP 已加载远程工具: {}", names);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(ChatClientConfig.class)
                        .warn("MCP 工具探测失败（不影响主应用启动）：{}", e.getMessage());
            }
        };
    }

    /**
     * 备用 ChatClient Bean（无装配，供测试使用）
     */
    @Bean
    public ChatClient ChatClient111(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
