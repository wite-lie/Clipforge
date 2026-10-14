package com.clipforge.contract.creation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ArticleSourceEvidence（图文来源证据）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleSourceEvidence {

    /** sourceType（来源类型）表示 RAG（检索增强生成）、素材摘要、视频摘要或创作知识。 */
    private String sourceType;

    /** sourceId（来源编号）表示知识切片、素材或视频摘要编号；没有时可为空。 */
    private String sourceId;

    /** quote（引用内容）表示可支撑图文段落的简短依据。 */
    private String quote;
}
