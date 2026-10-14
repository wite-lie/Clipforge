package com.clipforge.application;

import com.clipforge.contract.knowledge.KnowledgeDocumentResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeChunkStrategy;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentOverallStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentSourceType;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentStepStatus;
import com.clipforge.knowledge.application.api.KnowledgeDocumentUseCase;
import com.clipforge.knowledge.application.api.KnowledgeDocumentUploadCommand;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeBaseRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentMetadataRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.knowledge.application.document.KnowledgeDocumentManagementUseCase;
import com.clipforge.rag.KnowledgeFileStorage;
import com.clipforge.rag.KnowledgeStoredFile;
import com.clipforge.rag.KnowledgeUploadProperties;
import com.clipforge.knowledge.sync.KnowledgeDocumentUrlSyncProperties;
import com.clipforge.knowledge.sync.UrlSafetyValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentApplicationService（知识文档应用服务）。
 *
 * <p>该服务负责编排知识文档上传、本地文件存储、文档元数据落库和文档查询，支持 Markdown（标记文档）、TXT（纯文本）、PDF（便携式文档格式）和 Word（文字处理文档）。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentApplicationService implements KnowledgeDocumentUseCase {

    /** knowledgeBaseRepositoryProvider（知识库仓储提供器）用于校验知识库是否存在。 */
    private final ObjectProvider<KnowledgeBaseRepository> knowledgeBaseRepositoryProvider;

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeDocumentManagementUseCaseProvider（知识文档管理用例提供器）用于查询文档而不暴露 Knowledge 仓储。 */
    private final ObjectProvider<KnowledgeDocumentManagementUseCase> knowledgeDocumentManagementUseCaseProvider;

    /** knowledgeDocumentMetadataRepositoryProvider（知识文档元数据仓储提供器）用于保存文档级键值元数据。 */
    private final ObjectProvider<KnowledgeDocumentMetadataRepository> knowledgeDocumentMetadataRepositoryProvider;

    /** knowledgeFileStorage（知识文件存储）用于保存上传的知识文件。 */
    private final KnowledgeFileStorage knowledgeFileStorage;

    /** knowledgeUploadProperties（知识上传配置）用于校验文件类型和大小。 */
    private final KnowledgeUploadProperties knowledgeUploadProperties;

    /** urlSyncProperties（URL 同步配置）用于校验 URL 来源文档安全边界。 */
    private final KnowledgeDocumentUrlSyncProperties urlSyncProperties;

    /** urlSafetyValidator（URL 安全校验器）用于创建 URL 来源文档时提前拦截危险地址。 */
    private final UrlSafetyValidator urlSafetyValidator;

    /** objectMapper（对象映射器）用于生成 metadataJson（元数据 JSON）。 */
    private final ObjectMapper objectMapper;

    /**
     * uploadDocument（上传知识文档）用于保存本地文件并写入知识文档元数据。
     *
     * @param command 上传命令
     * @return 知识文档响应
     */
    @Transactional
    public KnowledgeDocumentResponse uploadDocument(KnowledgeDocumentUploadCommand command) {
        // 先获取仓储，未启用数据库时在保存文件前失败，避免产生孤儿文件。
        KnowledgeDocumentRepository documentRepository = requiredKnowledgeDocumentRepository();
        // 查询知识库实体，确认文档归属合法。
        KnowledgeBaseEntity knowledgeBase = requiredKnowledgeBase(command == null ? null : command.getKnowledgeBaseId());
        // 校验上传文件的大小、扩展名和内容类型。
        validateKnowledgeFile(command);
        // 保存文件到知识文件存储。
        KnowledgeStoredFile storedFile = knowledgeFileStorage.store(command);
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 选择文档所属用户，优先使用请求用户，否则沿用知识库用户。
        String resolvedUserId = resolveUserId(command.getUserId(), knowledgeBase.getUserId());
        // 提取原始文件名。
        String originalFilename = defaultOriginalFilename(command.getOriginalFilename(), storedFile.getStoredFilename());
        // 提取扩展名。
        String extension = extractExtension(originalFilename);
        // 构造知识文档实体。
        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                // 设置数据库主键。
                .id(newId("knowledge_document_pk"))
                // 设置对外文档编号。
                .documentId(newId("knowledge_document"))
                // 设置知识库编号。
                .knowledgeBaseId(command.getKnowledgeBaseId())
                // 设置用户编号。
                .userId(resolvedUserId)
                // 设置来源类型为本地上传。
                .sourceType(KnowledgeDocumentSourceType.LOCAL_UPLOAD.value())
                // 本地上传没有远程来源地址。
                .sourceLocation(null)
                // 设置原始文件名。
                .originalFilename(originalFilename)
                // 设置存储文件名。
                .storedFilename(storedFile.getStoredFilename())
                // 设置文件路径。
                .filePath(storedFile.getFilePath())
                // 设置内容类型。
                .contentType(command.getContentType())
                // 设置文件大小。
                .fileSize(command.getSize())
                // 设置文件哈希。
                .fileHash(storedFile.getFileHash())
                // 设置解析状态为待处理。
                .parseStatus(KnowledgeDocumentStepStatus.PENDING.value())
                // 设置切片状态为待处理。
                .chunkStatus(KnowledgeDocumentStepStatus.PENDING.value())
                // 设置向量化状态为待处理。
                .embeddingStatus(KnowledgeDocumentStepStatus.PENDING.value())
                // 设置整体状态为待处理。
                .overallStatus(KnowledgeDocumentOverallStatus.PENDING.value())
                // 当前没有错误消息。
                .errorMessage(null)
                // 设置切片策略，优先继承知识库默认值。
                .chunkStrategy(defaultText(knowledgeBase.getChunkStrategy(), KnowledgeChunkStrategy.defaultValue()))
                // 设置切片配置，优先继承知识库默认值。
                .chunkConfigJson(knowledgeBase.getChunkConfigJson())
                // 设置文档元数据 JSON。
                .metadataJson(buildMetadataJson("LOCAL", extension))
                // 新上传文档尚未切片。
                .chunkCount(0)
                // 新上传文档尚未解析字符数。
                .charCount(0)
                // 新上传文档尚未估算 Token（词元）数量。
                .tokenCount(0)
                // 默认启用文档。
                .enabled(1)
                // 本地上传文件默认不启用 URL（统一资源定位符）定时同步。
                .scheduleEnabled(0)
                // 本地上传文件没有定时表达式。
                .scheduleCron(null)
                // 本地上传文件没有 ETag（资源版本标识）。
                .lastEtag(null)
                // 本地上传文件没有远程 Last-Modified（最近修改时间）。
                .lastModified(null)
                // 本地上传文件没有远程内容哈希。
                .lastContentHash(null)
                // 文档还没有完成处理。
                .lastProcessTime(null)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 构造文档级键值元数据。
        List<KnowledgeDocumentMetadataEntity> metadataList = buildDocumentMetadataEntities(entity, "LOCAL", extension, now);
        // 保存知识文档实体，失败时尽量删除已经保存的本地文件，避免产生孤儿文件。
        KnowledgeDocumentEntity savedEntity = saveDocumentOrCleanupFile(documentRepository, entity, metadataList, storedFile);
        // 转换为接口响应。
        return toKnowledgeDocumentResponse(savedEntity);
    }

    /**
     * createUrlDocument（创建 URL 来源文档）用于只登记远程文档地址，实际抓取由 URL 同步任务触发。
     *
     * @param knowledgeBaseId 知识库编号
     * @param sourceUrl 远程 URL 地址
     * @param userId 用户编号
     * @return 知识文档响应
     */
    @Transactional
    public KnowledgeDocumentResponse createUrlDocument(String knowledgeBaseId, String sourceUrl, String userId) {
        KnowledgeDocumentRepository documentRepository = requiredKnowledgeDocumentRepository();
        KnowledgeBaseEntity knowledgeBase = requiredKnowledgeBase(knowledgeBaseId);
        validateSourceUrl(sourceUrl);
        LocalDateTime now = LocalDateTime.now();
        String resolvedUserId = resolveUserId(userId, knowledgeBase.getUserId());
        String originalFilename = resolveUrlFilename(sourceUrl);
        String extension = extractExtension(originalFilename);
        KnowledgeDocumentEntity entity = KnowledgeDocumentEntity.builder()
                .id(newId("knowledge_document_pk"))
                .documentId(newId("knowledge_document"))
                .knowledgeBaseId(knowledgeBaseId)
                .userId(resolvedUserId)
                .sourceType(KnowledgeDocumentSourceType.URL.value())
                .sourceLocation(sourceUrl.strip())
                .originalFilename(originalFilename)
                .storedFilename(null)
                .filePath(null)
                .contentType(null)
                .fileSize(0L)
                .fileHash(null)
                .parseStatus(KnowledgeDocumentStepStatus.PENDING.value())
                .chunkStatus(KnowledgeDocumentStepStatus.PENDING.value())
                .embeddingStatus(KnowledgeDocumentStepStatus.PENDING.value())
                .overallStatus(KnowledgeDocumentOverallStatus.PENDING.value())
                .errorMessage(null)
                .chunkStrategy(defaultText(knowledgeBase.getChunkStrategy(), KnowledgeChunkStrategy.defaultValue()))
                .chunkConfigJson(knowledgeBase.getChunkConfigJson())
                .metadataJson(buildMetadataJson("URL", extension))
                .chunkCount(0)
                .charCount(0)
                .tokenCount(0)
                .enabled(1)
                .scheduleEnabled(0)
                .scheduleCron(null)
                .lastEtag(null)
                .lastModified(null)
                .lastContentHash(null)
                .lastProcessTime(null)
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        List<KnowledgeDocumentMetadataEntity> metadataList = buildDocumentMetadataEntities(entity, "URL", extension, now);
        KnowledgeDocumentEntity savedEntity = documentRepository.save(entity);
        requiredKnowledgeDocumentMetadataRepository().saveAll(metadataList);
        return toKnowledgeDocumentResponse(savedEntity);
    }

    /**
     * saveDocumentOrCleanupFile（保存文档或清理文件）用于在数据库落库失败时尽量删除已保存文件。
     *
     * @param documentRepository 知识文档仓储
     * @param entity 知识文档实体
     * @param metadataList 文档元数据列表
     * @param storedFile 已保存知识文件
     * @return 已保存知识文档实体
     */
    private KnowledgeDocumentEntity saveDocumentOrCleanupFile(KnowledgeDocumentRepository documentRepository, KnowledgeDocumentEntity entity, List<KnowledgeDocumentMetadataEntity> metadataList, KnowledgeStoredFile storedFile) {
        try {
            // 尝试保存知识文档实体。
            KnowledgeDocumentEntity savedEntity = documentRepository.save(entity);
            // 同步保存文档级键值元数据。
            requiredKnowledgeDocumentMetadataRepository().saveAll(metadataList);
            // 返回已保存知识文档。
            return savedEntity;
        } catch (RuntimeException exception) {
            // 数据库写入失败时尽量删除刚保存的本地文件。
            deleteStoredFileQuietly(storedFile);
            // 继续抛出原异常，让全局异常处理器返回错误。
            throw exception;
        }
    }

    /**
     * deleteStoredFileQuietly（静默删除已保存文件）用于补偿数据库写入失败后的本地文件。
     *
     * @param storedFile 已保存知识文件
     */
    private void deleteStoredFileQuietly(KnowledgeStoredFile storedFile) {
        // 如果存储结果为空，则无需清理。
        if (storedFile == null || storedFile.getFilePath() == null || storedFile.getFilePath().isBlank()) {
            // 直接返回。
            return;
        }
        try {
            // 按文件路径删除文件，不抛出删除失败异常。
            Files.deleteIfExists(Path.of(storedFile.getFilePath()));
        } catch (IOException ignored) {
            // 删除补偿失败时不覆盖原始数据库异常。
        }
    }

    /**
     * queryDocument（查询知识文档）用于按文档编号读取详情。
     *
     * @param documentId 知识文档编号
     * @return 知识文档响应
     */
    public KnowledgeDocumentResponse queryDocument(String documentId) {
        // 查询文档实体，不存在时抛出数据不存在异常。
        KnowledgeDocumentEntity entity = requiredKnowledgeDocumentManagementUseCase().findByDocumentId(documentId)
                // 文档不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
        // 转换为接口响应。
        return toKnowledgeDocumentResponse(entity);
    }

    /**
     * listDocuments（查询知识文档列表）用于读取某个知识库下的全部文档。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识文档响应列表
     */
    public List<KnowledgeDocumentResponse> listDocuments(String knowledgeBaseId) {
        // 确认知识库存在，避免对不存在的知识库返回误导性的空列表。
        requiredKnowledgeBase(knowledgeBaseId);
        // 查询知识文档列表并转换为响应列表。
        return requiredKnowledgeDocumentManagementUseCase().listByKnowledgeBaseId(knowledgeBaseId).stream()
                // 转换单个知识文档实体。
                .map(this::toKnowledgeDocumentResponse)
                // 收集为列表。
                .toList();
    }

    /**
     * validateKnowledgeFile（校验知识文件）用于检查文件非空、大小、扩展名和内容类型。
     *
     * @param command 上传命令
     */
    private void validateKnowledgeFile(KnowledgeDocumentUploadCommand command) {
        // 如果文件为空引用，则拒绝请求。
        if (command == null) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文件不能为空");
        }
        // 如果文件内容为空，则拒绝请求。
        if (command.isEmpty() || command.getContent() == null) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文件不能为空");
        }
        // 如果文件大小超过业务配置限制，则拒绝请求。
        if (command.getSize() > knowledgeUploadProperties.getMaxFileSizeBytes()) {
            // 抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文件超过大小限制，当前单文件最大允许 " + knowledgeUploadProperties.getMaxFileSizeBytes() + " 字节");
        }
        // 提取扩展名。
        String extension = extractExtension(command.getOriginalFilename());
        // 如果扩展名不在白名单中，则拒绝请求。
        if (!knowledgeUploadProperties.getAllowedExtensions().contains(extension)) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前仅支持 md、txt、pdf、doc 和 docx 知识文件");
        }
        // 读取内容类型。
        String contentType = command.getContentType();
        // 如果内容类型不在白名单中，则拒绝请求。
        if (contentType != null && !knowledgeUploadProperties.getAllowedContentTypes().contains(contentType)) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识文件内容类型不受支持：" + contentType);
        }
    }

    /**
     * requiredKnowledgeBase（获取必需知识库）用于校验知识库存在。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识库实体
     */
    private KnowledgeBaseEntity requiredKnowledgeBase(String knowledgeBaseId) {
        // 查询知识库实体，不存在时抛出数据不存在异常。
        return requiredKnowledgeBaseRepository().findByKnowledgeBaseId(knowledgeBaseId)
                // 知识库不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识库，knowledgeBaseId：" + knowledgeBaseId));
    }

    /**
     * requiredKnowledgeBaseRepository（获取必需知识库仓储）用于处理未启用数据库的场景。
     *
     * @return 知识库仓储
     */
    private KnowledgeBaseRepository requiredKnowledgeBaseRepository() {
        // 从 Spring（Spring 框架）容器中获取知识库仓储。
        KnowledgeBaseRepository repository = knowledgeBaseRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentRepository（获取必需知识文档仓储）用于处理未启用数据库的场景。
     *
     * @return 知识文档仓储
     */
    private KnowledgeDocumentRepository requiredKnowledgeDocumentRepository() {
        // 从 Spring（Spring 框架）容器中获取知识文档仓储。
        KnowledgeDocumentRepository repository = knowledgeDocumentRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentManagementUseCase（获取必需文档管理用例）用于查询文档和列表。
     *
     * @return Knowledge 文档管理用例
     */
    private KnowledgeDocumentManagementUseCase requiredKnowledgeDocumentManagementUseCase() {
        // 从 Spring（Spring 框架）容器中获取 Knowledge 文档管理用例。
        KnowledgeDocumentManagementUseCase useCase = knowledgeDocumentManagementUseCaseProvider.getIfAvailable();
        // 如果用例不存在，说明没有启用 db（数据库）Profile。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档查询需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用用例。
        return useCase;
    }

    /**
     * requiredKnowledgeDocumentMetadataRepository（获取必需知识文档元数据仓储）用于处理未启用数据库的场景。
     *
     * @return 知识文档元数据仓储
     */
    private KnowledgeDocumentMetadataRepository requiredKnowledgeDocumentMetadataRepository() {
        // 从 Spring（Spring 框架）容器中获取知识文档元数据仓储。
        KnowledgeDocumentMetadataRepository repository = knowledgeDocumentMetadataRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档元数据接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * buildDocumentMetadataEntities（构建文档元数据实体）用于同步写入 cf_knowledge_document_metadata（知识文档元数据表）。
     *
     * @param entity 知识文档实体
     * @param extension 文件扩展名
     * @param now 当前时间
     * @return 文档元数据实体列表
     */
    private List<KnowledgeDocumentMetadataEntity> buildDocumentMetadataEntities(KnowledgeDocumentEntity entity, String uploadChannel, String extension, LocalDateTime now) {
        // 创建元数据列表。
        java.util.ArrayList<KnowledgeDocumentMetadataEntity> metadataList = new java.util.ArrayList<>();
        // 添加上传渠道。
        addDocumentMetadata(metadataList, entity, "upload_channel", uploadChannel, "STRING", "SYSTEM", now);
        // 添加文件扩展名。
        addDocumentMetadata(metadataList, entity, "extension", extension, "STRING", "SYSTEM", now);
        // 添加来源类型。
        addDocumentMetadata(metadataList, entity, "source_type", entity.getSourceType(), "STRING", "SYSTEM", now);
        // 添加内容类型。
        addDocumentMetadata(metadataList, entity, "content_type", entity.getContentType(), "STRING", "SYSTEM", now);
        // 添加文件大小。
        addDocumentMetadata(metadataList, entity, "file_size", String.valueOf(entity.getFileSize()), "NUMBER", "SYSTEM", now);
        // 添加文件哈希。
        addDocumentMetadata(metadataList, entity, "file_hash", entity.getFileHash(), "STRING", "SYSTEM", now);
        // 添加原始文件名。
        addDocumentMetadata(metadataList, entity, "original_filename", entity.getOriginalFilename(), "STRING", "SYSTEM", now);
        // 添加存储文件名。
        addDocumentMetadata(metadataList, entity, "stored_filename", entity.getStoredFilename(), "STRING", "SYSTEM", now);
        // 添加文件路径。
        addDocumentMetadata(metadataList, entity, "file_path", entity.getFilePath(), "STRING", "SYSTEM", now);
        // 添加远程来源地址。
        addDocumentMetadata(metadataList, entity, "source_location", entity.getSourceLocation(), "STRING", "SYSTEM", now);
        // 添加用户编号。
        addDocumentMetadata(metadataList, entity, "user_id", entity.getUserId(), "STRING", "USER", now);
        // 添加是否已解析。
        addDocumentMetadata(metadataList, entity, "parsed", "false", "BOOLEAN", "SYSTEM", now);
        // 返回元数据列表。
        return metadataList;
    }

    /**
     * addDocumentMetadata（添加文档元数据）用于创建单条文档键值元数据。
     *
     * @param metadataList 元数据列表
     * @param entity 知识文档实体
     * @param key 元数据键
     * @param value 元数据值
     * @param valueType 值类型
     * @param sourceType 来源类型
     * @param now 当前时间
     */
    private void addDocumentMetadata(List<KnowledgeDocumentMetadataEntity> metadataList, KnowledgeDocumentEntity entity, String key, String value, String valueType, String sourceType, LocalDateTime now) {
        // 如果元数据值为空，则不写入，避免无意义记录。
        if (value == null || value.isBlank()) {
            // 直接返回。
            return;
        }
        // 添加元数据实体。
        metadataList.add(KnowledgeDocumentMetadataEntity.builder()
                // 设置数据库主键。
                .id(newId("knowledge_document_metadata_pk"))
                // 设置对外元数据编号。
                .metadataId(newId("knowledge_document_metadata"))
                // 设置知识库编号。
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                // 设置知识文档编号。
                .documentId(entity.getDocumentId())
                // 设置元数据键。
                .metadataKey(key)
                // 设置元数据值。
                .metadataValue(value)
                // 设置值类型。
                .valueType(valueType)
                // 设置来源类型。
                .sourceType(sourceType)
                // 当前上传阶段不设置置信度。
                .confidence(null)
                // 默认启用。
                .enabled(1)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build());
    }

    /**
     * buildMetadataJson（构建元数据 JSON）用于保存上传阶段可直接获得的文档元数据。
     *
     * @param extension 文件扩展名
     * @return 元数据 JSON（对象表示格式）
     */
    private String buildMetadataJson(String uploadChannel, String extension) {
        try {
            // 使用 Map（映射）构造结构化元数据。
            Map<String, Object> metadata = Map.of(
                    // 保存上传渠道。
                    "uploadChannel", uploadChannel,
                    // 保存文件扩展名。
                    "extension", extension,
                    // 保存当前是否已经解析。
                    "parsed", false
            );
            // 序列化为 JSON（对象表示格式）字符串。
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            // 元数据序列化失败时抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档元数据构建失败", exception);
        }
    }

    /**
     * toKnowledgeDocumentResponse（转知识文档响应）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 知识文档实体
     * @return 知识文档响应
     */
    private KnowledgeDocumentResponse toKnowledgeDocumentResponse(KnowledgeDocumentEntity entity) {
        // 使用 Builder（建造者模式）组装知识文档响应。
        return KnowledgeDocumentResponse.builder()
                // 设置知识文档编号。
                .documentId(entity.getDocumentId())
                // 设置知识库编号。
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                // 设置用户编号。
                .userId(entity.getUserId())
                // 设置来源类型。
                .sourceType(entity.getSourceType())
                // 设置来源位置。
                .sourceLocation(entity.getSourceLocation())
                // 设置原始文件名。
                .originalFilename(entity.getOriginalFilename())
                // 设置存储文件名。
                .storedFilename(entity.getStoredFilename())
                // 设置文件路径。
                .filePath(entity.getFilePath())
                // 设置内容类型。
                .contentType(entity.getContentType())
                // 设置文件大小。
                .fileSize(entity.getFileSize())
                // 设置文件哈希。
                .fileHash(entity.getFileHash())
                // 设置解析状态。
                .parseStatus(entity.getParseStatus())
                // 设置切片状态。
                .chunkStatus(entity.getChunkStatus())
                // 设置向量化状态。
                .embeddingStatus(entity.getEmbeddingStatus())
                // 设置整体状态。
                .overallStatus(entity.getOverallStatus())
                // 设置错误消息。
                .errorMessage(entity.getErrorMessage())
                // 设置切片策略。
                .chunkStrategy(entity.getChunkStrategy())
                // 设置切片配置 JSON。
                .chunkConfigJson(entity.getChunkConfigJson())
                // 设置元数据 JSON。
                .metadataJson(entity.getMetadataJson())
                // 设置切片数量。
                .chunkCount(entity.getChunkCount())
                // 设置字符数。
                .charCount(entity.getCharCount())
                // 设置词元数。
                .tokenCount(entity.getTokenCount())
                // 设置启用状态。
                .enabled(entity.getEnabled())
                // 设置创建时间。
                .createTime(entity.getCreateTime())
                // 设置更新时间。
                .updateTime(entity.getUpdateTime())
                // 构建响应。
                .build();
    }

    /**
     * resolveUserId（解析用户编号）用于优先使用请求用户，缺省时继承知识库用户。
     *
     * @param requestUserId 请求用户编号
     * @param knowledgeBaseUserId 知识库用户编号
     * @return 最终用户编号
     */
    private String resolveUserId(String requestUserId, String knowledgeBaseUserId) {
        // 如果请求传入用户编号，则优先使用请求用户编号。
        if (requestUserId != null && !requestUserId.isBlank()) {
            // 返回去掉首尾空白后的请求用户编号。
            return requestUserId.strip();
        }
        // 如果请求未传用户编号，则继承知识库用户编号。
        return knowledgeBaseUserId;
    }

    /**
     * defaultOriginalFilename（默认原始文件名）用于处理客户端未传原始文件名的少数情况。
     *
     * @param originalFilename 客户端上传时携带的原始文件名
     * @param storedFilename 系统保存时生成的文件名
     * @return 可落库的原始文件名
     */
    private String defaultOriginalFilename(String originalFilename, String storedFilename) {
        // 如果客户端原始文件名为空，则使用系统生成的存储文件名兜底。
        if (originalFilename == null || originalFilename.isBlank()) {
            // 返回存储文件名作为兜底值。
            return storedFilename;
        }
        // 返回客户端传入的原始文件名。
        return originalFilename;
    }

    /**
     * defaultText（默认文本）用于在字符串为空时填充默认值。
     *
     * @param value 原始字符串
     * @param defaultValue 默认字符串
     * @return 可落库字符串
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始字符串为空白，则返回默认值。
        if (value == null || value.isBlank()) {
            // 返回默认字符串。
            return defaultValue;
        }
        // 返回去掉首尾空白后的字符串。
        return value.strip();
    }

    /**
     * extractExtension（提取扩展名）用于从原始文件名中获取后缀。
     *
     * @param originalFilename 原始文件名
     * @return 扩展名
     */
    private String extractExtension(String originalFilename) {
        // 如果文件名为空，则使用空后缀。
        if (originalFilename == null || originalFilename.isBlank()) {
            // 返回空后缀。
            return "";
        }
        // 找到最后一个点号。
        int index = originalFilename.lastIndexOf('.');
        // 如果没有扩展名，则返回空后缀。
        if (index < 0 || index == originalFilename.length() - 1) {
            // 返回空后缀。
            return "";
        }
        // 返回小写扩展名。
        return originalFilename.substring(index).toLowerCase();
    }

    private void validateSourceUrl(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "sourceUrl（来源 URL）不能为空");
        }
        urlSafetyValidator.validate(sourceUrl, urlSyncProperties.getAllowedHosts(), urlSyncProperties.isBlockPrivateNetwork());
    }

    private String resolveUrlFilename(String sourceUrl) {
        try {
            String path = java.net.URI.create(sourceUrl).getPath();
            if (path != null && !path.isBlank()) {
                int index = path.lastIndexOf('/');
                String filename = index >= 0 ? path.substring(index + 1) : path;
                if (filename != null && !filename.isBlank()) {
                    return filename;
                }
            }
            return "remote_document.txt";
        } catch (IllegalArgumentException exception) {
            return "remote_document.txt";
        }
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成本地开发阶段足够唯一的编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
