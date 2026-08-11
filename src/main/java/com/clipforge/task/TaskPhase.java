package com.clipforge.task;

/**
 * TaskPhase（任务阶段）。
 *
 * <p>该枚举用于表达后台任务所处的粗粒度处理阶段。
 */
public enum TaskPhase {

    /** WAITING_IMAGE_UNDERSTANDING（等待图片理解）表示图片已上传但尚未理解。 */
    WAITING_IMAGE_UNDERSTANDING,

    /** WAITING_VIDEO_PROCESSING（等待视频处理）表示视频已上传但尚未进入处理流水线。 */
    WAITING_VIDEO_PROCESSING,

    /** VIDEO_PROCESSING_ORCHESTRATING（视频处理编排中）表示视频父任务正在创建和调度子任务。 */
    VIDEO_PROCESSING_ORCHESTRATING,

    /** IMAGE_UNDERSTANDING（图片理解中）表示正在调用视觉能力理解图片。 */
    IMAGE_UNDERSTANDING,

    /** VIDEO_METADATA（视频元数据）表示正在读取视频元数据。 */
    VIDEO_METADATA,

    /** VIDEO_FRAME_EXTRACT（视频抽帧）表示正在抽取视频候选帧。 */
    VIDEO_FRAME_EXTRACT,

    /** VIDEO_FRAME_BRANCH（视频抽帧图像分支）表示抽帧、帧素材注册和帧图片理解子任务。 */
    VIDEO_FRAME_BRANCH,

    /** VIDEO_AUDIO_EXTRACT（视频音频提取）表示正在提取视频音频。 */
    VIDEO_AUDIO_EXTRACT,

    /** VIDEO_AUDIO_BRANCH（视频音频分支）表示音频提取和 ASR（自动语音识别）子任务。 */
    VIDEO_AUDIO_BRANCH,

    /** VIDEO_ASR（视频语音识别）表示正在进行语音转写。 */
    VIDEO_ASR,

    /** VIDEO_UNDERSTANDING（视频理解）表示正在汇总视频理解结果。 */
    VIDEO_UNDERSTANDING,

    /** VIDEO_SUMMARY（视频汇总）表示正在聚合图像分支和音频分支结果生成视频理解摘要。 */
    VIDEO_SUMMARY,

    /** READY_FOR_CREATION（可用于创作）表示素材处理完成，可以进入创作工作流。 */
    READY_FOR_CREATION,

    /** FAILED（失败）表示处理失败。 */
    FAILED;

    /**
     * value（字符串值）用于写入数据库或返回给前端。
     *
     * @return 任务阶段字符串
     */
    public String value() {
        // 当前阶段值直接使用枚举名称。
        return name();
    }
}
