package com.clipforge.application;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.contract.creation.CreateMarkdownExportRequest;
import com.clipforge.contract.creation.CreationMarkdownExportResponse;
import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.creation.application.api.CreationMarkdownExportUseCase;
import com.clipforge.creation.export.CreationMarkdownDocument;
import com.clipforge.creation.export.CreationMarkdownFileStorage;
import com.clipforge.creation.export.CreationMarkdownRenderer;
import com.clipforge.memory.infrastructure.persistence.entity.CreationExportEntity;
import com.clipforge.memory.infrastructure.persistence.entity.CreationOutputEntity;
import com.clipforge.memory.infrastructure.persistence.repository.CreationExportRepository;
import com.clipforge.memory.infrastructure.persistence.repository.CreationOutputRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CreationMarkdownExportApplicationService（创作 Markdown 导出应用服务）。
 */
@Service
@RequiredArgsConstructor
public class CreationMarkdownExportApplicationService implements CreationMarkdownExportUseCase {

    /** MARKDOWN_FORMAT（Markdown 格式）表示当前导出格式。 */
    private static final String MARKDOWN_FORMAT = "MARKDOWN";

    /** MARKDOWN_CONTENT_TYPE（Markdown 内容类型）用于下载响应。 */
    private static final String MARKDOWN_CONTENT_TYPE = "text/markdown; charset=UTF-8";

    /** creationOutputRepositoryProvider（创作产物仓储提供器）用于读取通用产物。 */
    private final ObjectProvider<CreationOutputRepository> creationOutputRepositoryProvider;

    /** creationExportRepositoryProvider（创作导出仓储提供器）用于保存和查询导出记录。 */
    private final ObjectProvider<CreationExportRepository> creationExportRepositoryProvider;

    /** creationMarkdownRenderer（Markdown 渲染器）负责确定性渲染文件内容。 */
    private final CreationMarkdownRenderer creationMarkdownRenderer;

    /** creationMarkdownFileStorage（Markdown 文件存储）负责把文件写入本地目录。 */
    private final CreationMarkdownFileStorage creationMarkdownFileStorage;

    @Override
    @Transactional
    public CreationMarkdownExportResponse exportByRunId(String runId, CreateMarkdownExportRequest request) {
        if (!hasText(runId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "runId 不能为空");
        }
        CreationOutputRepository outputRepository = requiredCreationOutputRepository();
        CreationExportRepository exportRepository = requiredCreationExportRepository();
        boolean forceRegenerate = request != null && Boolean.TRUE.equals(request.getForceRegenerate());
        if (!forceRegenerate) {
            CreationMarkdownExportResponse existing = exportRepository.findLatestSucceededByRunId(runId)
                    .filter(entity -> Files.exists(Path.of(entity.getFilePath())))
                    .map(this::toResponse)
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        CreationOutputEntity outputEntity = outputRepository.findLatestByRunId(runId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到可导出的创作产物，runId：" + runId));
        try {
            CreationMarkdownDocument document = creationMarkdownRenderer.render(outputEntity);
            Path savedPath = creationMarkdownFileStorage.save(document);
            long sizeBytes = Files.size(savedPath);
            CreationExportEntity entity = buildSucceededEntity(outputEntity, document, savedPath, sizeBytes);
            return toResponse(exportRepository.save(entity));
        } catch (Exception exception) {
            CreationExportEntity failedEntity = buildFailedEntity(outputEntity, exception);
            exportRepository.save(failedEntity);
            if (exception instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Markdown 导出失败", exception);
        }
    }

    @Override
    public CreationMarkdownExportResponse queryExport(String exportId) {
        CreationExportEntity entity = requiredCreationExportRepository().findByExportId(exportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到导出记录，exportId：" + exportId));
        return toResponse(entity);
    }

    @Override
    public Path resolveDownloadFile(String exportId) {
        CreationExportEntity entity = requiredCreationExportRepository().findByExportId(exportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到导出记录，exportId：" + exportId));
        if (!"SUCCEEDED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "导出记录不是成功状态，无法下载");
        }
        Path filePath = Path.of(entity.getFilePath()).toAbsolutePath().normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "导出文件不存在或已被移动，exportId：" + exportId);
        }
        return filePath;
    }

    private CreationExportEntity buildSucceededEntity(CreationOutputEntity outputEntity, CreationMarkdownDocument document, Path savedPath, long sizeBytes) {
        LocalDateTime now = LocalDateTime.now();
        return CreationExportEntity.builder()
                .id(newId("creation_export_pk"))
                .exportId(newId("creation_export"))
                .runId(outputEntity.getRunId())
                .outputId(outputEntity.getOutputId())
                .outputType(outputEntity.getOutputType())
                .exportFormat(MARKDOWN_FORMAT)
                .fileName(document.getFileName())
                .filePath(savedPath.toString())
                .contentType(MARKDOWN_CONTENT_TYPE)
                .sizeBytes(sizeBytes)
                .status("SUCCEEDED")
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private CreationExportEntity buildFailedEntity(CreationOutputEntity outputEntity, Exception exception) {
        LocalDateTime now = LocalDateTime.now();
        return CreationExportEntity.builder()
                .id(newId("creation_export_pk"))
                .exportId(newId("creation_export"))
                .runId(outputEntity == null ? null : outputEntity.getRunId())
                .outputId(outputEntity == null ? "unknown" : outputEntity.getOutputId())
                .outputType(outputEntity == null ? CreationOutputType.VIDEO_PLAN.name() : outputEntity.getOutputType())
                .exportFormat(MARKDOWN_FORMAT)
                .fileName("failed.md")
                .filePath("")
                .contentType(MARKDOWN_CONTENT_TYPE)
                .sizeBytes(0L)
                .status("FAILED")
                .errorMessage(exception == null ? null : exception.getMessage())
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
    }

    private CreationMarkdownExportResponse toResponse(CreationExportEntity entity) {
        return CreationMarkdownExportResponse.builder()
                .exportId(entity.getExportId())
                .runId(entity.getRunId())
                .outputId(entity.getOutputId())
                .outputType(CreationOutputType.resolveOrDefault(entity.getOutputType()))
                .exportFormat(entity.getExportFormat())
                .fileName(entity.getFileName())
                .filePath(entity.getFilePath())
                .contentType(entity.getContentType())
                .sizeBytes(entity.getSizeBytes())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .createTime(entity.getCreateTime())
                .build();
    }

    private CreationOutputRepository requiredCreationOutputRepository() {
        CreationOutputRepository repository = creationOutputRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "Markdown 导出需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private CreationExportRepository requiredCreationExportRepository() {
        CreationExportRepository repository = creationExportRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "Markdown 导出需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
