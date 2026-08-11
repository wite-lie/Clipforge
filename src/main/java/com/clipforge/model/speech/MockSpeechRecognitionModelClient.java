package com.clipforge.model.speech;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * MockSpeechRecognitionModelClient（模拟语音识别模型客户端）。
 *
 * <p>不调用真实 ASR（自动语音识别）模型，返回稳定的虎鲸科普转写文本，便于本地链路测试。
 */
@Component
@ConditionalOnProperty(prefix = "clipforge.speech", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockSpeechRecognitionModelClient implements SpeechRecognitionModelClient {

    @Override
    public SpeechRecognitionModelResponse transcribe(SpeechRecognitionModelRequest request) {
        String text = "这是一段关于虎鲸的科普视频，介绍了虎鲸的体型特征、黑白配色、海洋分布、社会性和捕食能力。";
        return SpeechRecognitionModelResponse.builder()
                .provider("mock")
                .modelName("mock-speech-recognition-model")
                .transcriptText(text)
                .speechDetected(true)
                .segments(List.of(SpeechRecognitionSegment.builder()
                        .startSeconds(0.0)
                        .endSeconds(8.0)
                        .text(text)
                        .confidence(1.0)
                        .build()))
                .rawContent("MockSpeechRecognitionModelClient（模拟语音识别模型客户端）返回固定虎鲸科普转写文本。")
                .build();
    }
}
