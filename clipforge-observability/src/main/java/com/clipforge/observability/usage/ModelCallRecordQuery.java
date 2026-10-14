package com.clipforge.observability.usage;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ModelCallRecordQuery（模型调用记录查询条件）。
 *
 * <p>该对象用于按运行、项目、会话、供应商、模型、用途、状态和时间范围查询模型调用明细。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelCallRecordQuery {

    /** runId（运行编号）用于过滤某一次 Agent（智能体）执行。 */
    private String runId;

    /** projectId（项目编号）用于过滤某个 Project（项目）。 */
    private String projectId;

    /** conversationId（会话编号）用于过滤某个 Conversation（会话）。 */
    private String conversationId;

    /** provider（供应商）用于过滤模型服务来源。 */
    private String provider;

    /** modelName（模型名称）用于过滤具体模型。 */
    private String modelName;

    /** purpose（调用用途）用于过滤生成、评估、修正、图片理解等场景。 */
    private String purpose;

    /** success（是否成功）用于过滤成功或失败调用。 */
    private Boolean success;

    /** startTime（开始时间）用于过滤创建时间下界，包含该时间。 */
    private LocalDateTime startTime;

    /** endTime（结束时间）用于过滤创建时间上界，包含该时间。 */
    private LocalDateTime endTime;
}
