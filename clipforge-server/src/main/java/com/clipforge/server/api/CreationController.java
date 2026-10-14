package com.clipforge.server.api;

import com.clipforge.contract.creation.CreateMarkdownExportRequest;
import com.clipforge.contract.creation.CreatePlanRequest;
import com.clipforge.contract.creation.CreationMarkdownExportResponse;
import com.clipforge.contract.knowledge.CreatePlanResponse;
import com.clipforge.contract.knowledge.QueryPlanResponse;
import com.clipforge.creation.application.api.CreationMarkdownExportUseCase;
import com.clipforge.creation.application.api.CreationPlanQueryUseCase;
import com.clipforge.creation.application.api.CreationPlanUseCase;
import com.clipforge.common.ApiResponse;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class CreationController {

    /** creationPlanUseCase（创作方案生成用例）负责组织一次创作方案生成。 */
    private final CreationPlanUseCase creationPlanUseCase;

    /** creationPlanQueryUseCase（创作方案查询用例）负责查询已经持久化的创作方案。 */
    private final CreationPlanQueryUseCase creationPlanQueryUseCase;

    /** creationMarkdownExportUseCase（Markdown 导出用例）负责创作产物导出和下载。 */
    private final CreationMarkdownExportUseCase creationMarkdownExportUseCase;

    /**
     * 构造函数用于注入创作方案生成和查询用例。
     *
     * @param creationPlanUseCase 创作方案生成用例
     * @param creationPlanQueryUseCase 创作方案查询用例
     */
    public CreationController(CreationPlanUseCase creationPlanUseCase,
                              CreationPlanQueryUseCase creationPlanQueryUseCase,
                              CreationMarkdownExportUseCase creationMarkdownExportUseCase) {
        this.creationPlanUseCase = creationPlanUseCase;
        this.creationPlanQueryUseCase = creationPlanQueryUseCase;
        this.creationMarkdownExportUseCase = creationMarkdownExportUseCase;
    }

    /**
     * createPlan（创建创作方案）接口用于触发最小 Agent（智能体）执行链路。
     *
     * @param request 创建方案请求
     * @return 创建方案响应
     */
    @PostMapping("/plans")
    public ApiResponse<CreatePlanResponse> createPlan(@Valid @RequestBody CreatePlanRequest request) {
        // 调用 Application（应用编排层）生成创作方案。
        CreatePlanResponse response = creationPlanUseCase.createPlan(request);
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
        QueryPlanResponse response = creationPlanQueryUseCase.queryPlanByRunId(runId);
        // 使用 ApiResponse（统一接口响应）包装查询结果。
        return ApiResponse.ok(response);
    }

    /**
     * exportMarkdown（导出 Markdown）接口用于把已保存的创作产物导出为 Markdown 文件。
     *
     * @param runId 运行编号
     * @param request 导出请求
     * @return 导出响应
     */
    @PostMapping("/runs/{runId}/exports/markdown")
    public ApiResponse<CreationMarkdownExportResponse> exportMarkdown(@PathVariable String runId,
                                                                       @RequestBody(required = false) CreateMarkdownExportRequest request) {
        CreationMarkdownExportResponse response = creationMarkdownExportUseCase.exportByRunId(runId, request);
        return ApiResponse.ok(response);
    }

    /**
     * queryExport（查询导出记录）接口用于按 exportId（导出编号）读取 Markdown 导出详情。
     *
     * @param exportId 导出编号
     * @return 导出响应
     */
    @GetMapping("/exports/{exportId}")
    public ApiResponse<CreationMarkdownExportResponse> queryExport(@PathVariable String exportId) {
        CreationMarkdownExportResponse response = creationMarkdownExportUseCase.queryExport(exportId);
        return ApiResponse.ok(response);
    }

    /**
     * downloadExport（下载导出文件）接口用于下载 Markdown 文件。
     *
     * @param exportId 导出编号
     * @return 文件响应
     */
    @GetMapping("/exports/{exportId}/download")
    public ResponseEntity<Resource> downloadExport(@PathVariable String exportId) {
        Path filePath = creationMarkdownExportUseCase.resolveDownloadFile(exportId);
        Resource resource = new FileSystemResource(filePath);
        String encodedFileName = URLEncoder.encode(filePath.getFileName().toString(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

}
