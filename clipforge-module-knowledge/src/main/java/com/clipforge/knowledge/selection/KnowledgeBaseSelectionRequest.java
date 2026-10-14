package com.clipforge.knowledge.selection;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KnowledgeBaseSelectionRequest（知识库选择请求）。
 *
 * <p>该对象承载一次创作请求在进入 RAG（检索增强生成）前可用于选择知识库范围的信息。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseSelectionRequest {

    /** explicitKnowledgeBaseIds（显式知识库编号列表）表示用户或前端手动选择的知识库。 */
    private List<String> explicitKnowledgeBaseIds;

    /** projectId（项目编号）用于匹配项目默认知识库。 */
    private String projectId;

    /** platform（目标平台）用于匹配平台默认知识库。 */
    private String platform;

    /** goal（创作目标）为后续按主题自动推荐知识库预留。 */
    private String goal;
}
