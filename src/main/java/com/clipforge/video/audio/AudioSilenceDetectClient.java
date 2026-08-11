package com.clipforge.video.audio;

/**
 * AudioSilenceDetectClient（音频静音检测客户端）。
 */
public interface AudioSilenceDetectClient {

    /**
     * detect（检测）用于判断音频是否近似静音。
     */
    AudioSilenceDetectResult detect(AudioSilenceDetectRequest request);
}
