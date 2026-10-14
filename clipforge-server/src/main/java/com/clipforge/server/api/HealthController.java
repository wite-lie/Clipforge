package com.clipforge.server.api;

import com.clipforge.common.ApiResponse;
import java.time.OffsetDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HealthController（健康检查控制器）。
 *
 * <p>该控制器用于验证项目是否已经成功启动，是第一阶段最小可运行接口。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * ping（连通性检查）接口用于返回应用基础状态。
     *
     * @return 包含服务状态、服务名称和当前时间的响应数据
     */
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        // 返回一个简单 Map（映射）对象，方便前端或测试快速确认服务可用。
        Map<String, Object> healthData = Map.of(
                // status（状态）字段用于表达当前服务可以正常响应。
                "status", "UP",
                // service（服务）字段用于标识当前响应来自 ClipForge Agent（创作规划智能体）。
                "service", "clipforge-agent",
                // timestamp（时间戳）字段用于标识服务响应时间。
                "timestamp", OffsetDateTime.now().toString());
        // 使用 ApiResponse（统一接口响应）包装健康检查数据。
        return ApiResponse.ok(healthData);
    }
}
