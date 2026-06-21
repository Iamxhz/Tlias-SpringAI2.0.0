package com.xhz.aiconfig;

import com.xhz.tools.DateTimeTools;
import com.xhz.tools.EmpManagementTools;
import com.xhz.tools.StudentManagementTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 全局装配配置 — Spring AI 2.0.0
 *
 * 一次性完成 ChatClient 的全部全局组装：
 * 1. RAG Advisor — 教务规章检索增强（VectorStoreDocumentRetriever + RetrievalAugmentationAdvisor）
 * 2. Chat Memory Advisor — 对话记忆滑动窗口（MessageChatMemoryAdvisor）
 * 3. Tool Calling — 学员管理 + 员工管理 + 日期时间
 * 4. Logger Advisor — 请求/响应日志（SimpleLoggerAdvisor）
 *
 * 业务层 {@link com.xhz.service.impl.AiServiceImpl} 注入后只需关心 conversationId 等请求级参数。
 */
@Configuration
public class ChatClientConfig {
    @Autowired
    private SyncMcpToolCallbackProvider mcpToolCallbackProvider;
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
                                      DateTimeTools dateTimeTools) {

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

        return chatClientBuilder
                // —— System Prompt（按需启用）——
                // .defaultSystem("""
                //     你是 Tlias 智能教务系统的全能助理。
                //     你可以结合学员数据、违纪信息等帮助班主任完成日常管理工作。
                //     当用户询问教务制度、请假扣分等规则时，请务必优先参考检索到的「内部规章制度上下文」进行严谨回答。
                //     """)
                // —— Advisors ——
                .defaultAdvisors(
                        ragAdvisor,    // RAG Advisor（按需启用）
                        memoryAdvisor,    // Chat Memory Advisor
                        new SimpleLoggerAdvisor() // 请求/响应日志
                )

                // —— Tool Calling ——
                .defaultTools(mcpToolCallbackProvider,studentManagementTools, empManagementTools, dateTimeTools)
                .build();
    }

    /**
     * 备用 ChatClient Bean（无装配，供测试使用）
     */
    @Bean
    public ChatClient ChatClient111(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }
}
