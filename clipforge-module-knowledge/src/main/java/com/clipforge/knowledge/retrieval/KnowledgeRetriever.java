package com.clipforge.knowledge.retrieval;

/**
 * KnowledgeRetriever（知识检索器）。
 *
 * <p>该接口是创作 Workflow（工作流）后续接入 RAG（检索增强生成）的统一入口，屏蔽 pgvector（PostgreSQL 向量扩展）、静态降级、混合检索和重排序等内部实现差异。
 */
public interface KnowledgeRetriever {

    /**
     * retrieve（检索）用于根据请求获取候选知识。
     *
     * @param request 知识检索请求
     * @return 知识检索结果
     */
    KnowledgeRetrievalResult retrieve(KnowledgeRetrievalRequest request);
}
