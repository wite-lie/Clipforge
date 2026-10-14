package com.clipforge.application;

import com.clipforge.contract.material.MaterialUnderstandingResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.application.MaterialUnderstandingQueryUseCase;
import com.clipforge.material.infrastructure.persistence.entity.MaterialUnderstandingEntity;
import com.clipforge.material.infrastructure.persistence.repository.MaterialUnderstandingRepository;
import com.clipforge.material.understanding.MaterialUnderstandingType;
import com.clipforge.ai.api.vision.ImageUnderstandingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * MaterialUnderstandingQueryApplicationService（素材理解结果查询应用服务）。
 *
 * <p>该服务负责查询已经落库的素材理解结果，并把数据库实体转换为接口响应对象。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class MaterialUnderstandingQueryApplicationService implements MaterialUnderstandingQueryUseCase {

    /** materialUnderstandingRepository（素材理解结果仓储）用于读取素材理解结果表。 */
    private final MaterialUnderstandingRepository materialUnderstandingRepository;

    /** objectMapper（对象映射器）用于解析结构化理解结果 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /**
     * queryLatestImageUnderstanding（查询最新图片理解结果）用于返回某个图片素材最近一次成功的视觉理解结果。
     *
     * @param materialId 素材编号
     * @return 素材理解结果响应
     */
    @Override
    public MaterialUnderstandingResponse queryLatestImageUnderstanding(String materialId) {
        // 校验素材编号，避免无意义查询。
        if (materialId == null || materialId.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "素材编号不能为空");
        }
        // 查询最新成功的 IMAGE_VISION（图片视觉理解）结果。
        MaterialUnderstandingEntity entity = materialUnderstandingRepository
                .findLatestSucceeded(materialId, MaterialUnderstandingType.IMAGE_VISION.value())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_READY, "图片素材尚未完成理解，materialId=" + materialId));
        // 转换为接口响应对象。
        return toResponse(entity);
    }

    /**
     * toResponse（转响应对象）用于把数据库实体转换为前端更容易使用的 DTO（数据传输对象）。
     *
     * @param entity 素材理解结果实体
     * @return 素材理解结果响应
     */
    public MaterialUnderstandingResponse toResponse(MaterialUnderstandingEntity entity) {
        // 解析结构化图片理解结果。
        ImageUnderstandingResult result = parseImageUnderstandingResult(entity.getStructuredResultJson());
        // 构建接口响应。
        return MaterialUnderstandingResponse.builder()
                .understandingId(entity.getUnderstandingId())
                .materialId(entity.getMaterialId())
                .materialType(entity.getMaterialType())
                .understandingType(entity.getUnderstandingType())
                .status(entity.getStatus())
                .modelProvider(entity.getModelProvider())
                .modelName(entity.getModelName())
                .promptVersion(entity.getPromptVersion())
                .contentHash(entity.getContentHash())
                .contentSummary(entity.getContentSummary())
                .imageUnderstandingResult(result)
                .inputTokenCount(entity.getInputTokenCount())
                .outputTokenCount(entity.getOutputTokenCount())
                .totalTokenCount(entity.getTotalTokenCount())
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .createTime(entity.getCreateTime())
                .build();
    }

    /**
     * parseImageUnderstandingResult（解析图片理解结果）用于把 JSON（对象表示格式）字符串还原为结构化对象。
     *
     * @param structuredResultJson 结构化结果 JSON
     * @return 图片理解结果
     */
    private ImageUnderstandingResult parseImageUnderstandingResult(String structuredResultJson) {
        // 校验结构化结果是否存在。
        if (structuredResultJson == null || structuredResultJson.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "素材理解结果缺少结构化 JSON");
        }
        try {
            // 使用 Jackson（JSON 处理库）解析结构化图片理解结果。
            return objectMapper.readValue(structuredResultJson, ImageUnderstandingResult.class);
        } catch (JsonProcessingException exception) {
            // 解析失败说明历史数据异常，转换为业务异常方便定位。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "解析素材理解结果 JSON 失败：" + exception.getMessage(), exception);
        }
    }
}
