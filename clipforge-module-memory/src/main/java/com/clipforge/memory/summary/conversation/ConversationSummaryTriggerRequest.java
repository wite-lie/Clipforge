package com.clipforge.memory.summary.conversation;

import com.clipforge.memory.infrastructure.persistence.entity.ConversationMessageEntity;
import com.clipforge.memory.infrastructure.persistence.entity.ConversationSummaryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationSummaryTriggerRequest（会话摘要触发请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryTriggerRequest {

    /** conversationId（会话编号）表示需要判断的会话。 */
    private String conversationId;

    /** existingSummary（已有摘要）表示当前会话已启用的滚动摘要，可以为空。 */
    private ConversationSummaryEntity existingSummary;

    /** messages（消息列表）表示当前会话按时间升序排列的全部消息。 */
    private List<ConversationMessageEntity> messages;
}
