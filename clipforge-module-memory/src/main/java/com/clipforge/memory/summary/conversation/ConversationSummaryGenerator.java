package com.clipforge.memory.summary.conversation;

/**
 * ConversationSummaryGenerator（会话摘要生成器）。
 */
public interface ConversationSummaryGenerator {

    /**
     * generate（生成）用于根据已有摘要和新增旧消息生成新的滚动摘要。
     *
     * @param request 摘要生成请求
     * @return 摘要生成结果
     */
    ConversationSummaryGenerateResult generate(ConversationSummaryGenerateRequest request);
}
