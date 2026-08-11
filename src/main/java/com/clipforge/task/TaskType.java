package com.clipforge.task;

/**
 * TaskType（任务类型）。
 *
 * <p>该枚举用于区分统一任务状态接口下不同种类的后台任务。
 */
public enum TaskType {

    /** MATERIAL_IMAGE_UNDERSTANDING（图片素材理解）表示图片上传后的理解任务。 */
    MATERIAL_IMAGE_UNDERSTANDING,

    /** MATERIAL_VIDEO_PROCESSING（视频素材处理）表示视频上传后的元数据、抽帧、音频和理解任务。 */
    MATERIAL_VIDEO_PROCESSING;

    /**
     * value（字符串值）用于写入数据库或返回给前端。
     *
     * @return 任务类型字符串
     */
    public String value() {
        // 当前任务类型值直接使用枚举名称。
        return name();
    }
}
