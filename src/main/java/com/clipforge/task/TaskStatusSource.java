package com.clipforge.task;

/**
 * TaskStatusSource（任务状态来源）。
 *
 * <p>该枚举用于说明统一任务状态接口的数据来源。
 */
public enum TaskStatusSource {

    /** DATABASE（数据库）表示状态来自真实任务表。 */
    DATABASE,

    /** MATERIAL_DERIVED（素材派生）表示状态由历史素材记录临时派生。 */
    MATERIAL_DERIVED,

    /** REDIS（缓存）表示状态来自 Redis，后续优化项 3 接入。 */
    REDIS;

    /**
     * value（字符串值）用于返回给前端。
     *
     * @return 状态来源字符串
     */
    public String value() {
        // 当前来源值直接使用枚举名称。
        return name();
    }
}
