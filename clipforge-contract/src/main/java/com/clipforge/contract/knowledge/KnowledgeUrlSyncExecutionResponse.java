package com.clipforge.contract.knowledge;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * KnowledgeUrlSyncExecutionResponse（知识库 URL 同步执行响应）。
 */
@Getter
@Builder
public class KnowledgeUrlSyncExecutionResponse {

    private String execId;
    private String scheduleId;
    private String documentId;
    private String knowledgeBaseId;
    private String processLogId;
    private String triggerType;
    private String status;
    private String sourceLocation;
    private String previousEtag;
    private String previousModified;
    private String previousContentHash;
    private String currentEtag;
    private String currentModified;
    private String currentContentHash;
    private Long contentLength;
    private Long fetchDurationMs;
    private Long syncDurationMs;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
