package com.clipforge.model.usage;

/**
 * ModelUsageGroupBy（模型用量分组维度）。
 *
 * <p>该枚举用于控制模型用量统计接口按哪个维度聚合。
 */
public enum ModelUsageGroupBy {

    /** NONE（不分组）表示只返回整体汇总。 */
    NONE,

    /** RUN（运行）表示按 runId（运行编号）分组。 */
    RUN,

    /** PROJECT（项目）表示按 projectId（项目编号）分组。 */
    PROJECT,

    /** CONVERSATION（会话）表示按 conversationId（会话编号）分组。 */
    CONVERSATION,

    /** DATE（日期）表示按创建日期分组。 */
    DATE,

    /** PROVIDER（供应商）表示按模型供应商分组。 */
    PROVIDER,

    /** MODEL（模型）表示按模型名称分组。 */
    MODEL,

    /** PURPOSE（用途）表示按模型调用用途分组。 */
    PURPOSE,

    /** PROVIDER_MODEL_PURPOSE（供应商模型用途）表示按供应商、模型和用途组合分组。 */
    PROVIDER_MODEL_PURPOSE
}
