package com.clipforge.creation.application.api;

import com.clipforge.contract.creation.CreateMarkdownExportRequest;
import com.clipforge.contract.creation.CreationMarkdownExportResponse;
import java.nio.file.Path;

/**
 * CreationMarkdownExportUseCase（创作 Markdown 导出用例）。
 */
public interface CreationMarkdownExportUseCase {

    /**
     * exportByRunId（按运行编号导出）用于把一次创作运行的通用产物导出为 Markdown 文件。
     *
     * @param runId 运行编号
     * @param request 导出请求
     * @return 导出响应
     */
    CreationMarkdownExportResponse exportByRunId(String runId, CreateMarkdownExportRequest request);

    /**
     * queryExport（查询导出记录）用于按导出编号读取导出详情。
     *
     * @param exportId 导出编号
     * @return 导出响应
     */
    CreationMarkdownExportResponse queryExport(String exportId);

    /**
     * resolveDownloadFile（解析下载文件）用于按导出编号定位本地 Markdown 文件。
     *
     * @param exportId 导出编号
     * @return 文件路径
     */
    Path resolveDownloadFile(String exportId);
}
