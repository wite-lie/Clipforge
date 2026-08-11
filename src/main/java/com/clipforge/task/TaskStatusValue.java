package com.clipforge.task;

/**
 * TaskStatusValue（任务状态值）。
 *
 * <p>该枚举用于统一前端任务状态查询接口中的 status（状态）字段。
 */
public enum TaskStatusValue {

    /** PENDING（等待中）表示任务已经创建但尚未开始执行。 */
    PENDING,

    /** PROCESSING（处理中）表示任务正在执行。 */
    PROCESSING,

    /** SUCCEEDED（成功）表示任务已经成功完成。 */
    SUCCEEDED,

    /** FAILED（失败）表示任务执行失败。 */
    FAILED,

    /** SKIPPED（跳过）表示任务被系统策略跳过。 */
    SKIPPED;

    /**
     * value（字符串值）用于写入数据库或返回给前端。
     *
     * @return 状态字符串
     */
    public String value() {
        // 当前状态值直接使用枚举名称。
        return name();
    }
}
