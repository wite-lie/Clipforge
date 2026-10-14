package com.clipforge.knowledge.application.processing;

/**
 * KnowledgeDocumentProcessUseCase（知识文档处理用例）。
 *
 * <p>该端口定义异步消费者、定时同步和恢复流程执行已有文档处理日志的最小契约。
 * 它不暴露 API DTO，也不依赖消息队列、Redis 或 legacy 模块。
 */
public interface KnowledgeDocumentProcessUseCase {

    /**
     * processExistingLog（处理已有日志）用于按照指定重试阶段执行知识文档处理。
     *
     * @param documentId 知识文档编号
     * @param processLogId 已创建的处理日志编号
     * @param retryStage 重试阶段文本；为空时执行完整处理
     */
    void processExistingLog(String documentId, String processLogId, String retryStage);
}
