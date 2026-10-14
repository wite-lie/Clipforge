package com.clipforge.server.api;

import com.clipforge.contract.knowledge.TaskStatusResponse;
import com.clipforge.application.TaskStatusApplicationService;
import com.clipforge.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TaskStatusController（任务状态控制器）。
 *
 * <p>该控制器提供统一后台任务状态查询接口，供前端轮询素材处理、RAG（检索增强生成）处理等慢任务状态。
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskStatusController {

    /** taskStatusApplicationService（任务状态应用服务）负责聚合不同业务任务状态。 */
    private final TaskStatusApplicationService taskStatusApplicationService;

    /**
     * queryTaskStatus（查询任务状态）接口用于按任务编号读取统一任务状态。
     *
     * @param taskId 任务编号
     * @return 任务状态响应
     */
    @GetMapping("/{taskId}/status")
    public ApiResponse<TaskStatusResponse> queryTaskStatus(@PathVariable String taskId) {
        // 调用应用服务查询任务状态。
        TaskStatusResponse response = taskStatusApplicationService.queryByTaskId(taskId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }
}
