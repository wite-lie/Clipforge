package com.clipforge.agent.intent;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * RuleBasedConversationIntentAgent（规则会话意图智能体）。
 *
 * <p>该实现不调用 LLM（大语言模型），只负责识别高置信度、低成本、低风险的常见意图。
 * 模糊表达会返回 UNKNOWN（未知），再由 HybridConversationIntentAgent（混合会话意图智能体）决定是否调用模型分类。
 */
@Component
public class RuleBasedConversationIntentAgent implements ConversationIntentAgent {

    /**
     * recognize（识别）用于根据关键词和结构化字段判断用户意图。
     *
     * @param request 会话意图识别请求
     * @return 会话意图决策
     */
    @Override
    public ConversationIntentDecision recognize(ConversationIntentRequest request) {
        String message = normalize(request == null ? null : request.getMessage());
        boolean hasMaterials = request != null && request.getMaterialIds() != null && !request.getMaterialIds().isEmpty();
        boolean hasTasks = request != null && request.getProcessTaskIds() != null && !request.getProcessTaskIds().isEmpty();

        if (hasTasks || containsAny(message, "处理好了吗", "解析好了吗", "进度", "状态", "完成了吗", "还要多久")) {
            return decision(ConversationIntentType.QUERY_MATERIAL_STATUS, 0.92, "QUERY_MATERIAL_STATUS_HANDLER", "命中素材处理状态查询规则", List.of("processTaskIds 或 materialIds"), missingWhen(!hasTasks && !hasMaterials, "processTaskIds 或 materialIds"));
        }
        if (hasMaterials && containsAny(message, "上传", "补充", "新增", "添加", "素材", "图片", "视频")) {
            return decision(ConversationIntentType.PROCESS_MATERIAL, 0.90, "PROCESS_MATERIAL_HANDLER", "本轮携带素材且命中补充素材规则", List.of("materialIds"), List.of());
        }
        if (containsAny(message, "token", "Token", "词元", "消耗", "成本", "花费", "多少钱", "调用次数")) {
            return decision(ConversationIntentType.QUERY_USAGE_COST, 0.88, "USAGE_COST_HANDLER", "命中模型用量和成本查询规则", List.of("runId 或 projectId 或 conversationId"), List.of());
        }
        if (containsAny(message, "评分", "评估", "质量", "复盘", "为什么修正", "改进建议")) {
            return decision(ConversationIntentType.EXPLAIN_EVALUATION, 0.86, "QUALITY_REPORT_HANDLER", "命中质量复盘查询规则", List.of("runId"), List.of());
        }
        if (containsAny(message, "修改", "优化", "调整", "改成", "不满意", "继续", "再生成一版", "换一种")) {
            return decision(ConversationIntentType.REVISE_PLAN, 0.78, "CREATION_WORKFLOW_HANDLER", "命中方案修正规则", List.of("conversationId 或 runId"), List.of());
        }
        if (containsAny(message, "知识库", "资料里", "文档里", "根据资料", "检索", "查一下")) {
            return decision(ConversationIntentType.KNOWLEDGE_QA, 0.76, "KNOWLEDGE_QA_HANDLER", "命中知识问答规则", List.of("knowledgeBaseIds 或 projectId"), List.of());
        }
        if (containsAny(message, "你好", "您好", "嗨", "hello", "hi")) {
            return decision(ConversationIntentType.CHITCHAT, 0.96, "CHITCHAT_HANDLER", "命中寒暄规则", List.of(), List.of());
        }
        if (!hasText(message) || containsAny(message, "帮我看看", "帮我做一下", "弄一下", "这个怎么做")) {
            return decision(ConversationIntentType.CLARIFICATION, 0.70, "CLARIFICATION_HANDLER", "输入缺少明确任务目标", List.of("message"), List.of("message"));
        }
        if (containsAny(message, "生成", "规划", "方案", "脚本", "分镜", "旁白", "标题", "短视频", "抖音", "小红书")) {
            return decision(ConversationIntentType.GENERATE_PLAN, 0.82, "CREATION_WORKFLOW_HANDLER", "命中创作方案生成规则", List.of("message", "platform"), missingForGeneration(request));
        }
        return decision(ConversationIntentType.UNKNOWN, 0.30, "FALLBACK_HANDLER", "规则无法稳定识别", List.of(), List.of());
    }

    /**
     * normalize（标准化）用于把空文本转为空串，并清理首尾空白。
     */
    private String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    /**
     * missingForGeneration（生成缺失项）用于判断创作路由是否缺少目标平台。
     */
    private List<String> missingForGeneration(ConversationIntentRequest request) {
        List<String> missing = new ArrayList<>();
        if (request == null || !hasText(request.getMessage())) {
            missing.add("message");
        }
        if (request == null || !hasText(request.getPlatform())) {
            missing.add("platform");
        }
        return missing;
    }

    /**
     * missingWhen（条件缺失）用于按条件生成缺失字段列表。
     */
    private List<String> missingWhen(boolean condition, String fieldName) {
        return condition ? List.of(fieldName) : List.of();
    }

    /**
     * decision（构造决策）用于统一填充规则识别结果。
     */
    private ConversationIntentDecision decision(ConversationIntentType intentType, double confidence, String routeTarget, String reason, List<String> requiredContext, List<String> missingContext) {
        return ConversationIntentDecision.builder()
                .intentType(intentType)
                .confidence(confidence)
                .recognitionMethod(ConversationIntentRecognitionMethod.RULE)
                .routeTarget(routeTarget)
                .requiredContext(requiredContext)
                .missingContext(missingContext)
                .reason(reason)
                .build();
    }

    /**
     * containsAny（是否包含任一关键词）用于做最小规则匹配。
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * hasText（是否有文本）用于判断字符串是否包含有效内容。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
