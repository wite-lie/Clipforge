package com.clipforge.memory.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ConversationSummarySnapshot（会话滚动摘要快照）。
 *
 * <p>该对象表示 Short-term Memory（短期记忆）中已经压缩过的历史会话摘要。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummarySnapshot {

    /** summaryId（摘要编号）用于追踪该滚动摘要。 */
    private String summaryId;

    /** summaryContent（摘要内容）保存压缩后的会话历史。 */
    private String summaryContent;

    /** summarizedUntilMessageId（已摘要到的消息编号）用于审计压缩边界。 */
    private String summarizedUntilMessageId;

    /** summarizedUntilTime（已摘要到的消息时间）用于判断哪些旧消息已压缩。 */
    private LocalDateTime summarizedUntilTime;

    /** sourceMessageCount（源消息数量）表示摘要覆盖了多少条消息。 */
    private Integer sourceMessageCount;

    /** summaryVersion（摘要版本）表示滚动摘要被重写过多少轮。 */
    private Integer summaryVersion;

    /** tokenEstimate（词元估算）表示摘要本身估算占用的 Token（词元）数量。 */
    private Integer tokenEstimate;
}
