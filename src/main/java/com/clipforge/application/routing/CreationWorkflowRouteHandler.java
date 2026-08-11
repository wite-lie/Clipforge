package com.clipforge.application.routing;

import com.clipforge.agent.intent.ConversationIntentDecision;
import com.clipforge.api.dto.ConversationRouteRequest;
import com.clipforge.api.dto.ConversationRouteResponse;
import com.clipforge.api.dto.CreatePlanRequest;
import com.clipforge.api.dto.CreatePlanResponse;
import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.api.dto.TaskStatusResponse;
import com.clipforge.application.CreationApplicationService;
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
 * CreationWorkflowRouteHandler（创作工作流路由处理器）用于处理生成方案和修正方案请求。
 */
@Component
@RequiredArgsConstructor
public class CreationWorkflowRouteHandler extends AbstractConversationRouteHandler {

    /** creationApplicationService（创作应用服务）用于执行完整创作 Workflow（工作流）。 */
    private final CreationApplicationService creationApplicationService;

    /** taskStatusApplicationService（任务状态应用服务）用于进入创作前确认素材处理状态。 */
    private final TaskStatusApplicationService taskStatusApplicationService;

    /** materialApplicationService（素材应用服务）用于根据素材编号补全处理任务编号。 */
    private final ObjectProvider<MaterialApplicationService> materialApplicationServiceProvider;

    /**
     * supports（是否支持）用于匹配创作工作流路由目标。
     */
    @Override
    public boolean supports(String routeTarget) {
        return "CREATION_WORKFLOW_HANDLER".equals(routeTarget);
    }

    /**
     * handle（处理）用于在素材就绪后进入创作 Workflow（工作流）。
     */
    @Override
    public ConversationRouteResponse handle(ConversationRouteRequest request, ConversationIntentDecision decision, String routeTarget) {
        List<String> missing = missingCreationFields(request);
        if (!missing.isEmpty()) {
            return directAnswer(decision, "CLARIFICATION_HANDLER", "要进入创作 Workflow（工作流），还需要补充：" + String.join("、", missing) + "。", missing);
        }
        List<String> taskIds = resolveProcessTaskIds(request);
        if (!taskIds.isEmpty()) {
            List<TaskStatusResponse> statuses = taskIds.stream()
                    .map(taskStatusApplicationService::queryByTaskId)
                    .toList();
            boolean allSucceeded = statuses.stream().allMatch(status -> "SUCCEEDED".equals(status.getStatus()));
            if (!allSucceeded) {
                return base(decision, "QUERY_MATERIAL_STATUS_HANDLER")
                        .answerText("本轮选择的素材还没有全部处理完成，暂不进入创作 Workflow（工作流）。" + buildTaskStatusAnswer(statuses))
                        .taskStatuses(statuses)
                        .build();
            }
        }
        CreatePlanResponse creation = creationApplicationService.createPlan(CreatePlanRequest.builder()
                .userId(request.getUserId())
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .goal(request.getMessage())
                .platform(request.getPlatform())
                .stylePreference(request.getStylePreference())
                .materialIds(request.getMaterialIds())
                .knowledgeBaseIds(request.getKnowledgeBaseIds())
                .build());
        return base(decision, routeTarget)
                .answerText("已完成创作 Workflow（工作流）执行。")
                .creation(creation)
                .build();
    }

    /**
     * missingCreationFields（创作缺失字段）用于避免不完整请求误触发大模型创作。
     */
    private List<String> missingCreationFields(ConversationRouteRequest request) {
        List<String> missing = new ArrayList<>();
        if (request == null || !hasText(request.getMessage())) {
            missing.add("message");
        }
        if (request == null || !hasText(request.getPlatform())) {
            missing.add("platform");
        }
        return missing;
    }

    /**
     * resolveProcessTaskIds（解析处理任务编号）用于优先使用显式任务编号，必要时从素材编号查询上传响应里的任务编号。
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
