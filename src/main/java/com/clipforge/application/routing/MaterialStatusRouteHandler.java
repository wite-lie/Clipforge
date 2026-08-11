package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.api.dto.ConversationRouteRequest;
import com.clipforge.api.dto.ConversationRouteResponse;
import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.api.dto.TaskStatusResponse;
import com.clipforge.application.MaterialApplicationService;
import com.clipforge.application.TaskStatusApplicationService;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * MaterialStatusRouteHandler（素材状态路由处理器）用于查询素材异步处理进度。
 */
@Component
@RequiredArgsConstructor
public class MaterialStatusRouteHandler extends AbstractConversationRouteHandler {

    /** taskStatusApplicationService（任务状态应用服务）用于查询处理任务状态。 */
    private final TaskStatusApplicationService taskStatusApplicationService;

    /** materialApplicationService（素材应用服务）用于根据素材编号补全处理任务编号。 */
    private final ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider;

    /**
     * supports（是否支持）用于匹配素材状态查询和素材处理提示路由目标。
     */
    @Override
    public boolean supports(String routeTarget) {
        return "QUERY_MATERIAL_STATUS_HANDLER".equals(routeTarget) || "PROCESS_MATERIAL_HANDLER".equals(routeTarget);
    }

    /**
     * handle（处理）用于返回处理任务进度，或提示前端传入任务编号。
     */
    @Override
    public ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget) {
        List<String> taskIds = resolveProcessTaskIds(request);
        if (taskIds.isEmpty()) {
            String message = "PROCESS_MATERIAL_HANDLER".equals(routeTarget)
                    ? "素材上传接口会在保存文件后自动创建处理任务并发送异步消息。请把上传响应里的 processTaskId（处理任务编号）传回来，我会继续查询处理进度。"
                    : "请先提供 processTaskIds（处理任务编号）或 materialIds（素材编号），我才能查询素材处理进度。";
            return directAnswer(decision, routeTarget, message, List.of("processTaskIds 或 materialIds"));
        }
        List<TaskStatusResponse> statuses = taskIds.stream()
                .map(taskStatusApplicationService::queryByTaskId)
                .toList();
        return base(decision, routeTarget)
                .answerText(buildTaskStatusAnswer(statuses))
                .taskStatuses(statuses)
                .build();
    }

    /**
     * resolveProcessTaskIds（解析处理任务编号）用于支持前端传任务编号或素材编号两种查询方式。
     */
    private List<String> resolveProcessTaskIds(ConversationRouteRequest request) {
        LinkedHashSet<String> taskIds = new LinkedHashSet<>();
        if (request != null && request.getProcessTaskIds() != null) {
            request.getProcessTaskIds().stream().filter(this::hasText).map(String::strip).forEach(taskIds::add);
        }
        if (!taskIds.isEmpty() || request == null || request.getMaterialIds() == null || request.getMaterialIds().isEmpty()) {
            return new ArrayList<>(taskIds);
        }
        MaterialApplicationService materialService = materialApplicationServiceProvider.getIfAvailable();
        if (materialService == null) {
            return new ArrayList<>(taskIds);
        }
        try {
            List<MaterialResponse> materials = materialService.listMaterialsByIds(request.getMaterialIds(), null);
            materials.stream()
                    .map(MaterialResponse::getProcessTaskId)
                    .filter(this::hasText)
                    .map(String::strip)
                    .forEach(taskIds::add);
        } catch (BusinessException exception) {
            if (exception.getErrorCode() != ErrorCode.PERSISTENCE_DISABLED) {
                throw exception;
            }
        }
        return new ArrayList<>(taskIds);
    }

    /**
     * buildTaskStatusAnswer（构建任务状态答复）用于生成前端可直接展示的简短状态说明。
     */
    private String buildTaskStatusAnswer(List<TaskStatusResponse> statuses) {
        long succeeded = statuses.stream().filter(status -> "SUCCEEDED".equals(status.getStatus())).count();
        long failed = statuses.stream().filter(status -> "FAILED".equals(status.getStatus())).count();
        long processing = statuses.stream().filter(status -> "PROCESSING".equals(status.getStatus()) || "PENDING".equals(status.getStatus())).count();
        return "共查询到 " + statuses.size() + " 个处理任务：成功 " + succeeded + " 个，处理中/等待中 " + processing + " 个，失败 " + failed + " 个。";
    }
}
