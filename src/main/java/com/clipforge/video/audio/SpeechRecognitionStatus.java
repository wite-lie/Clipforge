package com.clipforge.video.audio;

/**
 * SpeechRecognitionStatus（语音识别状态）。
 *
 * <p>该枚举用于描述 ASR（自动语音识别）转写结果。
 */
public enum SpeechRecognitionStatus {

    /** PENDING（待识别）表示音频已经准备好但尚未执行 ASR（自动语音识别）。 */
    PENDING,

    /** SKIPPED（跳过）表示无音轨或音频提取失败，因此跳过 ASR（自动语音识别）。 */
    SKIPPED,

    /** SUCCEEDED（识别成功）表示已经生成转写文本。 */
    SUCCEEDED,

    /** NO_SPEECH_DETECTED（未检测到人声）表示没有得到有效人声文本。 */
    NO_SPEECH_DETECTED,

    /** FAILED（识别失败）表示 ASR（自动语音识别）调用失败。 */
    FAILED;

    /**
     * value（数据库值）用于返回写入数据库的状态字符串。
     *
     * @return 数据库状态值
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
