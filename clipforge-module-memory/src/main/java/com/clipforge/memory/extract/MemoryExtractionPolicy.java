package com.clipforge.memory.extract;

import com.clipforge.memory.write.MemoryWriteRequest;
import java.util.List;

/**
 * MemoryExtractionPolicy（记忆抽取策略）。
 *
 * <p>该接口用于从一次创作请求中抽取用户偏好记忆，后续可以替换为 LLM（大语言模型）抽取实现。
 */
public interface MemoryExtractionPolicy {

    /**
     * extractUserPreferences（抽取用户偏好）用于识别可长期复用的用户偏好。
     *
     * @param request 记忆写入请求
     * @return 抽取出的用户偏好记忆列表
     */
    List<ExtractedUserPreferenceMemory> extractUserPreferences(MemoryWriteRequest request);
}
