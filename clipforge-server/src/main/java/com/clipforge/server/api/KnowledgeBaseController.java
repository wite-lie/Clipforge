package com.clipforge.server.api;

import com.clipforge.contract.knowledge.CreateKnowledgeBaseRequest;
import com.clipforge.contract.knowledge.KnowledgeBaseResponse;
import com.clipforge.knowledge.application.api.KnowledgeBaseUseCase;
import com.clipforge.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * KnowledgeBaseController（知识库控制器）。
 *
 * <p>该控制器负责 RAG（检索增强生成）知识库容器的创建、详情查询和列表查询。
 */
@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    /** knowledgeBaseUseCase（知识库用例）负责知识库接口的应用编排。 */
    private final KnowledgeBaseUseCase knowledgeBaseUseCase;

    /**
     * createKnowledgeBase（创建知识库）接口用于创建一个可承载知识文档的知识库容器。
     *
     * @param request 创建知识库请求
     * @return 知识库响应
     */
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        // 调用应用服务创建知识库。
        KnowledgeBaseResponse response = knowledgeBaseUseCase.createKnowledgeBase(request);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * queryKnowledgeBase（查询知识库）接口用于按知识库编号读取详情。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识库响应
     */
    @GetMapping("/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseResponse> queryKnowledgeBase(@PathVariable String knowledgeBaseId) {
        // 调用应用服务查询知识库详情。
        KnowledgeBaseResponse response = knowledgeBaseUseCase.queryKnowledgeBase(knowledgeBaseId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * listKnowledgeBases（查询知识库列表）接口用于按用户、分类、平台、状态和启用状态筛选知识库。
     *
     * @param userId 用户编号
     * @param category 分类
     * @param defaultPlatform 默认平台
     * @param status 状态
     * @param enabled 是否启用
     * @return 知识库响应列表
     */
    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> listKnowledgeBases(@RequestParam(required = false) String userId,
                                                                       @RequestParam(required = false) String category,
                                                                       @RequestParam(required = false) String defaultPlatform,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Integer enabled) {
        // 调用应用服务查询知识库列表。
        List<KnowledgeBaseResponse> responses = knowledgeBaseUseCase.listKnowledgeBases(userId, category, defaultPlatform, status, enabled);
        // 返回统一成功响应。
        return ApiResponse.ok(responses);
    }
}
