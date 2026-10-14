package com.clipforge.video.ffmpeg;

/**
 * FFmpegFrameExtractClient（FFmpeg 帧抽取客户端）。
 *
 * <p>该接口定义从视频中抽取单张候选帧图片的能力。
 */
public interface FFmpegFrameExtractClient {

    /**
     * extractFrame（抽取帧）用于按指定时间点生成一张候选帧图片。
     *
     * @param request 帧抽取请求
     * @return 帧抽取结果
     */
    FrameExtractResult extractFrame(FrameExtractRequest request);
}
