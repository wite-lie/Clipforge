package com.clipforge.material.understanding;

import com.clipforge.model.vision.ImageUnderstandingResult;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * NoOpSavedImageUnderstandingReader（空已保存图片理解结果读取器）。
 *
 * <p>该实现用于未启用数据库时保证 Spring（应用框架）可以启动。它不会调用 VLM（视觉语言模型），只返回空结果。
 */
@Component
@Profile("!db")
public class NoOpSavedImageUnderstandingReader implements SavedImageUnderstandingReader {

    /**
     * findLatestSucceeded（查询最新成功结果）在空实现中始终返回空。
     *
     * @param materialId 素材编号
     * @return 空结果
     */
    @Override
    public Optional<ImageUnderstandingResult> findLatestSucceeded(String materialId) {
        // 未启用数据库时没有可读取的已保存图片理解结果。
        return Optional.empty();
    }
}
