package com.clipforge.memory.write;

import com.clipforge.agent.CreationPlanResult;
import com.clipforge.infra.persistence.entity.CreationHistoryMemoryEntity;
import com.clipforge.infra.persistence.entity.ProjectMemoryEntity;
import com.clipforge.infra.persistence.entity.UserPreferenceMemoryEntity;
import com.clipforge.infra.persistence.enums.MemoryStatus;
import com.clipforge.infra.persistence.repository.CreationHistoryMemoryRepository;
import com.clipforge.infra.persistence.repository.ProjectMemoryRepository;
import com.clipforge.infra.persistence.repository.UserPreferenceMemoryRepository;
import com.clipforge.memory.extract.ExtractedUserPreferenceMemory;
import com.clipforge.memory.extract.MemoryExtractionModelClient;
import com.clipforge.memory.extract.MemoryExtractionModelRequest;
import com.clipforge.memory.extract.MemoryExtractionModelResponse;
import com.clipforge.memory.extract.MemoryExtractionPolicy;
import com.clipforge.memory.extract.MemoryExtractionTriggerDecision;
import com.clipforge.memory.extract.MemoryExtractionTriggerPolicy;
import com.clipforge.memory.summary.CreationHistoryMemorySummarizer;
import com.clipforge.memory.summary.CreationHistoryMemorySummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DefaultMemoryWriter（默认记忆写入器）。
 *
 * <p>该实现负责在创作成功后沉淀用户偏好、项目画像和高分历史经验；当场景足够重要时，还会额外调用 LLM 补充抽取。
 */
@Service
@Profile("db")
public class DefaultMemoryWriter implements MemoryWriter {

    private final UserPreferenceMemoryRepository userPreferenceMemoryRepository;
    private final ProjectMemoryRepository projectMemoryRepository;
    private final CreationHistoryMemoryRepository creationHistoryMemoryRepository;
    private final MemoryExtractionPolicy memoryExtractionPolicy;
    private final MemoryExtractionTriggerPolicy memoryExtractionTriggerPolicy;
    private final ObjectProvider<MemoryExtractionModelClient> memoryExtractionModelClientProvider;
    private final CreationHistoryMemorySummarizer creationHistoryMemorySummarizer;
    private final ObjectMapper objectMapper;

    @Value("${clipforge.memory.high-score-threshold:85}")
    private int highScoreThreshold;

    public DefaultMemoryWriter(UserPreferenceMemoryRepository userPreferenceMemoryRepository,
                               ProjectMemoryRepository projectMemoryRepository,
                               CreationHistoryMemoryRepository creationHistoryMemoryRepository,
                               MemoryExtractionPolicy memoryExtractionPolicy,
                               CreationHistoryMemorySummarizer creationHistoryMemorySummarizer,
                               ObjectMapper objectMapper) {
        this(userPreferenceMemoryRepository, projectMemoryRepository, creationHistoryMemoryRepository, memoryExtractionPolicy, null, null, creationHistoryMemorySummarizer, objectMapper);
    }

    @Autowired
    public DefaultMemoryWriter(UserPreferenceMemoryRepository userPreferenceMemoryRepository,
                               ProjectMemoryRepository projectMemoryRepository,
                               CreationHistoryMemoryRepository creationHistoryMemoryRepository,
                               MemoryExtractionPolicy memoryExtractionPolicy,
                               MemoryExtractionTriggerPolicy memoryExtractionTriggerPolicy,
                               ObjectProvider<MemoryExtractionModelClient> memoryExtractionModelClientProvider,
                               CreationHistoryMemorySummarizer creationHistoryMemorySummarizer,
                               ObjectMapper objectMapper) {
        this.userPreferenceMemoryRepository = userPreferenceMemoryRepository;
        this.projectMemoryRepository = projectMemoryRepository;
        this.creationHistoryMemoryRepository = creationHistoryMemoryRepository;
        this.memoryExtractionPolicy = memoryExtractionPolicy;
        this.memoryExtractionTriggerPolicy = memoryExtractionTriggerPolicy;
        this.memoryExtractionModelClientProvider = memoryExtractionModelClientProvider;
        this.creationHistoryMemorySummarizer = creationHistoryMemorySummarizer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public MemoryWriteResult write(MemoryWriteRequest request) {
        if (request == null || !hasText(request.getUserId())) {
            return MemoryWriteResult.skipped("缺少 userId（用户编号），跳过记忆写入");
        }

        List<String> messages = new ArrayList<>();
        List<ExtractedUserPreferenceMemory> ruleMemories = extractRuleUserPreferences(request);
        int rulePreferenceCount = writeRuleUserPreferences(request, ruleMemories, messages);
        int llmPreferenceCount = writeLlmUserPreferences(request, ruleMemories, messages);
        boolean projectMemoryInitialized = initializeProjectMemoryIfNeeded(request, messages);
        boolean historyMemoryWritten = writeHistoryMemoryIfHighScore(request, messages);

        return MemoryWriteResult.builder()
                .userPreferenceWrittenCount(rulePreferenceCount + llmPreferenceCount)
                .rulePreferenceWrittenCount(rulePreferenceCount)
                .llmPreferenceWrittenCount(llmPreferenceCount)
                .projectMemoryInitialized(projectMemoryInitialized)
                .historyMemoryWritten(historyMemoryWritten)
                .messages(messages)
                .build();
    }

    private List<ExtractedUserPreferenceMemory> extractRuleUserPreferences(MemoryWriteRequest request) {
        return memoryExtractionPolicy == null
                ? List.of()
                : memoryExtractionPolicy.extractUserPreferences(request);
    }

    private int writeRuleUserPreferences(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> ruleMemories, List<String> messages) {
        int writtenCount = persistUserPreferences(request, ruleMemories, messages, "规则抽取");
        messages.add("规则抽取完成，新增 " + writtenCount + " 条");
        return writtenCount;
    }

    private int writeLlmUserPreferences(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> ruleMemories, List<String> messages) {
        MemoryExtractionTriggerDecision decision = decideLlmExtraction(request, ruleMemories);
        if (decision == null || !decision.isShouldInvokeLlm()) {
            messages.add("LLM 记忆抽取已跳过：" + (decision == null ? "无触发决策" : decision.getReason()));
            return 0;
        }
        MemoryExtractionModelClient client = memoryExtractionModelClientProvider == null ? null : memoryExtractionModelClientProvider.getIfAvailable();
        if (client == null) {
            messages.add("LLM 记忆抽取已跳过：未找到可用的模型客户端");
            return 0;
        }

        try {
            MemoryExtractionModelResponse response = client.extract(MemoryExtractionModelRequest.builder()
                    .runId(request.getRunId())
                    .userId(request.getUserId())
                    .projectId(request.getProjectId())
                    .conversationId(request.getConversationId())
                    .platform(request.getPlatform())
                    .userInput(request.getUserInput())
                    .stylePreference(request.getStylePreference())
                    .finalResult(request.getFinalResult())
                    .ruleMemories(ruleMemories)
                    .triggerType(request.getTriggerType())
                    .triggerReason(request.getTriggerReason() == null ? decision.getReason() : request.getTriggerReason())
                    .build());
            int writtenCount = persistUserPreferences(request, response == null ? List.of() : response.getMemories(), messages, "LLM 抽取");
            messages.add("LLM 记忆抽取完成，新增 " + writtenCount + " 条");
            return writtenCount;
        } catch (RuntimeException exception) {
            messages.add("LLM 记忆抽取失败，回退到规则结果：" + exception.getMessage());
            return 0;
        }
    }

    private MemoryExtractionTriggerDecision decideLlmExtraction(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> ruleMemories) {
        if (memoryExtractionTriggerPolicy == null) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("未配置 LLM 记忆抽取触发策略")
                    .build();
        }
        if (request == null) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("记忆写入请求为空")
                    .build();
        }
        if ((ruleMemories == null || ruleMemories.isEmpty()) && !hasText(request.getUserInput()) && !hasText(request.getStylePreference()) && request.getFinalResult() == null) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("没有足够的抽取上下文")
                    .build();
        }
        return memoryExtractionTriggerPolicy.decide(request, ruleMemories == null ? List.of() : ruleMemories);
    }

    private int persistUserPreferences(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> memories, List<String> messages, String sourceLabel) {
        if (memories == null || memories.isEmpty()) {
            return 0;
        }
        int writtenCount = 0;
        for (ExtractedUserPreferenceMemory memory : memories) {
            if (!isValid(memory)) {
                messages.add("跳过空偏好记忆");
                continue;
            }
            if (alreadyExists(request.getUserId(), memory)) {
                messages.add("跳过重复用户偏好：" + memory.getMemoryType() + "/" + memory.getMemoryKey());
                continue;
            }
            userPreferenceMemoryRepository.save(buildUserPreferenceEntity(request, memory));
            writtenCount++;
            messages.add(sourceLabel + "写入：" + memory.getMemoryType() + "/" + memory.getMemoryKey());
        }
        return writtenCount;
    }

    private boolean initializeProjectMemoryIfNeeded(MemoryWriteRequest request, List<String> messages) {
        if (!hasText(request.getProjectId())) {
            messages.add("缺少 projectId（项目编号），跳过项目记忆初始化");
            return false;
        }
        if (projectMemoryRepository.findActiveByProjectId(request.getProjectId()).isPresent()) {
            messages.add("项目记忆已存在，跳过初始化");
            return false;
        }
        projectMemoryRepository.save(buildProjectMemoryEntity(request));
        messages.add("初始化项目记忆：" + request.getProjectId());
        return true;
    }

    private boolean writeHistoryMemoryIfHighScore(MemoryWriteRequest request, List<String> messages) {
        if (request.getFinalResult() == null) {
            messages.add("缺少最终创作结果，跳过历史记忆写入");
            return false;
        }
        if (request.getFinalResult().getScore() < highScoreThreshold) {
            messages.add("评分低于高分阈值 " + highScoreThreshold + "，跳过历史记忆写入");
            return false;
        }
        CreationHistoryMemorySummary summary = creationHistoryMemorySummarizer.summarize(request);
        creationHistoryMemoryRepository.save(buildHistoryMemoryEntity(request, summary));
        messages.add("写入高分历史记忆：" + request.getFinalResult().getScore());
        return true;
    }

    private UserPreferenceMemoryEntity buildUserPreferenceEntity(MemoryWriteRequest request, ExtractedUserPreferenceMemory memory) {
        LocalDateTime now = LocalDateTime.now();
        return UserPreferenceMemoryEntity.builder()
                .id(prefixedId("memory_pk_"))
                .memoryId(prefixedId("memory_user_"))
                .userId(request.getUserId())
                .memoryType(memory.getMemoryType())
                .memoryKey(memory.getMemoryKey())
                .memoryValue(memory.getMemoryValue())
                .confidence(memory.getConfidence())
                .sourceType(memory.getSourceType())
                .sourceRunId(request.getRunId())
                .status(MemoryStatus.ACTIVE.value())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private ProjectMemoryEntity buildProjectMemoryEntity(MemoryWriteRequest request) {
        CreationPlanResult result = request.getFinalResult();
        LocalDateTime now = LocalDateTime.now();
        String summary = "围绕“" + firstText(result == null ? null : result.getTheme(), request.getUserInput())
                + "”进行短视频创作规划，目标平台为 " + firstText(request.getPlatform(), "未提供")
                + "，目标受众为 " + firstText(result == null ? null : result.getTargetAudience(), "未提供")
                + "，内容风格参考用户偏好：" + firstText(request.getStylePreference(), "未提供") + "。";
        return ProjectMemoryEntity.builder()
                .id(prefixedId("memory_pk_"))
                .memoryId(prefixedId("memory_project_"))
                .projectId(request.getProjectId())
                .userId(request.getUserId())
                .summary(summary)
                .targetPlatform(request.getPlatform())
                .targetAudience(result == null ? null : result.getTargetAudience())
                .contentStyle(request.getStylePreference())
                .constraintsJson(toJson(List.of()))
                .status(MemoryStatus.ACTIVE.value())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private CreationHistoryMemoryEntity buildHistoryMemoryEntity(MemoryWriteRequest request, CreationHistoryMemorySummary summary) {
        CreationPlanResult result = request.getFinalResult();
        LocalDateTime now = LocalDateTime.now();
        return CreationHistoryMemoryEntity.builder()
                .id(prefixedId("memory_pk_"))
                .memoryId(prefixedId("memory_history_"))
                .userId(request.getUserId())
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .runId(request.getRunId())
                .theme(result.getTheme())
                .platform(request.getPlatform())
                .summary(summary == null ? "" : summary.getSummary())
                .score(result.getScore())
                .tagsJson(toJson(summary == null ? List.of() : summary.getTags()))
                .status(MemoryStatus.ACTIVE.value())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private boolean alreadyExists(String userId, ExtractedUserPreferenceMemory memory) {
        return userPreferenceMemoryRepository.listActiveByUserIdAndKey(userId, memory.getMemoryKey()).stream()
                .anyMatch(entity -> memory.getMemoryValue().equals(entity.getMemoryValue()));
    }

    private boolean isValid(ExtractedUserPreferenceMemory memory) {
        return memory != null && hasText(memory.getMemoryType()) && hasText(memory.getMemoryKey()) && hasText(memory.getMemoryValue());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private String prefixedId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String firstText(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
