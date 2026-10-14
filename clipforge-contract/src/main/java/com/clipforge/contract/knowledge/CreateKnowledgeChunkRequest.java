package com.clipforge.contract.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateKnowledgeChunkRequest（创建知识切片请求）。
 *
 * <p>该对象用于管理接口人工新增一个切片。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateKnowledgeChunkRequest {

    /** chunkIndex（切片序号）。为空时由系统自动生成。 */
    private Integer chunkIndex;

    /** titlePath（标题路径）。 */
    private String titlePath;

    /** content（切片正文）。 */
    private String content;

    /** metadataJson（切片元数据 JSON）。 */
    private String metadataJson;

    /** enabled（是否启用）。为空时默认启用。 */
    private Boolean enabled;
}
