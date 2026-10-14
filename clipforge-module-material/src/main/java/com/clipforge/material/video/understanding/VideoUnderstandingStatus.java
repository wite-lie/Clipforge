package com.clipforge.material.video.understanding;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * VideoUnderstandingStatus（视频理解状态）。
 *
 * <p>该枚举用于描述视频理解结果的整体状态，区分完整成功、部分成功和失败。
 */
@Getter
@RequiredArgsConstructor
public enum VideoUnderstandingStatus {

    /** SUCCEEDED（成功）表示元数据、视觉线索和音频线索都可正常使用。 */
    SUCCEEDED("SUCCEEDED"),

    /** PARTIAL_SUCCEEDED（部分成功）表示部分线索缺失，但仍然生成了可用理解结果。 */
    PARTIAL_SUCCEEDED("PARTIAL_SUCCEEDED"),

    /** FAILED（失败）表示关键前置数据缺失，无法生成视频理解结果。 */
    FAILED("FAILED");

    /** value（状态值）用于数据库保存和接口返回。 */
    private final String value;
}
