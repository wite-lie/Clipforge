package com.clipforge.memory.write;

/**
 * MemoryWriter（记忆写入器）。
 *
 * <p>该接口统一封装创作成功后的 Memory（记忆）沉淀逻辑。
 */
public interface MemoryWriter {

    /**
     * write（写入）用于根据一次成功创作沉淀长期记忆、项目记忆和历史记忆。
     *
     * @param request 记忆写入请求
     * @return 记忆写入结果
     */
    MemoryWriteResult write(MemoryWriteRequest request);
}
