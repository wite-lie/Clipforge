package com.clipforge.rag.vector;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VectorStoreUpsertItem（向量存储写入条目）。
 *
 * <p>该对象表示一个 KnowledgeChunk（知识切片）对应的一条 Embedding（嵌入）向量。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStoreUpsertItem {

    /** chunkId（知识切片编号）表示向量对应的切片。 */
    private String chunkId;

    /** vector（向量）表示 Embedding（嵌入）模型生成的浮点数组。 */
    private List<Float> vector;
}
