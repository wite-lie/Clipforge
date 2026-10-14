package com.clipforge.contract.creation;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;

/**
 * CreationOutputType（创作产物类型）。
 *
 * <p>该枚举用于区分一次创作运行最终产出的内容形态。
 */
public enum CreationOutputType {

    /** VIDEO_PLAN（短视频方案）表示当前已有的短视频结构化创作方案。 */
    VIDEO_PLAN,

    /** ARTICLE_PLAN（图文方案）表示后续头条、百家号等图文内容创作方案。 */
    ARTICLE_PLAN;

    /**
     * resolveOrDefault（解析或默认）用于兼容旧请求，未传产物类型时默认短视频方案。
     *
     * @param value 原始产物类型
     * @return 解析后的产物类型
     */
    public static CreationOutputType resolveOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return VIDEO_PLAN;
        }
        try {
            return CreationOutputType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的创作产物类型：" + value, exception);
        }
    }
}
