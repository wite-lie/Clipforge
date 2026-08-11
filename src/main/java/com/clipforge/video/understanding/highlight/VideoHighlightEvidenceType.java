package com.clipforge.video.understanding.highlight;

/**
 * VideoHighlightEvidenceType（视频高光证据类型）。
 *
 * <p>该枚举用于说明某个高光候选片段为什么被推荐，便于后续排序、复盘和前端展示。
 */
public enum VideoHighlightEvidenceType {

    /** KEY_FRAME（关键帧）表示片段附近存在最终入选关键帧。 */
    KEY_FRAME,

    /** FRAME_QUALITY（帧质量）表示候选帧质量分较高。 */
    FRAME_QUALITY,

    /** VISUAL_SUBJECT（视觉主体）表示画面主体或关键元素命中用户目标。 */
    VISUAL_SUBJECT,

    /** SUBTITLE_OVERLAP（字幕重叠）表示片段附近存在 ASR（自动语音识别）字幕。 */
    SUBTITLE_OVERLAP,

    /** SUBTITLE_KEYWORD（字幕关键词）表示字幕文本命中用户目标或高价值词。 */
    SUBTITLE_KEYWORD,

    /** OPENING_HOOK（开头钩子）表示片段位于视频前段，适合做强开头。 */
    OPENING_HOOK
}
