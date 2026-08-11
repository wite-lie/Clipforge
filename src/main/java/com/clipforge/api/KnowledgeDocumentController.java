package com.clipforge.api;

import com.clipforge.api.dto.AsyncProcessKnowledgeDocumentResponse;
import com.clipforge.api.dto.CreateKnowledgeChunkRequest;
import com.clipforge.api.dto.CreateKnowledgeUrlDocumentRequest;
import com.clipforge.api.dto.FullProcessKnowledgeDocumentResponse;
import com.clipforge.api.dto.KnowledgeChunkResponse;
import com.clipforge.api.dto.KnowledgeDocumentRecoveryResponse;
import com.clipforge.api.dto.KnowledgeDocumentProcessLogResponse;
import com.clipforge.api.dto.KnowledgeDocumentProcessStatusResponse;
import com.clipforge.api.dto.KnowledgeDocumentResponse;
import com.clipforge.api.dto.KnowledgeUrlSyncExecutionResponse;
import com.clipforge.api.dto.KnowledgeUrlSyncScheduleResponse;
import com.clipforge.api.dto.ProcessKnowledgeDocumentResponse;
import com.clipforge.api.dto.RecoverStuckKnowledgeDocumentResponse;
import com.clipforge.api.dto.RetryKnowledgeDocumentProcessRequest;
import com.clipforge.api.dto.UpdateKnowledgeChunkRequest;
import com.clipforge.api.dto.UpsertKnowledgeUrlSyncScheduleRequest;
import com.clipforge.application.KnowledgeDocumentApplicationService;
import com.clipforge.application.KnowledgeDocumentAsyncProcessApplicationService;
import com.clipforge.application.KnowledgeDocumentFullProcessApplicationService;
import com.clipforge.application.KnowledgeDocumentManagementApplicationService;
import com.clipforge.application.KnowledgeDocumentProcessApplicationService;
import com.clipforge.application.KnowledgeDocumentRecoveryApplicationService;
import com.clipforge.application.KnowledgeDocumentProcessStatusApplicationService;
import com.clipforge.application.KnowledgeDocumentScheduleApplicationService;
import com.clipforge.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * KnowledgeDocumentController（知识文档控制器）。
 *
 * <p>该控制器负责知识文档上传、处理、查询以及文档与切片的管理接口。
 */
@RestController
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    /** knowledgeDocumentApplicationService（知识文档应用服务）负责知识文档接口的应用编排。 */
    private final KnowledgeDocumentApplicationService knowledgeDocumentApplicationService;

    /** knowledgeDocumentProcessApplicationService（知识文档处理应用服务）负责文档解析和切片编排。 */
    private final KnowledgeDocumentProcessApplicationService knowledgeDocumentProcessApplicationService;

    /** knowledgeDocumentFullProcessApplicationService（知识文档完整处理应用服务）负责解析、切片、向量化和处理日志编排。 */
    private final KnowledgeDocumentFullProcessApplicationService knowledgeDocumentFullProcessApplicationService;

    /** knowledgeDocumentAsyncProcessApplicationService（知识文档异步处理应用服务）负责提交 RocketMQ（消息队列）异步处理任务。 */
    private final KnowledgeDocumentAsyncProcessApplicationService knowledgeDocumentAsyncProcessApplicationService;

    /** knowledgeDocumentProcessStatusApplicationService（知识文档处理状态应用服务）负责读取 Redis（缓存）和数据库中的处理状态。 */
    private final KnowledgeDocumentProcessStatusApplicationService knowledgeDocumentProcessStatusApplicationService;

    /** knowledgeDocumentRecoveryApplicationService（知识文档恢复应用服务）负责恢复建议、卡住任务扫描和人工重试。 */
    private final KnowledgeDocumentRecoveryApplicationService knowledgeDocumentRecoveryApplicationService;

    /** knowledgeDocumentManagementApplicationService（知识文档管理应用服务）负责处理日志、切片和文档的管理操作。 */
    private final KnowledgeDocumentManagementApplicationService knowledgeDocumentManagementApplicationService;

    /** knowledgeDocumentScheduleApplicationService（知识文档定时同步应用服务）负责 URL 来源文档的同步任务。 */
    private final KnowledgeDocumentScheduleApplicationService knowledgeDocumentScheduleApplicationService;

    /**
     * uploadDocument（上传知识文档）接口用于把 Markdown（标记文档）、TXT（纯文本）、PDF（便携式文档格式）或 Word（文字处理文档）文件保存到指定知识库。
     *
     * @param knowledgeBaseId 知识库编号
     * @param file 知识文件
     * @param userId 用户编号
     * @return 知识文档响应
     */
    @PostMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocument(@PathVariable String knowledgeBaseId,
                                                                 @RequestParam("file") MultipartFile file,
                                                                 @RequestParam(required = false) String userId) {
        KnowledgeDocumentResponse response = knowledgeDocumentApplicationService.uploadDocument(knowledgeBaseId, file, userId);
        return ApiResponse.ok(response);
    }

    /**
     * createUrlDocument（创建 URL 来源知识文档）接口用于登记远程知识文档地址，后续由 URL 同步任务抓取和处理。
     *
     * @param knowledgeBaseId 知识库编号
     * @param request 创建请求
     * @return 知识文档响应
     */
    @PostMapping("/api/knowledge-bases/{knowledgeBaseId}/url-documents")
    public ApiResponse<KnowledgeDocumentResponse> createUrlDocument(@PathVariable String knowledgeBaseId,
                                                                     @RequestBody CreateKnowledgeUrlDocumentRequest request) {
        KnowledgeDocumentResponse response = knowledgeDocumentApplicationService.createUrlDocument(
                knowledgeBaseId,
                request == null ? null : request.getSourceUrl(),
                request == null ? null : request.getUserId());
        if (request != null && request.getScheduleCron() != null && !request.getScheduleCron().isBlank()) {
            boolean enabled = request.getScheduleEnabled() == null || request.getScheduleEnabled();
            knowledgeDocumentScheduleApplicationService.upsertScheduleForDocument(response.getDocumentId(), request.getScheduleCron(), enabled);
        }
        return ApiResponse.ok(response);
    }

    /**
     * listDocuments（查询知识库文档列表）接口用于读取指定知识库下的全部知识文档。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识文档响应列表
     */
    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<List<KnowledgeDocumentResponse>> listDocuments(@PathVariable String knowledgeBaseId) {
        List<KnowledgeDocumentResponse> responses = knowledgeDocumentApplicationService.listDocuments(knowledgeBaseId);
        return ApiResponse.ok(responses);
    }

    /**
     * listUrlSyncSchedules（查询 URL 同步任务列表）接口用于读取某个知识库下的 URL 来源同步任务。
     *
     * @param knowledgeBaseId 知识库编号
     * @return URL 同步任务列表
     */
    @GetMapping("/api/knowledge-bases/{knowledgeBaseId}/url-sync-schedules")
    public ApiResponse<List<KnowledgeUrlSyncScheduleResponse>> listUrlSyncSchedules(@PathVariable String knowledgeBaseId) {
        List<KnowledgeUrlSyncScheduleResponse> responses = knowledgeDocumentScheduleApplicationService.listSchedulesByKnowledgeBaseId(knowledgeBaseId);
        return ApiResponse.ok(responses);
    }

    /**
     * queryDocument（查询知识文档）接口用于按文档编号读取文档详情。
     *
     * @param documentId 知识文档编号
     * @return 知识文档响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}")
    public ApiResponse<KnowledgeDocumentResponse> queryDocument(@PathVariable String documentId) {
        KnowledgeDocumentResponse response = knowledgeDocumentApplicationService.queryDocument(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * upsertUrlSyncSchedule（保存 URL 同步任务）接口用于为 URL 来源知识文档创建或更新定时同步配置。
     *
     * @param documentId 知识文档编号
     * @param request 保存请求
     * @return URL 同步任务响应
     */
    @PutMapping("/api/knowledge-documents/{documentId}/url-sync-schedule")
    public ApiResponse<KnowledgeUrlSyncScheduleResponse> upsertUrlSyncSchedule(@PathVariable String documentId,
                                                                                @RequestBody UpsertKnowledgeUrlSyncScheduleRequest request) {
        String scheduleCron = request == null ? null : request.getScheduleCron();
        boolean enabled = request == null || request.getEnabled() == null || request.getEnabled();
        KnowledgeUrlSyncScheduleResponse response = knowledgeDocumentScheduleApplicationService.upsertScheduleResponseForDocument(documentId, scheduleCron, enabled);
        return ApiResponse.ok(response);
    }

    /**
     * queryUrlSyncSchedule（查询 URL 同步任务）接口用于按文档编号读取 URL 同步配置。
     *
     * @param documentId 知识文档编号
     * @return URL 同步任务响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}/url-sync-schedule")
    public ApiResponse<KnowledgeUrlSyncScheduleResponse> queryUrlSyncSchedule(@PathVariable String documentId) {
        KnowledgeUrlSyncScheduleResponse response = knowledgeDocumentScheduleApplicationService.queryScheduleByDocumentId(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * syncUrlSchedule（手动同步 URL 任务）接口用于立即执行一次 URL 变更检测和必要的文档重处理。
     *
     * @param scheduleId URL 同步任务编号
     * @return URL 同步执行响应
     */
    @PostMapping("/api/knowledge-url-sync-schedules/{scheduleId}/sync")
    public ApiResponse<KnowledgeUrlSyncExecutionResponse> syncUrlSchedule(@PathVariable String scheduleId) {
        KnowledgeUrlSyncExecutionResponse response = knowledgeDocumentScheduleApplicationService.executeSingleScheduleResponse(scheduleId);
        return ApiResponse.ok(response);
    }

    /**
     * listUrlScheduleExecutions（查询 URL 同步历史）接口用于读取某个同步任务的执行记录。
     *
     * @param scheduleId URL 同步任务编号
     * @param limit 返回数量上限
     * @return URL 同步执行历史
     */
    @GetMapping("/api/knowledge-url-sync-schedules/{scheduleId}/executions")
    public ApiResponse<List<KnowledgeUrlSyncExecutionResponse>> listUrlScheduleExecutions(@PathVariable String scheduleId,
                                                                                           @RequestParam(required = false) Integer limit) {
        List<KnowledgeUrlSyncExecutionResponse> responses = knowledgeDocumentScheduleApplicationService.listExecutions(scheduleId, limit);
        return ApiResponse.ok(responses);
    }

    /**
     * processDocument（处理知识文档）接口用于同步解析文件并生成知识切片。
     *
     * @param documentId 知识文档编号
     * @return 文档处理响应
     */
    @PostMapping("/api/knowledge-documents/{documentId}/process")
    public ApiResponse<ProcessKnowledgeDocumentResponse> processDocument(@PathVariable String documentId) {
        ProcessKnowledgeDocumentResponse response = knowledgeDocumentProcessApplicationService.processDocument(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * fullProcessDocument（完整处理知识文档）接口用于同步执行解析、切片、Embedding（嵌入）和 pgvector（PostgreSQL 向量扩展）入库。
     *
     * @param documentId 知识文档编号
     * @return 完整处理响应
     */
    @PostMapping("/api/knowledge-documents/{documentId}/process/full")
    public ApiResponse<FullProcessKnowledgeDocumentResponse> fullProcessDocument(@PathVariable String documentId) {
        FullProcessKnowledgeDocumentResponse response = knowledgeDocumentFullProcessApplicationService.fullProcessDocument(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * asyncProcessDocument（异步处理知识文档）接口用于提交 RocketMQ（消息队列）异步处理任务。
     *
     * @param documentId 知识文档编号
     * @return 异步处理响应
     */
    @PostMapping("/api/knowledge-documents/{documentId}/process/async")
    public ApiResponse<AsyncProcessKnowledgeDocumentResponse> asyncProcessDocument(@PathVariable String documentId) {
        AsyncProcessKnowledgeDocumentResponse response = knowledgeDocumentAsyncProcessApplicationService.startAsyncProcess(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * queryProcessStatusByDocumentId（按文档编号查询处理状态）接口用于读取某个知识文档最近一次处理状态。
     *
     * @param documentId 知识文档编号
     * @return 处理状态响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}/process/status")
    public ApiResponse<KnowledgeDocumentProcessStatusResponse> queryProcessStatusByDocumentId(@PathVariable String documentId) {
        KnowledgeDocumentProcessStatusResponse response = knowledgeDocumentProcessStatusApplicationService.queryByDocumentId(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * queryProcessStatusByProcessLogId（按处理日志编号查询处理状态）接口用于读取某次处理任务的状态。
     *
     * @param processLogId 处理日志编号
     * @return 处理状态响应
     */
    @GetMapping("/api/knowledge-document-process-logs/{processLogId}/status")
    public ApiResponse<KnowledgeDocumentProcessStatusResponse> queryProcessStatusByProcessLogId(@PathVariable String processLogId) {
        KnowledgeDocumentProcessStatusResponse response = knowledgeDocumentProcessStatusApplicationService.queryByProcessLogId(processLogId);
        return ApiResponse.ok(response);
    }

    /**
     * queryRecovery（查询恢复建议）接口用于读取知识文档最近处理状态、是否可重试和建议重试阶段。
     *
     * @param documentId 知识文档编号
     * @return 恢复建议响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}/process/recovery")
    public ApiResponse<KnowledgeDocumentRecoveryResponse> queryRecovery(@PathVariable String documentId) {
        KnowledgeDocumentRecoveryResponse response = knowledgeDocumentRecoveryApplicationService.queryRecovery(documentId);
        return ApiResponse.ok(response);
    }

    /**
     * retryProcess（重试处理）接口用于人工触发知识文档处理恢复任务。
     *
     * @param documentId 知识文档编号
     * @param request 重试请求
     * @return 异步处理响应
     */
    @PostMapping("/api/knowledge-documents/{documentId}/process/retry")
    public ApiResponse<AsyncProcessKnowledgeDocumentResponse> retryProcess(@PathVariable String documentId,
                                                                           @RequestBody(required = false) RetryKnowledgeDocumentProcessRequest request) {
        AsyncProcessKnowledgeDocumentResponse response = knowledgeDocumentRecoveryApplicationService.retryDocumentProcess(documentId, request == null ? null : request.getRetryStage());
        return ApiResponse.ok(response);
    }

    /**
     * recoverStuckProcesses（恢复卡住处理任务）接口用于扫描长时间运行中的知识文档处理日志并标记失败。
     *
     * @param timeoutMinutes 超时分钟数，为空时使用后端默认配置
     * @return 卡住任务恢复响应
     */
    @PostMapping("/api/knowledge-document-process-logs/recover-stuck")
    public ApiResponse<RecoverStuckKnowledgeDocumentResponse> recoverStuckProcesses(@RequestParam(required = false) Long timeoutMinutes) {
        RecoverStuckKnowledgeDocumentResponse response = knowledgeDocumentRecoveryApplicationService.recoverStuckTasks(timeoutMinutes);
        return ApiResponse.ok(response);
    }

    /**
     * listChunks（查询知识切片列表）接口用于读取指定文档下的全部知识切片。
     *
     * @param documentId 知识文档编号
     * @return 知识切片响应列表
     */
    @GetMapping("/api/knowledge-documents/{documentId}/chunks")
    public ApiResponse<List<KnowledgeChunkResponse>> listChunks(@PathVariable String documentId) {
        List<KnowledgeChunkResponse> responses = knowledgeDocumentProcessApplicationService.listChunks(documentId);
        return ApiResponse.ok(responses);
    }

    /**
     * listProcessLogs（查询处理日志）接口用于读取指定文档的全部处理历史。
     *
     * @param documentId 知识文档编号
     * @return 处理日志列表响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}/process-logs")
    public ApiResponse<List<KnowledgeDocumentProcessLogResponse>> listProcessLogs(@PathVariable String documentId) {
        List<KnowledgeDocumentProcessLogResponse> responses = knowledgeDocumentManagementApplicationService.listProcessLogs(documentId);
        return ApiResponse.ok(responses);
    }

    /**
     * queryChunk（查询单个切片）接口用于读取指定文档下的某个切片详情。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 知识切片响应
     */
    @GetMapping("/api/knowledge-documents/{documentId}/chunks/{chunkId}")
    public ApiResponse<KnowledgeChunkResponse> queryChunk(@PathVariable String documentId, @PathVariable String chunkId) {
        KnowledgeChunkResponse response = knowledgeDocumentManagementApplicationService.queryChunk(documentId, chunkId);
        return ApiResponse.ok(response);
    }

    /**
     * createChunk（新增切片）接口用于人工补充一个切片。
     *
     * @param documentId 知识文档编号
     * @param request 新增切片请求
     * @return 新增后的切片响应
     */
    @PostMapping("/api/knowledge-documents/{documentId}/chunks")
    public ApiResponse<KnowledgeChunkResponse> createChunk(@PathVariable String documentId,
                                                            @RequestBody CreateKnowledgeChunkRequest request) {
        KnowledgeChunkResponse response = knowledgeDocumentManagementApplicationService.createChunk(documentId, request);
        return ApiResponse.ok(response);
    }

    /**
     * updateChunk（更新切片）接口用于修改人工维护的切片内容。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param request 更新切片请求
     * @return 更新后的切片响应
     */
    @PutMapping("/api/knowledge-documents/{documentId}/chunks/{chunkId}")
    public ApiResponse<KnowledgeChunkResponse> updateChunk(@PathVariable String documentId,
                                                            @PathVariable String chunkId,
                                                            @RequestBody UpdateKnowledgeChunkRequest request) {
        KnowledgeChunkResponse response = knowledgeDocumentManagementApplicationService.updateChunk(documentId, chunkId, request);
        return ApiResponse.ok(response);
    }

    /**
     * setChunkEnabled（切片启停）接口用于临时控制某个切片是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @param enabled 是否启用
     * @return 切片响应
     */
    @PatchMapping("/api/knowledge-documents/{documentId}/chunks/{chunkId}/enabled")
    public ApiResponse<KnowledgeChunkResponse> setChunkEnabled(@PathVariable String documentId,
                                                                @PathVariable String chunkId,
                                                                @RequestParam boolean enabled) {
        KnowledgeChunkResponse response = knowledgeDocumentManagementApplicationService.setChunkEnabled(documentId, chunkId, enabled);
        return ApiResponse.ok(response);
    }

    /**
     * deleteChunk（删除切片）接口用于逻辑删除单个切片。
     *
     * @param documentId 知识文档编号
     * @param chunkId 知识切片编号
     * @return 被删除的切片编号
     */
    @DeleteMapping("/api/knowledge-documents/{documentId}/chunks/{chunkId}")
    public ApiResponse<String> deleteChunk(@PathVariable String documentId, @PathVariable String chunkId) {
        String deletedChunkId = knowledgeDocumentManagementApplicationService.deleteChunk(documentId, chunkId);
        return ApiResponse.ok(deletedChunkId);
    }

    /**
     * setDocumentEnabled（文档启停）接口用于临时控制整个文档是否参与检索。
     *
     * @param documentId 知识文档编号
     * @param enabled 是否启用
     * @return 文档响应
     */
    @PatchMapping("/api/knowledge-documents/{documentId}/enabled")
    public ApiResponse<KnowledgeDocumentResponse> setDocumentEnabled(@PathVariable String documentId,
                                                                     @RequestParam boolean enabled) {
        KnowledgeDocumentResponse response = knowledgeDocumentManagementApplicationService.setDocumentEnabled(documentId, enabled);
        return ApiResponse.ok(response);
    }

    /**
     * deleteDocument（删除文档）接口用于逻辑删除文档及其全部切片。
     *
     * @param documentId 知识文档编号
     * @return 被删除的文档编号
     */
    @DeleteMapping("/api/knowledge-documents/{documentId}")
    public ApiResponse<String> deleteDocument(@PathVariable String documentId) {
        String deletedDocumentId = knowledgeDocumentManagementApplicationService.deleteDocument(documentId);
        return ApiResponse.ok(deletedDocumentId);
    }
}
