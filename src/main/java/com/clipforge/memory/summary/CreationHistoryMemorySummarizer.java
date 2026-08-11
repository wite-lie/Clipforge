package com.clipforge.memory.summary;

import com.clipforge.memory.write.MemoryWriteRequest;

/**
 * CreationHistoryMemorySummarizer（创作历史记忆摘要器）。
 *
 * <p>该接口用于把高分创作结果压缩成短摘要，后续可以替换为 LLM（大语言模型）摘要器。
 */
public interface CreationHistoryMemorySummarizer {

    /**
     * summarize（摘要）用于生成历史记忆摘要和标签。
     *
     * @param request 记忆写入请求
     * @return 创作历史记忆摘要
     */
    CreationHistoryMemorySummary summarize(MemoryWriteRequest request);
}
