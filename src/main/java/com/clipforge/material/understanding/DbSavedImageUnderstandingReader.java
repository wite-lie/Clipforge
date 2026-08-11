package com.clipforge.material.understanding;

import com.clipforge.infra.persistence.repository.MaterialUnderstandingRepository;
import com.clipforge.model.vision.ImageUnderstandingResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DbSavedImageUnderstandingReader（数据库已保存图片理解结果读取器）。
 *
 * <p>该实现从 cf_material_understanding（素材理解结果表）读取 IMAGE_VISION（图片视觉理解）成功结果，供创作 Workflow（工作流）复用。
 */
@Component
@Profile("db")
@RequiredArgsConstructor
public class DbSavedImageUnderstandingReader implements SavedImageUnderstandingReader {

    /** materialUnderstandingRepository（素材理解结果仓储）用于读取已保存理解结果。 */
    private final MaterialUnderstandingRepository materialUnderstandingRepository;

    /** objectMapper（对象映射器）用于把 JSON（对象表示格式）转换为图片理解结果对象。 */
    private final ObjectMapper objectMapper;

    /**
     * findLatestSucceeded（查询最新成功结果）用于读取某个图片素材最近一次成功的视觉理解结果。
     *
     * @param materialId 素材编号
     * @return 可能存在的图片理解结果
     */
    @Override
    public Optional<ImageUnderstandingResult> findLatestSucceeded(String materialId) {
        // 空素材编号无法查询，直接返回空结果。
        if (materialId == null || materialId.isBlank()) {
            return Optional.empty();
        }
        // 查询最新成功的 IMAGE_VISION（图片视觉理解）结果并解析结构化 JSON。
        return materialUnderstandingRepository.findLatestSucceeded(materialId, MaterialUnderstandingType.IMAGE_VISION.value())
                .flatMap(entity -> parse(entity.getStructuredResultJson()));
    }

    /**
     * parse（解析）用于把数据库中的 JSON（对象表示格式）还原为 ImageUnderstandingResult（图片理解结果）。
     *
     * @param json 结构化结果 JSON
     * @return 可能存在的图片理解结果
     */
    private Optional<ImageUnderstandingResult> parse(String json) {
        // 空 JSON 视为无可用结果。
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            // 使用 Jackson（JSON 处理库）解析图片理解结果。
            return Optional.of(objectMapper.readValue(json, ImageUnderstandingResult.class));
        } catch (Exception exception) {
            // 解析失败时返回空结果，让上层按素材未就绪处理。
            return Optional.empty();
        }
    }
}
