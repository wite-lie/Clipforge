package com.clipforge.material.application;

import com.clipforge.contract.material.MaterialUnderstandingResponse;

/**
 * MaterialUnderstandingQueryUseCase（素材理解查询用例）。
 *
 * <p>该接口只读取已经保存的图片理解结果，不触发 VLM（视觉语言模型）调用。
 */
public interface MaterialUnderstandingQueryUseCase {

    /**
     * queryLatestImageUnderstanding（查询最新图片理解结果）返回某个图片素材最近一次成功理解结果。
     */
    MaterialUnderstandingResponse queryLatestImageUnderstanding(String materialId);
}
