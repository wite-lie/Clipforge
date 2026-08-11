package com.clipforge.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UpdateKnowledgeChunkRequest（更新知识切片请求）。
 *
 * <p>该对象用于管理接口修改切片正文、标题路径和元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKnowledgeChunkRequest {

    /** titlePath（标题路径）。 */
    private String titlePath;

    /** content（切片正文）。 */
    private String content;

    /** metadataJson（切片元数据 JSON）。 */
    private String metadataJson;
}
