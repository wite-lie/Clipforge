package com.clipforge.rag.retrieval;

import com.clipforge.knowledge.retrieval.KnowledgeRetrievalProperties;
import com.clipforge.knowledge.retrieval.KeywordTokenizer;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalRequest;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalResult;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievalSource;
import com.clipforge.knowledge.retrieval.KnowledgeRetrievedChunk;
import com.clipforge.knowledge.retrieval.KnowledgeRetriever;
import com.clipforge.knowledge.retrieval.RrfHybridRanker;

import java.util.List;

/**
 * KeywordKnowledgeRetriever（关键词知识检索器）。
 *
 * <p>该接口是 Phase 7.10（阶段 7.10）为 BM25（最佳匹配 25）关键词检索预留的抽象。下一小步会实现基于知识切片表的 BM25 召回。
 */
public interface KeywordKnowledgeRetriever {

    /**
     * retrieveCandidates（检索候选知识）用于根据关键词从知识库中召回候选切片。
     *
     * @param request 知识检索请求
     * @param topK 最多召回多少条候选
     * @return 关键词候选切片列表
     */
    List<KnowledgeRetrievedChunk> retrieveCandidates(KnowledgeRetrievalRequest request, int topK);
}
