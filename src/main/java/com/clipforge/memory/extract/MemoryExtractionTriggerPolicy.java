package com.clipforge.memory.extract;

import java.util.List;
import com.clipforge.memory.write.MemoryWriteRequest;

/**
 * MemoryExtractionTriggerPolicy（记忆抽取触发策略）。
 *
 * <p>该接口用于判断一次记忆写入是否值得额外调用 LLM（大语言模型）做补充抽取。
 */
public interface MemoryExtractionTriggerPolicy {

    /**
     * decide（决策）用于判断当前写入是否应该启用 LLM 抽取。
     *
     * @param request 记忆写入请求
     * @param ruleMemories 规则抽取结果
     * @return 触发决策
     */
    MemoryExtractionTriggerDecision decide(MemoryWriteRequest request, List<ExtractedUserPreferenceMemory> ruleMemories);
}
