package com.clipforge.api;

import com.clipforge.api.dto.CreatePlanRequest;
import com.clipforge.api.dto.CreatePlanResponse;
import com.clipforge.api.dto.QueryPlanResponse;
import com.clipforge.application.CreationApplicationService;
import com.clipforge.application.CreationQueryApplicationService;
import com.clipforge.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CreationController（创作方案控制器）。
 *
 * <p>该控制器负责接收外部创作规划请求，并把请求交给 Application（应用编排层）处理。
 */
@RestController
@RequestMapping("/api/creation")
@RequiredArgsConstructor
public class CreationController {

    /** creationApplicationService（创作应用服务）负责组织一次创作方案生成用例。 */
    private final CreationApplicationService creationApplicationService;

    /** creationQueryApplicationService（创作查询应用服务）负责查询已经持久化的创作方案和执行链路。 */
    private final CreationQueryApplicationService creationQueryApplicationService;

    /**
     * createPlan（创建创作方案）接口用于触发最小 Agent（智能体）执行链路。
     *
     * @param request 创建方案请求
     * @return 创建方案响应
     */
    @PostMapping("/plans")
    public ApiResponse<CreatePlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        // 调用 Application（应用编排层）生成创作方案。
        CreatePlanResponse response = creationApplicationService.createPlan(request);
        // 使用 ApiResponse（统一接口响应）包装业务数据。
        return ApiResponse.ok(response);
    }

    /**
     * queryPlan（查询创作方案）接口用于按 runId（运行编号）读取已经持久化的创作方案。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询创作方案响应
     */
    @GetMapping("/plans/{runId}")
    public ApiResponse<QueryPlanResponse> queryPlan(@PathVariable String runId) {
        // 调用 Application（应用编排层）查询创作方案。
        QueryPlanResponse response = creationQueryApplicationService.queryPlanByRunId(runId);
        // 使用 ApiResponse（统一接口响应）包装查询结果。
        return ApiResponse.ok(response);
    }

}
