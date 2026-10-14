package com.clipforge.memory.summary.conversation;

import com.clipforge.memory.infrastructure.persistence.entity.ConversationMessageEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationSummaryTriggerDecision（会话摘要触发决策）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryTriggerDecision {

    /** shouldSummarize（是否摘要）表示本次是否需要调用 LLM（大语言模型）。 */
    private boolean shouldSummarize;

    /** reason（原因）用于测试打印和链路排查。 */
    private String reason;

    /** summarizableMessages（可摘要消息）表示最近窗口之外、且尚未被摘要的旧消息。 */
    private List<ConversationMessageEntity> summarizableMessages;

    /** totalHistoryTokens（完整会话历史词元估算）表示全部消息的大致 Token（词元）占用。 */
    private int totalHistoryTokens;

    /** unsummarizedChars（未摘要候选字符数）表示本次候选消息正文总字符数。 */
    private int unsummarizedChars;
}
