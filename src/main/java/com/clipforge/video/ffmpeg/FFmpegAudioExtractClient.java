package com.clipforge.video.ffmpeg;

/**
 * FFmpegAudioExtractClient（FFmpeg 音频提取客户端）。
 *
 * <p>该接口屏蔽本地 FFmpeg（音视频处理工具）命令细节，向应用层提供受控音频提取能力。
 */
public interface FFmpegAudioExtractClient {

    /**
     * extractAudio（提取音频）用于从视频文件中提取音轨。
     *
     * @param request 音频提取请求
     * @return 音频提取结果
     */
    AudioExtractResult extractAudio(AudioExtractRequest request);
}
