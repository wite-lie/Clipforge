package com.clipforge.material.understanding;

/**
 * MaterialUnderstandingType（素材理解类型）。
 *
 * <p>该枚举用于区分图片视觉理解、视频候选帧视觉理解、OCR（光学字符识别）等基础素材理解类型。
 */
public enum MaterialUnderstandingType {

    /** IMAGE_VISION（图片视觉理解）表示原始图片素材的 VLM（视觉语言模型）理解结果。 */
    IMAGE_VISION,

    /** VIDEO_FRAME_VISION（视频帧视觉理解）表示视频候选帧图片的 VLM（视觉语言模型）理解结果。 */
    VIDEO_FRAME_VISION,

    /** OCR_TEXT（光学字符识别文本）表示图片或视频帧中的文字识别结果。 */
    OCR_TEXT;

    /**
     * value（字符串值）用于写入数据库或返回前端。
     *
     * @return 理解类型字符串
     */
    public String value() {
        // 当前数据库字段直接保存枚举名称。
        return name();
    }
}
