package com.clipforge.memory.extract;

import java.util.List;
import org.springframework.stereotype.Component;
import com.clipforge.memory.write.MemoryWriteRequest;

/**
 * DefaultMemoryExtractionTriggerPolicy（默认记忆抽取触发策略）。
 */
@Component
public class DefaultMemoryExtractionTriggerPolicy implements MemoryExtractionTriggerPolicy {

    private final MemoryExtractionProperties properties;

    public DefaultMemoryExtractionTriggerPolicy(MemoryExtractionProperties properties) {
        this.properties = properties;
    }

    @Override
    public MemoryExtractionTriggerDecision decide(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> ruleMemories) {
        if (properties == null || !properties.isEnabled() || !properties.isLlmEnabled()) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("LLM 记忆抽取未启用")
                    .build();
        }
        MemoryExtractionTriggerType triggerType = request == null || request.getTriggerType() == null
                ? MemoryExtractionTriggerType.CREATION_SUCCESS
                : request.getTriggerType();
        if (properties.getLlmTriggerTypes() == null || !properties.getLlmTriggerTypes().contains(triggerType)) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("当前触发类型不在 LLM 抽取白名单中：" + triggerType)
                    .build();
        }
        if (!hasText(request == null ? null : request.getUserInput())
                && !hasText(request == null ? null : request.getStylePreference())
                && (request == null || request.getFinalResult() == null)) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("缺少可用于抽取的输入内容")
                    .build();
        }
        if (ruleMemories != null && properties.getMaxCandidateCount() > 0 && ruleMemories.size() >= properties.getMaxCandidateCount()) {
            return MemoryExtractionTriggerDecision.builder()
                    .shouldInvokeLlm(false)
                    .reason("规则抽取结果已足够丰富，跳过 LLM 补充")
                    .build();
        }
        return MemoryExtractionTriggerDecision.builder()
                .shouldInvokeLlm(true)
                .reason("满足高价值场景与内容条件")
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
