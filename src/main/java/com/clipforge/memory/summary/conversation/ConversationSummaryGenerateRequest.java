package com.clipforge.memory.summary.conversation;

import com.clipforge.infra.persistence.entity.ConversationMessageEntity;
import com.clipforge.infra.persistence.entity.ConversationSummaryEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationSummaryGenerateRequest（会话摘要生成请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryGenerateRequest {

    /** runId（运行编号）用于记录模型调用来源。 */
    private String runId;

    /** conversationId（会话编号）表示摘要所属会话。 */
    private String conversationId;

    /** projectId（项目编号）用于模型用量归属。 */
    private String projectId;

    /** userId（用户编号）用于 Prompt（提示词）上下文说明。 */
    private String userId;

    /** existingSummary（已有摘要）表示上一版滚动摘要，可以为空。 */
    private ConversationSummaryEntity existingSummary;

    /** messagesToSummarize（待摘要消息）表示本轮新增需要压缩的旧消息。 */
    private List<ConversationMessageEntity> messagesToSummarize;
}
