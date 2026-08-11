package com.clipforge.model.governance;

/**
 * ModelCallLimiter（模型调用限流器）负责在真正请求模型前控制并发。
 */
public interface ModelCallLimiter {

    /**
     * execute（执行）在拿到调用许可后运行真实模型请求，并在结束后释放许可。
     *
     * @param callType 模型调用类型
     * @param bizId 业务编号，通常使用 runId（运行编号）、materialId（素材编号）或 documentId（文档编号）
     * @param supplier 真正的模型调用
     * @return 模型调用结果
     * @param <T> 返回类型
     */
    <T> T execute(ModelCallType callType, String bizId, ModelCallSupplier<T> supplier);
}
