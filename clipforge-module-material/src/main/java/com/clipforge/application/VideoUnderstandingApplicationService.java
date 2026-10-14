package com.clipforge.application;

import com.clipforge.agent.video.VideoUnderstandingAgent;
import com.clipforge.contract.knowledge.VideoUnderstandingResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.material.infrastructure.persistence.entity.VideoUnderstandingEntity;
import com.clipforge.material.infrastructure.persistence.repository.VideoUnderstandingRepository;
import com.clipforge.material.video.understanding.VideoUnderstandingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * VideoUnderstandingApplicationService（视频理解应用服务）。
 *
 * <p>该服务负责调用 VideoUnderstandingAgent（视频理解智能体）、保存理解结果，并提供结果查询。
 */
@Service
@RequiredArgsConstructor
public class VideoUnderstandingApplicationService {

    /** videoUnderstandingRepositoryProvider（视频理解仓储提供器）用于保存和查询视频理解结果。 */
    private final ObjectProvider<VideoUnderstandingRepository> videoUnderstandingRepositoryProvider;

    /** videoUnderstandingAgent（视频理解智能体）负责生成视频理解结果。 */
    private final VideoUnderstandingAgent videoUnderstandingAgent;

    /** objectMapper（JSON 对象映射器）用于保存和读取结构化 JSON。 */
    private final ObjectMapper objectMapper;

    /**
     * understand（理解视频）用于执行视频理解并保存最新结果。
     *
     * @param materialId 视频素材编号
     * @param runId 运行编号
     * @param instruction 理解指令
     * @return 视频理解响应
     */
    public VideoUnderstandingResponse understand(String materialId, String runId, String instruction) {
        // 校验素材编号不能为空。
        if (materialId == null || materialId.isBlank()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        // 生成默认运行编号。
        String safeRunId = runId == null || runId.isBlank() ? newId("run") : runId;
        // 调用视频理解智能体。
        VideoUnderstandingResult result = videoUnderstandingAgent.understand(safeRunId, materialId, instruction);
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 构建视频理解实体。
        VideoUnderstandingEntity entity = VideoUnderstandingEntity.builder()
                .id(newId("video_understanding_pk"))
                .understandingId(newId("video_understanding"))
                .materialId(materialId)
                .videoMetadataSnapshot(buildMetadataSnapshot(result))
                .frameSummarySnapshot(result.getVisualSummary())
                .audioTranscriptSnapshot(normalizeEmptyAudioSummary(result.getAudioSummary()))
                .understandingJson(toJson(result))
                .understandingStatus(result.getStatus())
                .errorMessage(joinRiskNotes(result))
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        // 保存或更新视频理解结果。
        VideoUnderstandingEntity savedEntity = requiredVideoUnderstandingRepository().saveOrUpdateByMaterialId(entity);
        // 转换并返回接口响应。
        return toResponse(savedEntity);
    }

    /**
     * queryUnderstanding（查询视频理解）用于读取已经保存的视频理解结果。
     *
     * @param materialId 视频素材编号
     * @return 视频理解响应
     */
    public VideoUnderstandingResponse queryUnderstanding(String materialId) {
        // 校验素材编号不能为空。
        if (materialId == null || materialId.isBlank()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "materialId（素材编号）不能为空");
        }
        // 查询视频理解结果。
        VideoUnderstandingEntity entity = requiredVideoUnderstandingRepository().findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到视频理解结果，请先调用视频理解接口，materialId：" + materialId));
        // 转换并返回接口响应。
        return toResponse(entity);
    }

    /**
     * toResponse（转响应）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 视频理解实体
     * @return 视频理解响应
     */
    private VideoUnderstandingResponse toResponse(VideoUnderstandingEntity entity) {
        // 构建视频理解响应，并对前端展示内容做一次脱敏。
        return VideoUnderstandingResponse.builder()
                .understandingId(entity.getUnderstandingId())
                .materialId(entity.getMaterialId())
                .understandingStatus(entity.getUnderstandingStatus())
                .result(sanitizeForDisplay(fromJson(entity.getUnderstandingJson())))
                .errorMessage(sanitizeErrorMessage(entity.getErrorMessage()))
                .build();
    }

    /**
     * toJson（转 JSON）用于把视频理解结果序列化为文本。
     *
     * @param result 视频理解结果
     * @return JSON 文本
     */
    private String toJson(VideoUnderstandingResult result) {
        // 尝试序列化 JSON。
        try {
            // 返回 JSON 文本。
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            // 抛出模型输出解析异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频理解结果序列化失败", exception);
        }
    }

    /**
     * fromJson（从 JSON 读取）用于把数据库中的理解结果还原为对象。
     *
     * @param json JSON 文本
     * @return 视频理解结果
     */
    private VideoUnderstandingResult fromJson(String json) {
        // 如果 JSON 为空，则返回空对象。
        if (json == null || json.isBlank()) {
            // 返回空对象。
            return null;
        }
        // 尝试反序列化 JSON。
        try {
            // 返回视频理解结果。
            return objectMapper.readValue(json, VideoUnderstandingResult.class);
        } catch (JsonProcessingException exception) {
            // 抛出解析异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频理解结果反序列化失败", exception);
        }
    }

    /**
     * joinRiskNotes（合并风险提示）用于把风险列表保存到错误消息字段，方便查询列表时快速判断。
     *
     * @param result 视频理解结果
     * @return 风险提示文本
     */
    private String joinRiskNotes(VideoUnderstandingResult result) {
        // 如果没有风险提示，则返回空。
        if (result.getRiskNotes() == null || result.getRiskNotes().isEmpty()) {
            // 返回空。
            return null;
        }
        // 合并风险提示。
        return String.join("；", result.getRiskNotes());
    }

    /**
     * buildMetadataSnapshot（构建元数据快照）用于在没有重新暴露上下文对象的情况下保存可复盘的视频基本信息。
     *
     * @param result 视频理解结果
     * @return 元数据快照文本
     */
    private String buildMetadataSnapshot(VideoUnderstandingResult result) {
        // 当前第一版由 VideoUnderstandingResult（视频理解结果）的创作注入摘要承载视频基本信息。
        return result.getSummaryForCreation();
    }

    /**
     * normalizeEmptyAudioSummary（规范化空音频摘要）用于避免把兜底提示误当成真实转写文本。
     *
     * @param audioSummary 音频摘要
     * @return 可保存的音频快照
     */
    private String normalizeEmptyAudioSummary(String audioSummary) {
        // 如果音频摘要为空，则返回空。
        if (audioSummary == null || audioSummary.isBlank()) {
            // 返回空。
            return null;
        }
        // 如果是系统兜底提示，也按空处理。
        if ("当前没有可用音频摘要。".equals(audioSummary)) {
            // 返回空。
            return null;
        }
        // 返回真实音频摘要。
        return audioSummary;
    }

    /**
     * sanitizeForDisplay（展示脱敏）用于移除视频理解结果中的内部帧编号。
     *
     * @param result 原始结果
     * @return 脱敏后的结果
     */
    private VideoUnderstandingResult sanitizeForDisplay(VideoUnderstandingResult result) {
        if (result == null) {
            return null;
        }
        VideoUnderstandingResult copy = objectMapper.convertValue(result, VideoUnderstandingResult.class);
        copy.setSummaryForCreation(sanitizeText(copy.getSummaryForCreation()));
        copy.setVisualSummary(sanitizeText(copy.getVisualSummary()));
        copy.setAudioSummary(sanitizeText(copy.getAudioSummary()));
        copy.setOverallTheme(sanitizeText(copy.getOverallTheme()));
        copy.setHighlightCandidates(sanitizeTextList(copy.getHighlightCandidates()));
        copy.setRiskNotes(sanitizeTextList(copy.getRiskNotes()));
        copy.setCreativeAngles(sanitizeTextList(copy.getCreativeAngles()));
        copy.setKeyVisualMoments(sanitizeTextList(copy.getKeyVisualMoments()));
        return copy;
    }

    /**
     * sanitizeText（文本脱敏）用于去掉内部素材编号。
     */
    private String sanitizeText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        return text
                .replaceAll("material_frame_[0-9a-f]+", "[内部帧素材]")
                .replaceAll("video_frame_[0-9a-f]+", "[内部视频帧]")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    /**
     * sanitizeTextList（列表脱敏）用于批量去掉内部编号。
     */
    private List<String> sanitizeTextList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        List<String> sanitized = new ArrayList<>();
        for (String value : values) {
            String text = sanitizeText(value);
            if (text != null && !text.isBlank() && !sanitized.contains(text)) {
                sanitized.add(text);
            }
        }
        return sanitized;
    }

    /**
     * sanitizeErrorMessage（错误信息脱敏）用于展示层压缩重复风险提示。
     */
    private String sanitizeErrorMessage(String errorMessage) {
        String sanitized = sanitizeText(errorMessage);
        if (sanitized == null || sanitized.isBlank()) {
            return sanitized;
        }
        String[] parts = sanitized.split("[；;]");
        List<String> uniqueParts = new ArrayList<>();
        for (String part : parts) {
            String text = part == null ? null : part.strip();
            if (text != null && !text.isBlank() && !uniqueParts.contains(text)) {
                uniqueParts.add(text);
            }
        }
        return String.join("；", uniqueParts);
    }

    /**
     * requiredVideoUnderstandingRepository（获取必需视频理解仓储）用于处理未启用数据库的场景。
     *
     * @return 视频理解仓储
     */
    private VideoUnderstandingRepository requiredVideoUnderstandingRepository() {
        // 从 Spring（Spring 框架）容器中读取仓储。
        VideoUnderstandingRepository repository = videoUnderstandingRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，则抛出持久化未启用异常。
        if (repository == null) {
            // 抛出异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "视频理解接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回仓储。
        return repository;
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成第一版业务编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
