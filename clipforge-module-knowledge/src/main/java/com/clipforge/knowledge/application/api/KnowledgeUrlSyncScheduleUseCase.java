package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.KnowledgeUrlSyncExecutionResponse;
import com.clipforge.contract.knowledge.KnowledgeUrlSyncScheduleResponse;
import java.util.List;

/**
 * KnowledgeUrlSyncScheduleUseCase（知识 URL 同步接口用例）定义 URL 同步任务管理和执行入口。
 */
public interface KnowledgeUrlSyncScheduleUseCase {

    KnowledgeUrlSyncScheduleResponse upsertScheduleResponseForDocument(String documentId, String scheduleCron, boolean enabled);

    KnowledgeUrlSyncScheduleResponse queryScheduleByDocumentId(String documentId);

    List<KnowledgeUrlSyncScheduleResponse> listSchedulesByKnowledgeBaseId(String knowledgeBaseId);

    KnowledgeUrlSyncExecutionResponse executeSingleScheduleResponse(String scheduleId);

    List<KnowledgeUrlSyncExecutionResponse> listExecutions(String scheduleId, Integer limit);
}
