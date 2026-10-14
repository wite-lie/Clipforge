package com.clipforge.memory.read;

import com.clipforge.memory.model.MemoryContext;

/**
 * MemoryReader（记忆读取器）。
 *
 * <p>该接口定义从长期记忆、项目记忆、历史记忆和会话滚动摘要中组装 MemoryContext（记忆上下文）的能力。
 */
public interface MemoryReader {

    /**
     * read（读取）根据请求条件组装记忆上下文。
     *
     * @param request 记忆读取请求
     * @return 记忆上下文
     */
    MemoryContext read(MemoryReadRequest request);
}
