package com.clipforge.memory.read;

import com.clipforge.infra.persistence.entity.ConversationSummaryEntity;
import com.clipforge.infra.persistence.entity.CreationHistoryMemoryEntity;
import com.clipforge.infra.persistence.entity.ProjectMemoryEntity;
import com.clipforge.infra.persistence.entity.UserPreferenceMemoryEntity;
import com.clipforge.infra.persistence.repository.ConversationSummaryRepository;
import com.clipforge.infra.persistence.repository.CreationHistoryMemoryRepository;
import com.clipforge.infra.persistence.repository.ProjectMemoryRepository;
import com.clipforge.infra.persistence.repository.UserPreferenceMemoryRepository;
import com.clipforge.memory.model.ConversationSummarySnapshot;
import com.clipforge.memory.model.CreationHistoryMemorySnapshot;
import com.clipforge.memory.model.MemoryContext;
import com.clipforge.memory.model.MemoryReadStats;
import com.clipforge.memory.model.ProjectMemorySnapshot;
import com.clipforge.memory.model.UserPreferenceMemory;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * DefaultMemoryReader（默认记忆读取器）。
 *
 * <p>该实现从 PostgreSQL（关系数据库）读取用户偏好、项目记忆、历史记忆和会话摘要，并组装为 MemoryContext（记忆上下文）。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class DefaultMemoryReader implements MemoryReader {

    /** userPreferenceMemoryRepository（用户偏好记忆仓储）负责读取用户长期偏好。 */
    private final UserPreferenceMemoryRepository userPreferenceMemoryRepository;

    /** projectMemoryRepository（项目记忆仓储）负责读取项目画像。 */
    private final ProjectMemoryRepository projectMemoryRepository;

    /** creationHistoryMemoryRepository（创作历史记忆仓储）负责读取历史高质量方案摘要。 */
    private final CreationHistoryMemoryRepository creationHistoryMemoryRepository;

    /** conversationSummaryRepository（会话滚动摘要仓储）负责读取短期记忆摘要。 */
    private final ConversationSummaryRepository conversationSummaryRepository;

    @Override
    public MemoryContext read(MemoryReadRequest request) {
        // 使用 normalizeRequest（规范化请求）补齐空请求和非法限制值。
        MemoryReadRequest normalizedRequest = normalizeRequest(request);
        // 根据 userId（用户编号）读取用户偏好，并裁剪到上限。
        List<UserPreferenceMemory> userPreferences = readUserPreferences(normalizedRequest);
        // 根据 projectId（项目编号）读取项目记忆。
        ProjectMemorySnapshot projectMemory = readProjectMemory(normalizedRequest).orElse(null);
        // 根据 projectId（项目编号）、platform（平台）或 userId（用户编号）读取历史记忆。
        List<CreationHistoryMemorySnapshot> historyMemories = readHistoryMemories(normalizedRequest);
        // 根据 conversationId（会话编号）读取会话滚动摘要。
        ConversationSummarySnapshot conversationSummary = readConversationSummary(normalizedRequest).orElse(null);
        // 组装读取统计，后续可以写入 Trace（执行链路追踪）。
        MemoryReadStats stats = MemoryReadStats.builder()
                .userPreferenceCount(userPreferences.size())
                .projectMemoryHit(projectMemory != null)
                .historyMemoryCount(historyMemories.size())
                .conversationSummaryHit(conversationSummary != null)
                .userPreferenceLimit(normalizedRequest.getUserPreferenceLimit())
                .historyMemoryLimit(normalizedRequest.getHistoryMemoryLimit())
                .build();
        // 返回统一 MemoryContext（记忆上下文）。
        return MemoryContext.builder()
                .userId(normalizedRequest.getUserId())
                .projectId(normalizedRequest.getProjectId())
                .conversationId(normalizedRequest.getConversationId())
                .platform(normalizedRequest.getPlatform())
                .userPreferences(userPreferences)
                .projectMemory(projectMemory)
                .historyMemories(historyMemories)
                .conversationSummary(conversationSummary)
                .stats(stats)
                .build();
    }

    /**
     * readUserPreferences（读取用户偏好）用于读取并裁剪用户级长期偏好。
     *
     * @param request 记忆读取请求
     * @return 用户偏好列表
     */
    private List<UserPreferenceMemory> readUserPreferences(MemoryReadRequest request) {
        // 如果 userId（用户编号）为空，则无法读取用户偏好。
        if (!StringUtils.hasText(request.getUserId())) {
            // 返回空列表表示未命中。
            return Collections.emptyList();
        }
        // 从仓储读取启用中的用户偏好。
        return userPreferenceMemoryRepository.listActiveByUserId(request.getUserId()).stream()
                // 使用 limit（限制）避免一次注入过多偏好。
                .limit(request.getUserPreferenceLimit())
                // 将数据库实体转换为 MemoryContext（记忆上下文）使用的视图对象。
                .map(this::toUserPreferenceMemory)
                // 收集为不可变语义的列表。
                .toList();
    }

    /**
     * readProjectMemory（读取项目记忆）用于读取当前项目画像。
     *
     * @param request 记忆读取请求
     * @return 可能存在的项目记忆
     */
    private Optional<ProjectMemorySnapshot> readProjectMemory(MemoryReadRequest request) {
        // 如果 projectId（项目编号）为空，则无法读取项目记忆。
        if (!StringUtils.hasText(request.getProjectId())) {
            // 返回 Optional.empty（空结果）。
            return Optional.empty();
        }
        // 从仓储读取启用中的项目记忆并转换为快照对象。
        return projectMemoryRepository.findActiveByProjectId(request.getProjectId())
                .map(this::toProjectMemorySnapshot);
    }

    /**
     * readHistoryMemories（读取历史记忆）用于读取历史高质量方案摘要。
     *
     * @param request 记忆读取请求
     * @return 历史记忆列表
     */
    private List<CreationHistoryMemorySnapshot> readHistoryMemories(MemoryReadRequest request) {
        // 如果 projectId（项目编号）和 platform（平台）都存在，则优先读取同项目同平台的高分历史经验。
        if (StringUtils.hasText(request.getProjectId()) && StringUtils.hasText(request.getPlatform())) {
            // 查询同项目同平台高分历史记忆。
            return creationHistoryMemoryRepository
                    .listHighScoreByProjectAndPlatform(request.getProjectId(), request.getPlatform(), request.getHighScoreThreshold(), request.getHistoryMemoryLimit())
                    .stream()
                    // 将数据库实体转换为历史记忆快照。
                    .map(this::toCreationHistoryMemorySnapshot)
                    // 收集为列表。
                    .toList();
        }
        // 如果没有项目和平台，但有 userId（用户编号），则使用用户近期历史记忆兜底。
        if (StringUtils.hasText(request.getUserId())) {
            // 查询用户近期历史记忆。
            return creationHistoryMemoryRepository
                    .listRecentActiveByUserId(request.getUserId(), request.getHistoryMemoryLimit())
                    .stream()
                    // 将数据库实体转换为历史记忆快照。
                    .map(this::toCreationHistoryMemorySnapshot)
                    // 收集为列表。
                    .toList();
        }
        // 没有足够条件时返回空列表。
        return Collections.emptyList();
    }

    /**
     * readConversationSummary（读取会话摘要）用于读取短期记忆滚动摘要。
     *
     * @param request 记忆读取请求
     * @return 可能存在的会话摘要
     */
    private Optional<ConversationSummarySnapshot> readConversationSummary(MemoryReadRequest request) {
        // 如果 conversationId（会话编号）为空，则无法读取会话摘要。
        if (!StringUtils.hasText(request.getConversationId())) {
            // 返回 Optional.empty（空结果）。
            return Optional.empty();
        }
        // 从仓储读取启用摘要并转换为快照。
        return conversationSummaryRepository.findActiveByConversationId(request.getConversationId())
                .map(this::toConversationSummarySnapshot);
    }

    /**
     * normalizeRequest（规范化请求）用于处理空请求和非法限制值。
     *
     * @param request 原始读取请求
     * @return 规范化后的读取请求
     */
    private MemoryReadRequest normalizeRequest(MemoryReadRequest request) {
        // 如果请求为空，则创建默认请求，避免调用方空指针。
        MemoryReadRequest source = request == null ? MemoryReadRequest.builder().build() : request;
        // 用户偏好上限至少为 1，避免 limit（限制）传入非法值。
        int userPreferenceLimit = Math.max(source.getUserPreferenceLimit(), 1);
        // 历史记忆上限至少为 1，避免 limit（限制）传入非法值。
        int historyMemoryLimit = Math.max(source.getHistoryMemoryLimit(), 1);
        // 高分阈值限制在 0 到 100 之间。
        int highScoreThreshold = Math.max(0, Math.min(source.getHighScoreThreshold(), 100));
        // 返回补齐后的请求对象。
        return MemoryReadRequest.builder()
                .runId(source.getRunId())
                .userId(source.getUserId())
                .projectId(source.getProjectId())
                .conversationId(source.getConversationId())
                .platform(source.getPlatform())
                .userPreferenceLimit(userPreferenceLimit)
                .historyMemoryLimit(historyMemoryLimit)
                .highScoreThreshold(highScoreThreshold)
                .build();
    }

    /**
     * toUserPreferenceMemory（转为用户偏好记忆）用于实体到领域视图的转换。
     *
     * @param entity 用户偏好记忆实体
     * @return 用户偏好记忆
     */
    private UserPreferenceMemory toUserPreferenceMemory(UserPreferenceMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装用户偏好记忆。
        return UserPreferenceMemory.builder()
                .memoryId(entity.getMemoryId())
                .memoryType(entity.getMemoryType())
                .memoryKey(entity.getMemoryKey())
                .memoryValue(entity.getMemoryValue())
                .confidence(entity.getConfidence())
                .sourceType(entity.getSourceType())
                .build();
    }

    /**
     * toProjectMemorySnapshot（转为项目记忆快照）用于实体到领域视图的转换。
     *
     * @param entity 项目记忆实体
     * @return 项目记忆快照
     */
    private ProjectMemorySnapshot toProjectMemorySnapshot(ProjectMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装项目记忆快照。
        return ProjectMemorySnapshot.builder()
                .memoryId(entity.getMemoryId())
                .summary(entity.getSummary())
                .targetPlatform(entity.getTargetPlatform())
                .targetAudience(entity.getTargetAudience())
                .contentStyle(entity.getContentStyle())
                .constraintsJson(entity.getConstraintsJson())
                .build();
    }

    /**
     * toCreationHistoryMemorySnapshot（转为创作历史记忆快照）用于实体到领域视图的转换。
     *
     * @param entity 创作历史记忆实体
     * @return 创作历史记忆快照
     */
    private CreationHistoryMemorySnapshot toCreationHistoryMemorySnapshot(CreationHistoryMemoryEntity entity) {
        // 使用 Builder（建造者模式）组装创作历史记忆快照。
        return CreationHistoryMemorySnapshot.builder()
                .memoryId(entity.getMemoryId())
                .runId(entity.getRunId())
                .theme(entity.getTheme())
                .platform(entity.getPlatform())
                .summary(entity.getSummary())
                .score(entity.getScore())
                .tagsJson(entity.getTagsJson())
                .build();
    }

    /**
     * toConversationSummarySnapshot（转为会话摘要快照）用于实体到领域视图的转换。
     *
     * @param entity 会话滚动摘要实体
     * @return 会话摘要快照
     */
    private ConversationSummarySnapshot toConversationSummarySnapshot(ConversationSummaryEntity entity) {
        // 使用 Builder（建造者模式）组装会话摘要快照。
        return ConversationSummarySnapshot.builder()
                .summaryId(entity.getSummaryId())
                .summaryContent(entity.getSummaryContent())
                .summarizedUntilMessageId(entity.getSummarizedUntilMessageId())
                .summarizedUntilTime(entity.getSummarizedUntilTime())
                .sourceMessageCount(entity.getSourceMessageCount())
                .summaryVersion(entity.getSummaryVersion())
                .tokenEstimate(entity.getTokenEstimate())
                .build();
    }
}
