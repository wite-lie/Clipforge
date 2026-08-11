package com.clipforge.model.speech;

import java.nio.file.Path;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SpeechRecognitionModelRequest（语音识别模型请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeechRecognitionModelRequest {

    /** 原视频素材编号。 */
    private String materialId;

    /** 音频记录编号。 */
    private String audioId;

    /** 待识别音频文件路径。 */
    private Path audioPath;

    /** 期望识别语言，当前可为空。 */
    private String language;
}
