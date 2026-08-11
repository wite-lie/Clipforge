package com.clipforge.memory.summary.conversation;

/**
 * ConversationSummaryTriggerPolicy（会话摘要触发策略）。
 */
public interface ConversationSummaryTriggerPolicy {

    /**
     * decide（决策）用于判断本轮创作前是否需要刷新会话滚动摘要。
     *
     * @param request 触发请求
     * @return 触发决策
     */
    ConversationSummaryTriggerDecision decide(ConversationSummaryTriggerRequest request);
}
