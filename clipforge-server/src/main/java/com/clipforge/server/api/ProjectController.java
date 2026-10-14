package com.clipforge.server.api;

import com.clipforge.contract.knowledge.ConversationResponse;
import com.clipforge.contract.knowledge.CreateConversationRequest;
import com.clipforge.contract.knowledge.CreateProjectRequest;
import com.clipforge.contract.knowledge.ProjectResponse;
import com.clipforge.application.ProjectConversationApplicationService;
import com.clipforge.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ProjectController（项目控制器）。
 *
 * <p>该控制器负责 Project（项目）和项目下 Conversation（会话）的创建与查询。
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    /** projectConversationApplicationService（项目会话应用服务）负责项目和会话应用编排。 */
    private final ProjectConversationApplicationService projectConversationApplicationService;

    /**
     * createProject（创建项目）接口用于创建长期创作项目。
     *
     * @param request 创建项目请求
     * @return 项目响应
     */
    @PostMapping
    public ApiResponse<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        // 调用应用服务创建项目。
        ProjectResponse response = projectConversationApplicationService.createProject(request);
        // 使用统一响应包装项目数据。
        return ApiResponse.ok(response);
    }

    /**
     * queryProject（查询项目）接口用于按项目编号读取项目详情。
     *
     * @param projectId 项目编号
     * @return 项目响应
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> queryProject(@PathVariable String projectId) {
        // 调用应用服务查询项目。
        ProjectResponse response = projectConversationApplicationService.queryProject(projectId);
        // 使用统一响应包装项目数据。
        return ApiResponse.ok(response);
    }

    /**
     * listProjects（查询项目列表）接口用于按用户编号读取长期创作项目。
     *
     * @param userId 用户编号
     * @return 项目响应列表
     */
    @GetMapping
    public ApiResponse<List<ProjectResponse>> listProjects(@RequestParam String userId) {
        // 调用应用服务查询项目列表。
        List<ProjectResponse> response = projectConversationApplicationService.listProjects(userId);
        // 使用统一响应包装项目列表。
        return ApiResponse.ok(response);
    }

    /**
     * createConversation（创建会话）接口用于在项目下创建一次连续创作讨论。
     *
     * @param projectId 项目编号
     * @param request 创建会话请求
     * @return 会话响应
     */
    @PostMapping("/{projectId}/conversations")
    public ApiResponse<ConversationResponse> createConversation(@PathVariable String projectId, @Valid @RequestBody CreateConversationRequest request) {
        // 调用应用服务创建会话。
        ConversationResponse response = projectConversationApplicationService.createConversation(projectId, request);
        // 使用统一响应包装会话数据。
        return ApiResponse.ok(response);
    }

    /**
     * listConversations（查询项目会话列表）接口用于读取项目下的全部会话。
     *
     * @param projectId 项目编号
     * @return 会话响应列表
     */
    @GetMapping("/{projectId}/conversations")
    public ApiResponse<List<ConversationResponse>> listConversations(@PathVariable String projectId) {
        // 调用应用服务查询会话列表。
        List<ConversationResponse> response = projectConversationApplicationService.listConversations(projectId);
        // 使用统一响应包装会话列表。
        return ApiResponse.ok(response);
    }
}
