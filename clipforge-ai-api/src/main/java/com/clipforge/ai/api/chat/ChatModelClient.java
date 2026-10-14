package com.clipforge.ai.api.chat;

/**
 * ChatModelClient（聊天模型客户端）。
 *
 * <p>该接口屏蔽 Mock（模拟）模型、LangChain4j（LangChain 的 Java 封装）模型和后续多供应商模型调用差异。
 */
public interface ChatModelClient {

    /**
     * chat（对话）用于发起一次非流式 Chat Model（聊天模型）调用。
     *
     * @param request 聊天模型请求
     * @return 聊天模型响应
     */
    ChatModelResponse chat(ChatModelRequest request);
}
