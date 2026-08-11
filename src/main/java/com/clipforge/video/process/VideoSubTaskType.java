package com.clipforge.video.process;

/**
 * VideoSubTaskType（视频子任务类型）用于区分视频父任务下的并行分支和汇总分支。
 */
public enum VideoSubTaskType {

    /** FRAME_BRANCH（抽帧图像分支）负责抽帧、帧素材注册和帧图片理解。 */
    FRAME_BRANCH,

    /** AUDIO_BRANCH（音频分支）负责音频提取和 ASR（自动语音识别）。 */
    AUDIO_BRANCH,

    /** SUMMARY（汇总分支）负责读取图像和音频结果并生成视频理解摘要。 */
    SUMMARY;

    /**
     * value（字符串值）用于写入数据库或消息体。
     *
     * @return 子任务类型字符串
     */
    public String value() {
        // 当前直接使用枚举名作为稳定存储值。
        return name();
    }
}
