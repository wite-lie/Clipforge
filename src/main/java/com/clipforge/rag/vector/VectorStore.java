package com.clipforge.rag.vector;

import java.util.List;

/**
 * VectorStore（向量存储）。
 *
 * <p>该接口屏蔽 PostgreSQL + pgvector（PostgreSQL 向量扩展）的具体 SQL（结构化查询语言）实现。
 */
public interface VectorStore {

    /**
     * replaceByDocumentId（按文档编号替换向量）用于短事务删除旧向量并写入新向量。
     *
     * @param request 替换文档向量请求
     */
    void replaceByDocumentId(VectorStoreReplaceDocumentRequest request);

    /**
     * search（检索）用于执行 TopK（前 K 个）向量相似度检索。
     *
     * @param request 向量检索请求
     * @return 检索结果列表
     */
    List<VectorStoreSearchResult> search(VectorStoreSearchRequest request);
}
