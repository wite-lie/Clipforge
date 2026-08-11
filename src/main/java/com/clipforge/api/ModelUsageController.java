package com.clipforge.api;

import com.clipforge.api.dto.ModelUsageSummaryResponse;
import com.clipforge.application.ModelUsageQueryApplicationService;
import com.clipforge.common.ApiResponse;
import com.clipforge.model.usage.ModelCallRecordQuery;
import com.clipforge.model.usage.ModelUsageGroupBy;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ModelUsageController（模型用量控制器）。
 *
 * <p>该控制器负责按运行、项目、会话、供应商、模型、用途和时间范围查询模型 Token（词元）用量和估算成本。
 */
@RestController
@RequestMapping("/api/model-usage")
@RequiredArgsConstructor
public class ModelUsageController {

    /** modelUsageQueryApplicationService（模型用量查询应用服务）负责模型用量聚合查询。 */
    private final ModelUsageQueryApplicationService modelUsageQueryApplicationService;

    /**
     * querySummary（查询汇总）用于返回模型调用 Token（词元）和成本统计。
     */
    @GetMapping("/summary")
    public ApiResponse<ModelUsageSummaryResponse> querySummary(@RequestParam(required = false) String runId,
                                                               @RequestParam(required = false) String projectId,
                                                               @RequestParam(required = false) String conversationId,
                                                               @RequestParam(required = false) String provider,
                                                               @RequestParam(required = false) String modelName,
                                                               @RequestParam(required = false) String purpose,
                                                               @RequestParam(required = false) Boolean success,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                                               @RequestParam(required = false, defaultValue = "NONE") ModelUsageGroupBy groupBy) {
        ModelCallRecordQuery query = ModelCallRecordQuery.builder()
                .runId(runId)
                .projectId(projectId)
                .conversationId(conversationId)
                .provider(provider)
                .modelName(modelName)
                .purpose(purpose)
                .success(success)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        ModelUsageSummaryResponse response = modelUsageQueryApplicationService.querySummary(query, groupBy);
        return ApiResponse.ok(response);
    }
}
