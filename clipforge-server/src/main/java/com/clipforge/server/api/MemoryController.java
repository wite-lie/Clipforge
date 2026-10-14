package com.clipforge.server.api;

import com.clipforge.contract.knowledge.CreationHistoryMemoryResponse;
import com.clipforge.contract.knowledge.DisableMemoryResponse;
import com.clipforge.contract.knowledge.ProjectMemoryResponse;
import com.clipforge.contract.knowledge.UserPreferenceMemoryResponse;
import com.clipforge.common.ApiResponse;
import com.clipforge.memory.application.api.MemoryManagementUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MemoryController（记忆控制器）。
 *
 * <p>该控制器负责 Memory（记忆）的查询和人工停用，避免错误记忆长期影响创作结果。
 */
@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
@Profile("db")
public class MemoryController {

    /** memoryManagementUseCase（记忆管理用例）负责记忆查询和人工修正编排。 */
    private final MemoryManagementUseCase memoryManagementUseCase;

    /**
     * listUserPreferenceMemories（查询用户偏好记忆）接口用于读取用户启用中的长期偏好。
     *
     * @param userId 用户编号
     * @return 用户偏好记忆列表
     */
    @GetMapping("/user-preferences")
    public ApiResponse<List<UserPreferenceMemoryResponse>> listUserPreferenceMemories(@RequestParam String userId) {
        // 调用应用服务查询用户偏好记忆。
        List<UserPreferenceMemoryResponse> responses = memoryManagementUseCase.listUserPreferenceMemories(userId);
        // 返回统一成功响应。
        return ApiResponse.ok(responses);
    }

    /**
     * disableUserPreferenceMemory（停用用户偏好记忆）接口用于人工移除错误偏好。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    @PatchMapping("/user-preferences/{memoryId}/disable")
    public ApiResponse<DisableMemoryResponse> disableUserPreferenceMemory(@PathVariable String memoryId) {
        // 调用应用服务停用用户偏好记忆。
        DisableMemoryResponse response = memoryManagementUseCase.disableUserPreferenceMemory(memoryId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * queryProjectMemory（查询项目记忆）接口用于读取项目启用中的长期画像。
     *
     * @param projectId 项目编号
     * @return 项目记忆响应
     */
    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectMemoryResponse> queryProjectMemory(@PathVariable String projectId) {
        // 调用应用服务查询项目记忆。
        ProjectMemoryResponse response = memoryManagementUseCase.queryProjectMemory(projectId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * disableProjectMemory（停用项目记忆）接口用于人工移除错误项目画像。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    @PatchMapping("/projects/{memoryId}/disable")
    public ApiResponse<DisableMemoryResponse> disableProjectMemory(@PathVariable String memoryId) {
        // 调用应用服务停用项目记忆。
        DisableMemoryResponse response = memoryManagementUseCase.disableProjectMemory(memoryId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * listCreationHistoryMemories（查询创作历史记忆）接口用于读取历史高分方案摘要。
     *
     * @param userId 用户编号
     * @param projectId 项目编号
     * @param platform 目标平台
     * @param limit 最大返回数量
     * @return 创作历史记忆列表
     */
    @GetMapping("/creation-histories")
    public ApiResponse<List<CreationHistoryMemoryResponse>> listCreationHistoryMemories(@RequestParam(required = false) String userId,
                                                                                         @RequestParam(required = false) String projectId,
                                                                                         @RequestParam(required = false) String platform,
                                                                                         @RequestParam(required = false) Integer limit) {
        // 调用应用服务查询创作历史记忆。
        List<CreationHistoryMemoryResponse> responses = memoryManagementUseCase.listCreationHistoryMemories(userId, projectId, platform, limit);
        // 返回统一成功响应。
        return ApiResponse.ok(responses);
    }

    /**
     * disableCreationHistoryMemory（停用创作历史记忆）接口用于人工移除不再适合复用的历史经验。
     *
     * @param memoryId 记忆编号
     * @return 停用结果
     */
    @PatchMapping("/creation-histories/{memoryId}/disable")
    public ApiResponse<DisableMemoryResponse> disableCreationHistoryMemory(@PathVariable String memoryId) {
        // 调用应用服务停用创作历史记忆。
        DisableMemoryResponse response = memoryManagementUseCase.disableCreationHistoryMemory(memoryId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }
}
