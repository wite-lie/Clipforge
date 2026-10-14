package com.clipforge.knowledge.application.api;

import com.clipforge.contract.knowledge.CreateKnowledgeBaseRequest;
import com.clipforge.contract.knowledge.KnowledgeBaseResponse;
import java.util.List;

/**
 * KnowledgeBaseUseCase（知识库接口用例）定义 Controller 层需要的知识库容器能力。
 */
public interface KnowledgeBaseUseCase {

    KnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request);

    KnowledgeBaseResponse queryKnowledgeBase(String knowledgeBaseId);

    List<KnowledgeBaseResponse> listKnowledgeBases(String userId, String category, String defaultPlatform, String status, Integer enabled);
}
