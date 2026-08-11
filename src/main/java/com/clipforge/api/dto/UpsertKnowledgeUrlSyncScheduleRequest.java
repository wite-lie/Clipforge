package com.clipforge.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * UpsertKnowledgeUrlSyncScheduleRequest（保存知识库 URL 同步任务请求）。
 */
@Getter
@Setter
public class UpsertKnowledgeUrlSyncScheduleRequest {

    /** scheduleCron（定时表达式）表示 URL 同步周期。 */
    private String scheduleCron;

    /** enabled（是否启用）表示保存后是否启用定时同步。 */
    private Boolean enabled;
}
