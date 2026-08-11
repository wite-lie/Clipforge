package com.clipforge.trace;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TraceEvent（执行链路事件）。
 *
 * <p>该对象记录 Agent（智能体）执行过程中的一个关键事件。
 */
@Getter
@RequiredArgsConstructor
public class TraceEvent {

    /** runId（运行编号）表示事件所属的智能体执行。 */
    private final String runId;

    /** eventType（事件类型）表示事件的机器可读标识。 */
    private final String eventType;

    /** message（事件消息）表示事件的人类可读说明。 */
    private final String message;

    /** detail（事件详情）表示事件的补充信息。 */
    private final String detail;

    /** occurredAt（发生时间）表示事件记录的时间点。 */
    private final OffsetDateTime occurredAt;
}
