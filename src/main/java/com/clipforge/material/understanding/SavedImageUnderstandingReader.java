package com.clipforge.material.understanding;

import com.clipforge.model.vision.ImageUnderstandingResult;
import java.util.Optional;

/**
 * SavedImageUnderstandingReader（已保存图片理解结果读取器）。
 *
 * <p>该接口用于隔离创作 Workflow（工作流）和数据库实现，让创作期只读取已经完成的图片理解结果，不再同步调用 VLM（视觉语言模型）。
 */
public interface SavedImageUnderstandingReader {

    /**
     * findLatestSucceeded（查询最新成功结果）用于读取某个图片素材最近一次成功的视觉理解结果。
     *
     * @param materialId 素材编号
     * @return 可能存在的图片理解结果
     */
    Optional<ImageUnderstandingResult> findLatestSucceeded(String materialId);
}
