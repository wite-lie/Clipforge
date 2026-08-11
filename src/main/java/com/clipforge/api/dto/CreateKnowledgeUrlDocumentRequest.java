package com.clipforge.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * CreateKnowledgeUrlDocumentRequest（创建 URL 来源知识文档请求）。
 */
@Getter
@Setter
public class CreateKnowledgeUrlDocumentRequest {

    /** sourceUrl（来源 URL）表示远程知识文档地址。 */
    private String sourceUrl;

    /** userId（用户编号）为空时继承知识库用户。 */
    private String userId;

    /** scheduleCron（定时表达式）为空时只创建文档，不创建同步任务。 */
    private String scheduleCron;

    /** scheduleEnabled（是否启用定时同步）为空时默认启用。 */
    private Boolean scheduleEnabled;
}
