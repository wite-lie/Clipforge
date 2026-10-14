package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.AsyncProcessKnowledgeDocumentResponse;
import com.clipforge.contract.knowledge.KnowledgeDocumentRecoveryResponse;
import com.clipforge.contract.knowledge.RecoverStuckKnowledgeDocumentResponse;

/**
 * KnowledgeDocumentRecoveryUseCase（知识文档恢复接口用例）定义恢复建议、人工重试和卡住任务恢复入口。
 *
 * <p>该接口属于 Knowledge 模块应用边界，对外只接收字符串阶段值，不暴露 RocketMQ（消息队列）消息枚举。
 */
public interface KnowledgeDocumentRecoveryUseCase {

    KnowledgeDocumentRecoveryResponse queryRecovery(String documentId);

    AsyncProcessKnowledgeDocumentResponse retryDocumentProcess(String documentId, String requestedStage);

    RecoverStuckKnowledgeDocumentResponse recoverStuckTasks(Long timeoutMinutes);
}
