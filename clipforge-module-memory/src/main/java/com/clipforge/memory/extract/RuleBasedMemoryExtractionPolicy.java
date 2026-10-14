package com.clipforge.memory.extract;

import com.clipforge.memory.infrastructure.persistence.enums.MemorySourceType;
import com.clipforge.memory.infrastructure.persistence.enums.UserPreferenceMemoryType;
import com.clipforge.memory.write.MemoryWriteRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * RuleBasedMemoryExtractionPolicy（基于规则的记忆抽取策略）。
 *
 * <p>第一版只做确定性抽取，避免为了写记忆额外消耗 Token（令牌）成本。
 */
@Component
@Profile("db")
public class RuleBasedMemoryExtractionPolicy implements MemoryExtractionPolicy {

    /** explicitConfidence（显式偏好置信度）表示用户通过字段或明确语句表达的可信度。 */
    private static final BigDecimal EXPLICIT_CONFIDENCE = new BigDecimal("0.9000");

    /** inferredConfidence（推断偏好置信度）表示从普通输入中规则识别出的可信度。 */
    private static final BigDecimal INFERRED_CONFIDENCE = new BigDecimal("0.8000");

    /**
     * extractUserPreferences（抽取用户偏好）用于从用户输入和风格偏好字段中提取长期记忆。
     *
     * @param request 记忆写入请求
     * @return 抽取出的用户偏好记忆列表
     */
    @Override
    public List<ExtractedUserPreferenceMemory> extractUserPreferences(MemoryWriteRequest request) {
        // 创建结果列表，保持规则抽取顺序稳定，方便测试和排查。
        List<ExtractedUserPreferenceMemory> memories = new ArrayList<>();
        // 如果请求为空，则无法抽取任何记忆。
        if (request == null) {
            // 返回空列表。
            return memories;
        }
        // 从结构化风格偏好字段抽取风格记忆。
        addStylePreference(memories, request.getStylePreference());
        // 从目标平台字段抽取平台偏好。
        addPlatformPreference(memories, request.getPlatform());
        // 从用户输入中抽取负向偏好。
        addNegativePreference(memories, request.getUserInput());
        // 从用户输入中抽取受众偏好。
        addAudiencePreference(memories, request.getUserInput());
        // 从最终结果中抽取稳定主题偏好。
        addTopicPreference(memories, request);
        // 返回抽取结果。
        return memories;
    }

    /**
     * addStylePreference（添加风格偏好）用于保存用户显式填写的风格要求。
     *
     * @param memories 记忆列表
     * @param stylePreference 风格偏好
     */
    private void addStylePreference(List<ExtractedUserPreferenceMemory> memories, String stylePreference) {
        // 如果风格偏好为空，则不写入。
        if (!hasText(stylePreference)) {
            // 直接返回。
            return;
        }
        // 添加风格偏好记忆。
        memories.add(buildMemory(UserPreferenceMemoryType.STYLE.value(), "tone", trimToLimit(stylePreference, 120), EXPLICIT_CONFIDENCE, MemorySourceType.USER_EXPLICIT.value()));
    }

    /**
     * addPlatformPreference（添加平台偏好）用于保存用户本次明确选择的平台。
     *
     * @param memories 记忆列表
     * @param platform 目标平台
     */
    private void addPlatformPreference(List<ExtractedUserPreferenceMemory> memories, String platform) {
        // 如果平台为空，则不写入。
        if (!hasText(platform)) {
            // 直接返回。
            return;
        }
        // 添加平台偏好记忆。
        memories.add(buildMemory(UserPreferenceMemoryType.PLATFORM.value(), "recent_platform", platform.trim(), INFERRED_CONFIDENCE, MemorySourceType.SYSTEM_INFERRED.value()));
    }

    /**
     * addNegativePreference（添加负向偏好）用于识别“不要、别、避免”等约束表达。
     *
     * @param memories 记忆列表
     * @param userInput 用户输入
     */
    private void addNegativePreference(List<ExtractedUserPreferenceMemory> memories, String userInput) {
        // 如果用户输入为空，则不抽取。
        if (!hasText(userInput)) {
            // 直接返回。
            return;
        }
        // 判断是否包含常见负向约束词。
        if (userInput.contains("不要") || userInput.contains("别") || userInput.contains("避免")) {
            // 提取包含负向表达的短句作为记忆值。
            String value = trimToLimit(userInput, 140);
            // 添加负向偏好记忆。
            memories.add(buildMemory(UserPreferenceMemoryType.NEGATIVE.value(), "avoid", value, INFERRED_CONFIDENCE, MemorySourceType.USER_EXPLICIT.value()));
        }
    }

    /**
     * addAudiencePreference（添加受众偏好）用于识别小朋友、青少年、学生等受众线索。
     *
     * @param memories 记忆列表
     * @param userInput 用户输入
     */
    private void addAudiencePreference(List<ExtractedUserPreferenceMemory> memories, String userInput) {
        // 如果用户输入为空，则不抽取。
        if (!hasText(userInput)) {
            // 直接返回。
            return;
        }
        // 如果用户提到儿童或青少年受众，则沉淀受众偏好。
        if (userInput.contains("小朋友") || userInput.contains("孩子") || userInput.contains("青少年") || userInput.contains("学生")) {
            // 添加受众偏好记忆。
            memories.add(buildMemory(UserPreferenceMemoryType.AUDIENCE.value(), "target_audience", trimToLimit(userInput, 140), INFERRED_CONFIDENCE, MemorySourceType.SYSTEM_INFERRED.value()));
        }
    }

    /**
     * addTopicPreference（添加主题偏好）用于把最终方案主题沉淀成近期关注方向。
     *
     * @param memories 记忆列表
     * @param request 记忆写入请求
     */
    private void addTopicPreference(List<ExtractedUserPreferenceMemory> memories, MemoryWriteRequest request) {
        // 如果最终结果为空或主题为空，则不写入主题偏好。
        if (request.getFinalResult() == null || !hasText(request.getFinalResult().getTheme())) {
            // 直接返回。
            return;
        }
        // 添加主题偏好记忆。
        memories.add(buildMemory(UserPreferenceMemoryType.TOPIC.value(), "recent_topic", trimToLimit(request.getFinalResult().getTheme(), 120), INFERRED_CONFIDENCE, MemorySourceType.SYSTEM_INFERRED.value()));
    }

    /**
     * buildMemory（构建记忆）用于减少规则分支里的样板代码。
     *
     * @param type 记忆类型
     * @param key 记忆键
     * @param value 记忆值
     * @param confidence 置信度
     * @param sourceType 来源类型
     * @return 抽取出的用户偏好记忆
     */
    private ExtractedUserPreferenceMemory buildMemory(String type, String key, String value, BigDecimal confidence, String sourceType) {
        // 使用 Builder（建造者模式）构建不可缺字段清晰的中间对象。
        return ExtractedUserPreferenceMemory.builder()
                .memoryType(type)
                .memoryKey(key)
                .memoryValue(value)
                .confidence(confidence)
                .sourceType(sourceType)
                .build();
    }

    /**
     * trimToLimit（裁剪到长度上限）用于避免记忆值过长污染后续 Prompt（提示词）。
     *
     * @param value 原始文本
     * @param limit 长度上限
     * @return 裁剪后的文本
     */
    private String trimToLimit(String value, int limit) {
        // 去掉首尾空白。
        String trimmed = value == null ? "" : value.trim();
        // 如果没有超过长度上限，则直接返回。
        if (trimmed.length() <= limit) {
            // 返回原文本。
            return trimmed;
        }
        // 超过长度时裁剪并追加省略号。
        return trimmed.substring(0, limit) + "...";
    }

    /**
     * hasText（是否有文本）用于判断字符串去除空白后是否仍有内容。
     *
     * @param value 原始文本
     * @return 是否有文本
     */
    private boolean hasText(String value) {
        // 判断字符串不为空且去除空白后仍有内容。
        return value != null && !value.isBlank();
    }
}
