package com.clipforge.api.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * KnowledgeUrlSyncScheduleResponse（知识库 URL 同步任务响应）。
 */
@Getter
@Builder
public class KnowledgeUrlSyncScheduleResponse {

    private String scheduleId;
    private String documentId;
    private String knowledgeBaseId;
    private String sourceLocation;
    private String scheduleCron;
    private Integer enabled;
    private String lastEtag;
    private String lastModified;
    private String lastContentHash;
    private LocalDateTime lastSyncTime;
    private LocalDateTime nextSyncTime;
    private String leaseOwner;
    private LocalDateTime leaseUntil;
    private String lastExecStatus;
    private String lastErrorCode;
    private String lastErrorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
