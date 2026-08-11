package com.clipforge.model.chat;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockChatModelClient（模拟聊天模型客户端）。
 *
 * <p>该实现用于在没有真实 API Key（接口密钥）时模拟 LLM（大语言模型）响应，保证 Agent（智能体）链路可测试、可演示。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.model", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockChatModelClient implements ChatModelClient {

    /**
     * chat（对话）用于返回稳定的模拟模型响应。
     *
     * @param request 聊天模型请求
     * @return 聊天模型响应
     */
    @Override
    public ChatModelResponse chat(ChatModelRequest request) {
        // 计算输入内容长度，用于模拟 Token（词元）用量。
        int promptLength = calculatePromptLength(request.getMessages());
        // 构造稳定的模拟 JSON（对象表示格式）内容，后续真实生成器会替换为真实模型输出。
        String content = """
                {
                  "theme": "模拟模型生成的创作主题",
                  "targetAudience": "对该主题感兴趣的目标用户",
                  "hook": "你是否也想把一个想法快速变成可执行内容？",
                  "storyboard": ["用问题开场", "展示核心方案", "拆解执行步骤", "总结并引导互动"],
                  "voiceoverScript": "这是一段来自 MockChatModelClient（模拟聊天模型客户端）的稳定旁白脚本。",
                  "subtitleSuggestions": ["痛点是什么", "方案怎么做", "结果怎么看"],
                  "titleCandidates": ["模拟标题一", "模拟标题二", "模拟标题三"],
                  "publishCopy": "这是一段模拟发布文案，用于验证模型客户端抽象。",
                  "hashtags": ["#AI创作", "#内容规划", "#Agent"],
                  "score": 82,
                  "improvementSuggestions": ["接入真实模型", "增加输出修复"]
                }
                """;
        // 使用 Builder（建造者模式）组装模拟模型响应。
        return ChatModelResponse.builder()
                // 设置供应商为 mock（模拟）。
                .provider("mock")
                // 设置模型名称。
                .modelName("mock-chat-model")
                // 设置模型生成内容。
                .content(content)
                // 设置模拟输入 Token（词元）数量。
                .promptTokens(Math.max(1, promptLength / 4))
                // 设置模拟输出 Token（词元）数量。
                .completionTokens(Math.max(1, content.length() / 4))
                // 设置模拟总 Token（词元）数量。
                .totalTokens(Math.max(1, (promptLength + content.length()) / 4))
                // 构建聊天模型响应。
                .build();
    }

    /**
     * calculatePromptLength（计算提示词长度）用于模拟 Token（词元）用量。
     *
     * @param messages 消息列表
     * @return 文本总长度
     */
    private int calculatePromptLength(List<ChatMessage> messages) {
        // 如果消息列表为空，返回 0。
        if (messages == null || messages.isEmpty()) {
            // 返回空长度。
            return 0;
        }
        // 统计所有消息内容长度。
        return messages.stream()
                // 读取消息内容。
                .map(ChatMessage::getContent)
                // 过滤空内容。
                .filter(content -> content != null)
                // 计算每条消息长度。
                .mapToInt(String::length)
                // 求和得到总长度。
                .sum();
    }
}
