package com.clipforge.model.speech;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SpeechRecognitionProperties（语音识别配置属性）。
 *
 * <p>用于配置真实 ASR（自动语音识别）供应商、模型、接口地址和音频格式参数。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "clipforge.speech")
public class SpeechRecognitionProperties {

    /** 是否启用 Mock（模拟）语音识别。 */
    private boolean mockEnabled = true;

    /** ASR（自动语音识别）供应商。 */
    private String provider = "bailian";

    /** ASR（自动语音识别）接口地址。 */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /** API Key（接口密钥）。 */
    private String apiKey;

    /** 模型名称。 */
    private String modelName = "qwen-audio-3.0-asr-flash";

    /** 请求超时时间，单位秒。 */
    private long timeoutSeconds = 120;

    /** 传给 ASR（自动语音识别）接口的音频格式。 */
    private String audioFormat = "wav";

    /** 传给 ASR（自动语音识别）接口的采样率。 */
    private int sampleRate = 16000;

    /** 是否打印请求，用于本地真实模型测试。 */
    private boolean logRequests = false;

    /** 是否打印响应，用于本地真实模型测试。 */
    private boolean logResponses = false;
}
