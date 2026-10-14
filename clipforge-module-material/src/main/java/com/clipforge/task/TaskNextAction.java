package com.clipforge.task;

/**
 * TaskNextAction（任务下一步动作）。
 *
 * <p>该枚举用于给前端提供任务状态之后的简单操作建议。
 */
public enum TaskNextAction {

    /** WAIT_PROCESSING（等待处理）表示前端应继续等待或轮询状态。 */
    WAIT_PROCESSING,

    /** ALLOW_CREATION（允许创作）表示前端可以允许用户勾选素材进入创作。 */
    ALLOW_CREATION,

    /** RETRY_PROCESSING（重试处理）表示前端可以展示重试入口。 */
    RETRY_PROCESSING;

    /**
     * value（字符串值）用于写入数据库或返回给前端。
     *
     * @return 下一步动作字符串
     */
    public String value() {
        // 当前动作值直接使用枚举名称。
        return name();
    }
}
