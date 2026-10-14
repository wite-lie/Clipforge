package com.clipforge.agent.intent;

import com.clipforge.contract.creation.CreatePlanRequest;

/**
 * ConversationIntentAgent（会话意图智能体）。
 *
 * <p>该接口用于识别用户当前轮次到底要生成方案、修正方案、处理素材、查询状态、
 * 查询成本、查看质量复盘，还是需要系统进一步追问。
 */
public interface ConversationIntentAgent {

    /**
     * recognize（识别）用于根据通用会话请求判断用户意图。
     *
     * @param request 会话意图识别请求
     * @return 会话意图决策
     */
    ConversationIntentDecision recognize(ConversationIntentRequest request);

    /**
     * recognize（识别）用于兼容旧的创作方案接口。
     *
     * @param request 创建方案请求
     * @return 会话意图决策
     */
    default ConversationIntentDecision recognize(CreatePlanRequest request) {
        return recognize(ConversationIntentRequest.builder()
                .userId(request == null ? null : request.getUserId())
                .projectId(request == null ? null : request.getProjectId())
                .conversationId(request == null ? null : request.getConversationId())
                .message(request == null ? null : request.getGoal())
                .platform(request == null ? null : request.getPlatform())
                .stylePreference(request == null ? null : request.getStylePreference())
                .materialIds(request == null ? null : request.getMaterialIds())
                .knowledgeBaseIds(request == null ? null : request.getKnowledgeBaseIds())
                .build());
    }
}
