package com.clipforge.knowledge.selection;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * KnowledgeBaseSelectionResult（知识库选择结果）。
 *
 * <p>该对象表示知识库自动选择后的结果，包括知识库编号列表、是否全库检索、选择来源和原因。
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseSelectionResult {

    /** knowledgeBaseIds（知识库编号列表）表示最终要限制检索的知识库范围。 */
    private List<String> knowledgeBaseIds;

    /** searchAllKnowledgeBases（检索全部知识库）表示不限制知识库编号，在全部有效切片中检索。 */
    private boolean searchAllKnowledgeBases;

    /** source（选择来源）表示本次选择命中了哪一个规则。 */
    private KnowledgeBaseSelectionSource source;

    /** reason（选择原因）用于 Trace（执行链路追踪）和前端解释。 */
    private String reason;
}
