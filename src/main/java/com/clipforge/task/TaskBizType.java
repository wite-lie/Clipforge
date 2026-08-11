package com.clipforge.task;

/**
 * TaskBizType（任务业务类型）。
 *
 * <p>该枚举用于说明统一任务状态关联的是哪类业务对象。
 */
public enum TaskBizType {

    /** MATERIAL（素材）表示任务关联图片、视频等素材。 */
    MATERIAL;

    /**
     * value（字符串值）用于写入数据库或返回给前端。
     *
     * @return 业务类型字符串
     */
    public String value() {
        // 当前业务类型值直接使用枚举名称。
        return name();
    }
}
