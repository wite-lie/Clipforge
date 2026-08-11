package com.clipforge.video.understanding;

import com.clipforge.infra.persistence.entity.VideoUnderstandingEntity;
import com.clipforge.infra.persistence.repository.VideoUnderstandingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * VideoUnderstandingReadService（视频理解读取服务）。
 *
 * <p>该服务只读取已经保存的视频理解结果，供创作 Workflow（工作流）消费，不在创作期触发任何视频处理。
 */
@Service
@RequiredArgsConstructor
public class VideoUnderstandingReadService {

    /** videoUnderstandingRepositoryProvider（视频理解仓储提供器）用于在 db（数据库）环境下读取结果。 */
    private final ObjectProvider<VideoUnderstandingRepository> videoUnderstandingRepositoryProvider;

    /** objectMapper（JSON 对象映射器）用于把数据库中的理解结果 JSON（对象表示格式）还原为对象。 */
    private final ObjectMapper objectMapper;

    /**
     * readForCreation（读取创作用视频理解上下文）用于按视频素材编号读取可注入创作链路的视频摘要。
     *
     * @param videoMaterialIds 视频素材编号列表
     * @return 视频理解创作上下文
     */
    public VideoUnderstandingCreationContext readForCreation(List<String> videoMaterialIds) {
        // 清洗视频素材编号。
        List<String> safeVideoMaterialIds = safeIds(videoMaterialIds);
        // 如果没有视频素材编号，则返回空上下文。
        if (safeVideoMaterialIds.isEmpty()) {
            // 返回空上下文。
            return emptyContext(List.of(), List.of());
        }
        // 获取视频理解仓储。
        VideoUnderstandingRepository repository = videoUnderstandingRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境），返回警告而不是打断创作。
        if (repository == null) {
            // 返回持久化不可用警告。
            return emptyContext(safeVideoMaterialIds, List.of("视频理解结果读取需要启用 db Profile（数据库配置环境），本次创作将不使用视频摘要。"));
        }
        // 创建可用结果列表。
        List<VideoUnderstandingResult> summaries = new ArrayList<>();
        // 创建警告列表。
        List<String> warnings = new ArrayList<>();
        // 遍历视频素材编号。
        for (String materialId : safeVideoMaterialIds) {
            // 读取视频理解实体。
            repository.findByMaterialId(materialId).ifPresentOrElse(entity -> handleEntity(entity, summaries, warnings), () -> warnings.add("视频素材 " + materialId + " 没有可用视频理解结果，请先完成视频理解处理。"));
        }
        // 构建摘要文本。
        String summaryText = buildSummaryText(summaries);
        // 返回创作用视频上下文。
        return VideoUnderstandingCreationContext.builder()
                .videoMaterialIds(safeVideoMaterialIds)
                .summaries(summaries)
                .summaryText(summaryText)
                .warnings(warnings)
                .build();
    }

    /**
     * handleEntity（处理实体）用于校验状态并解析视频理解结果。
     *
     * @param entity 视频理解实体
     * @param summaries 可用结果列表
     * @param warnings 警告列表
     */
    private void handleEntity(VideoUnderstandingEntity entity, List<VideoUnderstandingResult> summaries, List<String> warnings) {
        // 如果理解状态不可用于创作，则记录警告。
        if (!isUsableStatus(entity.getUnderstandingStatus())) {
            // 添加状态不可用警告。
            warnings.add("视频素材 " + entity.getMaterialId() + " 的理解状态不可用于创作，状态：" + entity.getUnderstandingStatus());
            // 直接返回。
            return;
        }
        // 尝试解析理解结果。
        try {
            // 如果理解结果 JSON 为空，则记录警告。
            if (entity.getUnderstandingJson() == null || entity.getUnderstandingJson().isBlank()) {
                // 添加结果为空警告。
                warnings.add("视频素材 " + entity.getMaterialId() + " 的视频理解结果为空。");
                // 直接返回。
                return;
            }
            // 反序列化理解结果。
            VideoUnderstandingResult result = objectMapper.readValue(entity.getUnderstandingJson(), VideoUnderstandingResult.class);
            // 如果摘要为空，则记录警告。
            if (result.getSummaryForCreation() == null || result.getSummaryForCreation().isBlank()) {
                // 添加摘要为空警告。
                warnings.add("视频素材 " + entity.getMaterialId() + " 的视频理解结果缺少 summaryForCreation（创作注入摘要）。");
                // 直接返回。
                return;
            }
            // 添加可用结果。
            summaries.add(result);
        } catch (JsonProcessingException exception) {
            // 解析失败时记录警告，不打断创作。
            warnings.add("视频素材 " + entity.getMaterialId() + " 的视频理解 JSON（对象表示格式）解析失败：" + exception.getOriginalMessage());
        }
    }

    /**
     * isUsableStatus（是否可用状态）用于判断视频理解结果是否可以进入创作链路。
     *
     * @param status 状态值
     * @return 是否可用
     */
    private boolean isUsableStatus(String status) {
        // 成功和部分成功都可以作为创作上下文。
        return VideoUnderstandingStatus.SUCCEEDED.getValue().equals(status)
                || VideoUnderstandingStatus.PARTIAL_SUCCEEDED.getValue().equals(status);
    }

    /**
     * buildSummaryText（构建摘要文本）用于把多个视频理解结果合并为 Prompt（提示词）片段。
     *
     * @param summaries 视频理解结果列表
     * @return 摘要文本
     */
    private String buildSummaryText(List<VideoUnderstandingResult> summaries) {
        // 如果没有可用结果，则返回空文本。
        if (summaries == null || summaries.isEmpty()) {
            // 返回空文本。
            return "";
        }
        // 创建字符串构建器。
        StringBuilder builder = new StringBuilder();
        // 遍历视频理解结果。
        for (int index = 0; index < summaries.size(); index++) {
            // 读取当前结果。
            VideoUnderstandingResult result = summaries.get(index);
            // 追加视频序号。
            builder.append("【视频 ").append(index + 1).append("】").append(System.lineSeparator());
            // 追加视频素材编号。
            builder.append("视频素材编号：").append(result.getMaterialId()).append(System.lineSeparator());
            // 追加创作注入摘要。
            builder.append(result.getSummaryForCreation()).append(System.lineSeparator());
        }
        // 返回合并后的摘要文本。
        return builder.toString().stripTrailing();
    }

    /**
     * emptyContext（空上下文）用于返回带警告的空视频理解创作上下文。
     *
     * @param videoMaterialIds 视频素材编号列表
     * @param warnings 警告列表
     * @return 视频理解创作上下文
     */
    private VideoUnderstandingCreationContext emptyContext(List<String> videoMaterialIds, List<String> warnings) {
        // 构建空上下文。
        return VideoUnderstandingCreationContext.builder()
                .videoMaterialIds(videoMaterialIds)
                .summaries(List.of())
                .summaryText("")
                .warnings(warnings)
                .build();
    }

    /**
     * safeIds（安全编号列表）用于清洗空白编号并保持用户选择顺序。
     *
     * @param ids 原始编号列表
     * @return 清洗后的编号列表
     */
    private List<String> safeIds(List<String> ids) {
        // 如果原始列表为空，则返回空列表。
        if (ids == null || ids.isEmpty()) {
            // 返回空列表。
            return List.of();
        }
        // 清洗并去重。
        return ids.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
    }
}
