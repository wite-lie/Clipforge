package com.clipforge.video.audio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * VideoAudioProperties（视频音频配置属性）。
 *
 * <p>该配置集中管理音频输出目录、音频格式、采样率和声道数。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.video.audio")
public class VideoAudioProperties {

    /** outputRootPath（输出根路径）表示提取出的音频文件本地保存目录。 */
    private String outputRootPath = "data/uploads/video-audios";

    /** audioFormat（音频格式）表示输出音频后缀，当前默认 wav。 */
    private String audioFormat = "wav";

    /** sampleRate（采样率）表示输出音频采样率，默认 16000，适合多数 ASR（自动语音识别）模型。 */
    private int sampleRate = 16000;

    /** channels（声道数）表示输出音频声道数，默认 1，也就是单声道。 */
    private int channels = 1;
}
