package com.clipforge.memory.infrastructure.persistence.enums;

/**
 * CreationTaskStatus（创作任务状态）。
 *
 * <p>该枚举用于统一数据库中 cf_creation_task（创作任务表）的 status（状态）字段取值。
 */
public enum CreationTaskStatus {

    /** CREATED（已创建）表示任务记录已经创建，但还未开始执行。 */
    CREATED,

    /** RUNNING（执行中）表示 Agent（智能体）正在处理任务。 */
    RUNNING,

    /** SUCCEEDED（已成功）表示 Agent（智能体）已经生成创作方案。 */
    SUCCEEDED,

    /** FAILED（已失败）表示任务执行失败。 */
    FAILED
}
