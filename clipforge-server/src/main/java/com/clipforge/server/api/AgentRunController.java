package com.clipforge.server.api;

import com.clipforge.contract.knowledge.QueryModelCallRecordsResponse;
import com.clipforge.contract.knowledge.QueryQualityReportResponse;
import com.clipforge.contract.knowledge.QueryTraceResponse;
import com.clipforge.application.CreationQueryApplicationService;
import com.clipforge.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AgentRunController（智能体运行控制器）。
 *
 * <p>该控制器负责查询 Agent（智能体）运行级信息，例如 Trace（执行链路追踪）。
 */
@RestController
@RequestMapping("/api/agent-runs")
public class AgentRunController {

    /** creationQueryApplicationService（创作查询应用服务）负责查询已经持久化的执行链路。 */
    private final CreationQueryApplicationService creationQueryApplicationService;

    /**
     * 构造函数用于注入创作查询应用服务。
     *
     * @param creationQueryApplicationService 创作查询应用服务
     */
    public AgentRunController(CreationQueryApplicationService creationQueryApplicationService) {
        this.creationQueryApplicationService = creationQueryApplicationService;
    }

    /**
     * queryTrace（查询执行链路）接口用于按 runId（运行编号）读取已经持久化的 Trace（执行链路追踪）。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询执行链路响应
     */
    @GetMapping("/{runId}/trace")
    public ApiResponse<QueryTraceResponse> queryTrace(@PathVariable String runId) {
        // 调用 Application（应用编排层）查询执行链路。
        QueryTraceResponse response = creationQueryApplicationService.queryTraceByRunId(runId);
        // 使用 ApiResponse（统一接口响应）包装查询结果。
        return ApiResponse.ok(response);
    }

    /**
     * queryModelCalls（查询模型调用记录）接口用于按 runId（运行编号）读取模型调用 Token（词元）消耗明细。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询模型调用记录响应
     */
    @GetMapping("/{runId}/model-calls")
    public ApiResponse<QueryModelCallRecordsResponse> queryModelCalls(@PathVariable String runId) {
        // 调用 Application（应用编排层）查询模型调用记录。
        QueryModelCallRecordsResponse response = creationQueryApplicationService.queryModelCallRecordsByRunId(runId);
        // 使用 ApiResponse（统一接口响应）包装查询结果。
        return ApiResponse.ok(response);
    }

    /**
     * queryQualityReport（查询质量复盘）接口用于聚合查看评估、修正和 Token（词元）成本。
     *
     * @param runId Agent（智能体）运行编号
     * @return 查询质量复盘响应
     */
    @GetMapping("/{runId}/quality-report")
    public ApiResponse<QueryQualityReportResponse> queryQualityReport(@PathVariable String runId) {
        // 调用 Application（应用编排层）查询质量复盘。
        QueryQualityReportResponse response = creationQueryApplicationService.queryQualityReportByRunId(runId);
        // 使用 ApiResponse（统一接口响应）包装查询结果。
        return ApiResponse.ok(response);
    }
}
