package com.clipforge.knowledge.application.processing;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeChunkMetadataEntity;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeChunkRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentMetadataRepository;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeDocumentRepository;
import com.clipforge.knowledge.document.DocumentParseResult;
import com.clipforge.knowledge.document.KnowledgeDocumentParseRequest;
import com.clipforge.knowledge.document.KnowledgeDocumentParser;
import com.clipforge.knowledge.document.LangChain4jDocumentSplitterFactory;
import com.clipforge.knowledge.document.MarkdownSection;
import com.clipforge.knowledge.document.MarkdownSectionParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KnowledgeDocumentChunkingApplicationService（知识文档解析切片应用服务）。
 *
 * <p>该服务负责 Knowledge 模块内部的文档解析、切片、元数据同步和文档状态更新，
 * Markdown（标记文档）先按标题拆章节，章节内部切分能力优先使用 LangChain4j（LangChain 的 Java 封装）。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentChunkingApplicationService implements KnowledgeDocumentChunkingUseCase {

    /** PREVIEW_LIMIT（预览限制）用于控制处理接口返回的切片预览数量。 */
    private static final int PREVIEW_LIMIT = 5;

    /** knowledgeDocumentRepositoryProvider（知识文档仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<KnowledgeDocumentRepository> knowledgeDocumentRepositoryProvider;

    /** knowledgeChunkRepositoryProvider（知识切片仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<KnowledgeChunkRepository> knowledgeChunkRepositoryProvider;

    /** knowledgeDocumentMetadataRepositoryProvider（知识文档元数据仓储提供器）用于追加解析阶段文档元数据。 */
    private final ObjectProvider<KnowledgeDocumentMetadataRepository> knowledgeDocumentMetadataRepositoryProvider;

    /** knowledgeDocumentStatusUseCaseProvider（知识文档状态用例提供器）用于更新文档处理状态。 */
    private final ObjectProvider<KnowledgeDocumentStatusUseCase> knowledgeDocumentStatusUseCaseProvider;

    /** knowledgeDocumentParser（知识文档解析器）用于统一解析 Markdown、TXT、PDF 和 Word 文档。 */
    private final KnowledgeDocumentParser knowledgeDocumentParser;

    /** documentSplitterFactory（文档切分器工厂）负责创建 LangChain4j DocumentSplitter（文档切分器）。 */
    private final LangChain4jDocumentSplitterFactory documentSplitterFactory;

    /** markdownSectionParser（Markdown 章节解析器）用于在 Markdown 文档中先按标题结构拆分章节。 */
    private final MarkdownSectionParser markdownSectionParser;

    /** objectMapper（对象映射器）用于生成切片元数据 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /**
     * processDocument（处理文档）用于同步执行文档解析和切片。
     *
     * @param documentId 知识文档编号
     * @return 文档处理响应
     */
    @Transactional
    @Override
    public KnowledgeDocumentChunkingResult processDocument(String documentId) {
        // 查询文档实体，不存在时抛出数据不存在异常。
        KnowledgeDocumentEntity documentEntity = requiredDocument(documentId);
        try {
            // 使用统一 KnowledgeDocumentParser（知识文档解析器）解析本地文件。
            DocumentParseResult parseResult = knowledgeDocumentParser.parse(KnowledgeDocumentParseRequest.builder()
                    .documentId(documentEntity.getDocumentId())
                    .originalFilename(documentEntity.getOriginalFilename())
                    .filePath(documentEntity.getFilePath())
                    .contentType(documentEntity.getContentType())
                    .sourceType(documentEntity.getSourceType())
                    .build());
            // 读取 LangChain4j（LangChain 的 Java 封装）Document（文档）。
            Document document = parseResult.getDocument();
            // 使用 LangChain4j（LangChain 的 Java 封装）按配置创建 DocumentSplitter（文档切分器）。
            DocumentSplitter splitter = documentSplitterFactory.createSplitter(documentEntity.getChunkStrategy(), documentEntity.getChunkConfigJson());
            // 执行文档切片，得到知识切片实体列表。
            List<KnowledgeChunkEntity> chunkEntities = splitDocumentToChunkEntities(documentEntity, parseResult, splitter);
            // 如果没有生成有效切片，则标记失败。
            if (chunkEntities == null || chunkEntities.isEmpty()) {
                // 抛出知识文件错误。
                throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "文档未生成有效切片");
            }
            // 根据切片实体构造切片级键值元数据。
            List<KnowledgeChunkMetadataEntity> chunkMetadataEntities = buildChunkMetadataEntities(chunkEntities);
            // 替换该文档旧切片。
            requiredKnowledgeChunkRepository().replaceByDocumentId(documentId, chunkEntities, chunkMetadataEntities);
            // 追加文档解析阶段元数据，方便后续复盘 PDF / Word 解析效果。
            saveParseMetadata(documentEntity, parseResult);
            // 计算文档总字符数。
            int charCount = document.text().length();
            // 计算文档估算 Token（词元）数。
            int tokenCount = estimateTokenCount(document.text());
            // 更新文档处理状态为 CHUNKED（已切片）。
            KnowledgeDocumentEntity updatedDocument = requiredKnowledgeDocumentStatusUseCase()
                    .markChunked(documentId, chunkEntities.size(), charCount, tokenCount);
            // 查询已保存切片，保证响应展示数据库实际结果。
            List<KnowledgeChunkEntity> chunksPreview = listChunks(documentId).stream()
                    // 限制返回预览数量。
                    .limit(PREVIEW_LIMIT)
                    // 收集为列表。
                    .toList();
            // 构造 Knowledge 内部处理结果，API DTO 由 legacy 适配器负责转换。
            return KnowledgeDocumentChunkingResult.builder()
                    // 设置更新后的文档实体。
                    .document(updatedDocument)
                    // 设置切片预览实体。
                    .chunksPreview(chunksPreview)
                    // 设置切片数量。
                    .chunkCount(chunkEntities.size())
                    // 设置字符数。
                    .charCount(charCount)
                    // 设置估算 Token 数。
                    .tokenCount(tokenCount)
                    // 构建处理结果。
                    .build();
        } catch (RuntimeException exception) {
            // 处理失败时更新文档状态。
            KnowledgeDocumentEntity failedDocument = requiredKnowledgeDocumentStatusUseCase()
                    .markFailed(documentId, exception.getMessage());
            // 如果原异常已经是业务异常，则继续抛出，保留明确错误码。
            if (exception instanceof BusinessException businessException) {
                // 抛出原业务异常。
                throw businessException;
            }
            // 抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档解析或切片失败，documentId：" + failedDocument.getDocumentId(), exception);
        }
    }

    /**
     * listChunks（查询切片列表）用于读取某个文档下的全部切片。
     *
     * @param documentId 知识文档编号
     * @return 知识切片响应列表
     */
    @Override
    public List<KnowledgeChunkEntity> listChunks(String documentId) {
        // 确认文档存在。
        requiredDocument(documentId);
        // 查询切片实体并转换为响应。
        return requiredKnowledgeChunkRepository().listByDocumentId(documentId).stream()
                // 收集为列表。
                .toList();
    }

    /**
     * splitDocumentToChunkEntities（切分文档为切片实体）用于按文档类型选择切片流程。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     * @param splitter LangChain4j DocumentSplitter（文档切分器）
     * @return 知识切片实体列表
     */
    private List<KnowledgeChunkEntity> splitDocumentToChunkEntities(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult, DocumentSplitter splitter) {
        // 如果是 Markdown（标记文档），优先按标题章节结构切分。
        if (isMarkdownDocument(documentEntity)) {
            // 返回 Markdown（标记文档）结构化切片。
            return splitMarkdownDocumentToChunkEntities(documentEntity, parseResult, splitter);
        }
        // 非 Markdown（标记文档）则按整文使用 LangChain4j（LangChain 的 Java 封装）切分器。
        return splitPlainDocumentToChunkEntities(documentEntity, parseResult, splitter);
    }

    /**
     * splitMarkdownDocumentToChunkEntities（切分 Markdown 文档为切片实体）用于先按标题拆章节，再在章节内使用 LangChain4j 切分器。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     * @param splitter LangChain4j DocumentSplitter（文档切分器）
     * @return 知识切片实体列表
     */
    private List<KnowledgeChunkEntity> splitMarkdownDocumentToChunkEntities(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult, DocumentSplitter splitter) {
        // 读取 LangChain4j Document（文档）。
        Document document = parseResult.getDocument();
        // 按 Markdown（标记文档）标题解析章节。
        List<MarkdownSection> sections = markdownSectionParser.parse(document.text());
        // 如果章节解析为空，则回退整文 LangChain4j（LangChain 的 Java 封装）切片。
        if (sections.isEmpty()) {
            // 返回整文切片结果。
            return splitPlainDocumentToChunkEntities(documentEntity, parseResult, splitter);
        }
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 创建结果列表。
        java.util.ArrayList<KnowledgeChunkEntity> chunks = new java.util.ArrayList<>();
        // 遍历章节。
        for (MarkdownSection section : sections) {
            // 把章节内容包装成 LangChain4j Document（文档）。
            Document sectionDocument = Document.from(section.getContent());
            // 在章节内部继续使用 LangChain4j DocumentSplitter（文档切分器）。
            List<TextSegment> segments = splitter.split(sectionDocument);
            // 遍历章节内切片。
            for (TextSegment segment : segments) {
                // 添加切片实体。
                chunks.add(toChunkEntity(documentEntity, parseResult, segment, chunks.size(), section.getTitlePath(), section.getSectionIndex(), "MARKDOWN_SECTION", now));
            }
        }
        // 返回切片列表。
        return chunks;
    }

    /**
     * splitPlainDocumentToChunkEntities（切分普通文档为切片实体）用于处理 TXT（纯文本）等没有标题结构的文档。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     * @param splitter LangChain4j DocumentSplitter（文档切分器）
     * @return 知识切片实体列表
     */
    private List<KnowledgeChunkEntity> splitPlainDocumentToChunkEntities(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult, DocumentSplitter splitter) {
        // 使用 LangChain4j DocumentSplitter（文档切分器）处理整篇文档。
        List<TextSegment> segments = splitter.split(parseResult.getDocument());
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 遍历文本片段并转换为实体。
        return java.util.stream.IntStream.range(0, segments.size())
                // 转换每个序号对应的切片。
                .mapToObj(index -> toChunkEntity(documentEntity, parseResult, segments.get(index), index, null, null, "PLAIN_DOCUMENT", now))
                // 收集为列表。
                .toList();
    }

    /**
     * toChunkEntity（转切片实体）用于转换单个 LangChain4j TextSegment（文本片段）。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     * @param segment 文本片段
     * @param index 切片序号
     * @param titlePath 标题路径
     * @param sectionIndex Markdown（标记文档）章节序号
     * @param splitMode 切片模式
     * @param now 当前时间
     * @return 知识切片实体
     */
    private KnowledgeChunkEntity toChunkEntity(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult, TextSegment segment, int index, String titlePath, Integer sectionIndex, String splitMode, LocalDateTime now) {
        // 读取切片正文。
        String content = segment.text();
        // 构造切片实体。
        return KnowledgeChunkEntity.builder()
                // 设置数据库主键。
                .id(newId("knowledge_chunk_pk"))
                // 设置对外切片编号。
                .chunkId(newId("knowledge_chunk"))
                // 设置知识库编号。
                .knowledgeBaseId(documentEntity.getKnowledgeBaseId())
                // 设置文档编号。
                .documentId(documentEntity.getDocumentId())
                // 设置切片序号。
                .chunkIndex(index)
                // 设置标题路径。
                .titlePath(blankToNull(titlePath))
                // 设置切片正文。
                .content(content)
                // 设置内容哈希。
                .contentHash(sha256(content))
                // 设置字符数。
                .charCount(content.length())
                // 设置估算 Token（词元）数量。
                .tokenCount(estimateTokenCount(content))
                // 设置切片元数据 JSON。
                .metadataJson(buildChunkMetadataJson(documentEntity, parseResult, index, titlePath, sectionIndex, splitMode))
                // 默认启用切片。
                .enabled(1)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
    }

    /**
     * buildChunkMetadataJson（构建切片元数据 JSON）用于保存切片来源和策略信息。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     * @param chunkIndex 切片序号
     * @param titlePath 标题路径
     * @param sectionIndex Markdown（标记文档）章节序号
     * @param splitMode 切片模式
     * @return 元数据 JSON
     */
    private String buildChunkMetadataJson(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult, int chunkIndex, String titlePath, Integer sectionIndex, String splitMode) {
        try {
            // 使用可变 Map（映射）构造结构化元数据，避免 Map.of（不可变映射）不允许空值。
            Map<String, Object> metadata = new java.util.LinkedHashMap<>();
            // 保存来源文件名。
            metadata.put("originalFilename", documentEntity.getOriginalFilename());
            // 保存来源类型。
            metadata.put("sourceType", documentEntity.getSourceType());
            // 保存切片策略。
            metadata.put("chunkStrategy", documentEntity.getChunkStrategy());
            // 保存切片序号。
            metadata.put("chunkIndex", chunkIndex);
            // 保存标题路径。
            metadata.put("titlePath", titlePath);
            // 保存章节序号。
            metadata.put("sectionIndex", sectionIndex);
            // 保存切片模式。
            metadata.put("splitMode", splitMode);
            // 保存解析器名称。
            metadata.put("parser", parseResult.getParserName());
            // 保存文件扩展名。
            metadata.put("fileExtension", parseResult.getFileExtension());
            // 保存抽取文本长度。
            metadata.put("extractedTextLength", parseResult.getExtractedTextLength());
            // 保存页数。
            metadata.put("pageCount", parseResult.getPageCount());
            // 保存解析警告。
            metadata.put("parserWarnings", parseResult.getParserWarnings());
            // 保存切分器来源。
            metadata.put("splitter", "LangChain4j DocumentSplitter");
            // 序列化为 JSON（对象表示格式）字符串。
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            // 元数据序列化失败时抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识切片元数据构建失败", exception);
        }
    }

    /**
     * buildChunkMetadataEntities（构建切片元数据实体）用于同步写入 cf_knowledge_chunk_metadata（知识切片元数据表）。
     *
     * @param chunks 知识切片实体列表
     * @return 切片元数据实体列表
     */
    private List<KnowledgeChunkMetadataEntity> buildChunkMetadataEntities(List<KnowledgeChunkEntity> chunks) {
        // 创建元数据结果列表。
        java.util.ArrayList<KnowledgeChunkMetadataEntity> metadataList = new java.util.ArrayList<>();
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 遍历切片实体。
        for (KnowledgeChunkEntity chunk : chunks) {
            // 解析切片 metadataJson（元数据 JSON）。
            JsonNode metadataJson = parseChunkMetadataJson(chunk.getMetadataJson());
            // 添加标题路径。
            addChunkMetadata(metadataList, chunk, "title_path", chunk.getTitlePath(), "STRING", "PARSER", now);
            // 添加切片序号。
            addChunkMetadata(metadataList, chunk, "chunk_index", String.valueOf(chunk.getChunkIndex()), "NUMBER", "PARSER", now);
            // 添加章节序号。
            addChunkMetadata(metadataList, chunk, "section_index", textValue(metadataJson, "sectionIndex"), "NUMBER", "PARSER", now);
            // 添加切片模式。
            addChunkMetadata(metadataList, chunk, "split_mode", textValue(metadataJson, "splitMode"), "STRING", "PARSER", now);
            // 添加切片策略。
            addChunkMetadata(metadataList, chunk, "chunk_strategy", textValue(metadataJson, "chunkStrategy"), "STRING", "PARSER", now);
            // 添加原始文件名。
            addChunkMetadata(metadataList, chunk, "original_filename", textValue(metadataJson, "originalFilename"), "STRING", "SYSTEM", now);
            // 添加字符数。
            addChunkMetadata(metadataList, chunk, "char_count", String.valueOf(chunk.getCharCount()), "NUMBER", "PARSER", now);
            // 添加估算 Token（词元）数量。
            addChunkMetadata(metadataList, chunk, "token_count", String.valueOf(chunk.getTokenCount()), "NUMBER", "PARSER", now);
            // 添加内容哈希。
            addChunkMetadata(metadataList, chunk, "content_hash", chunk.getContentHash(), "STRING", "PARSER", now);
            // 添加解析器名称。
            addChunkMetadata(metadataList, chunk, "parser", textValue(metadataJson, "parser"), "STRING", "PARSER", now);
            // 添加文件扩展名。
            addChunkMetadata(metadataList, chunk, "file_extension", textValue(metadataJson, "fileExtension"), "STRING", "SYSTEM", now);
            // 添加页数。
            addChunkMetadata(metadataList, chunk, "page_count", textValue(metadataJson, "pageCount"), "NUMBER", "PARSER", now);
        }
        // 返回元数据列表。
        return metadataList;
    }

    /**
     * saveParseMetadata（保存解析元数据）用于在文档处理成功后追加文档级解析结果。
     *
     * @param documentEntity 知识文档实体
     * @param parseResult 文档解析结果
     */
    private void saveParseMetadata(KnowledgeDocumentEntity documentEntity, DocumentParseResult parseResult) {
        // 创建元数据列表。
        java.util.ArrayList<com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity> metadataList = new java.util.ArrayList<>();
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 添加已解析标记。
        addDocumentProcessMetadata(metadataList, documentEntity, "parsed", "true", "BOOLEAN", "PARSER", now);
        // 添加解析器名称。
        addDocumentProcessMetadata(metadataList, documentEntity, "parser", parseResult.getParserName(), "STRING", "PARSER", now);
        // 添加文件扩展名。
        addDocumentProcessMetadata(metadataList, documentEntity, "file_extension", parseResult.getFileExtension(), "STRING", "SYSTEM", now);
        // 添加抽取文本长度。
        addDocumentProcessMetadata(metadataList, documentEntity, "extracted_text_length", String.valueOf(parseResult.getExtractedTextLength()), "NUMBER", "PARSER", now);
        // 添加页数。
        addDocumentProcessMetadata(metadataList, documentEntity, "page_count", parseResult.getPageCount() == null ? null : String.valueOf(parseResult.getPageCount()), "NUMBER", "PARSER", now);
        // 添加解析警告。
        addDocumentProcessMetadata(metadataList, documentEntity, "parser_warnings", parseResult.getParserWarnings(), "STRING", "PARSER", now);
        // 按键保存或更新解析元数据，避免重复键冲突。
        for (com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity metadata : metadataList) {
            // 保存或更新单条元数据。
            requiredKnowledgeDocumentMetadataRepository().upsertByDocumentIdAndKey(metadata);
        }
    }

    /**
     * addDocumentProcessMetadata（添加文档处理元数据）用于创建文档级解析键值元数据。
     *
     * @param metadataList 元数据列表
     * @param entity 知识文档实体
     * @param key 元数据键
     * @param value 元数据值
     * @param valueType 值类型
     * @param sourceType 来源类型
     * @param now 当前时间
     */
    private void addDocumentProcessMetadata(List<com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity> metadataList, KnowledgeDocumentEntity entity, String key, String value, String valueType, String sourceType, LocalDateTime now) {
        // 如果元数据值为空，则不写入。
        if (value == null || value.isBlank() || "null".equals(value)) {
            // 直接返回。
            return;
        }
        // 添加文档元数据实体。
        metadataList.add(com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeDocumentMetadataEntity.builder()
                // 设置数据库主键。
                .id(newId("knowledge_document_metadata_pk"))
                // 设置元数据编号。
                .metadataId(newId("knowledge_document_metadata"))
                // 设置知识库编号。
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                // 设置文档编号。
                .documentId(entity.getDocumentId())
                // 设置元数据键。
                .metadataKey(key)
                // 设置元数据值。
                .metadataValue(value)
                // 设置值类型。
                .valueType(valueType)
                // 设置来源类型。
                .sourceType(sourceType)
                // 当前不设置置信度。
                .confidence(null)
                // 默认启用。
                .enabled(1)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除。
                .deleted(0)
                // 构建实体。
                .build());
    }

    /**
     * addChunkMetadata（添加切片元数据）用于创建单条切片键值元数据。
     *
     * @param metadataList 元数据列表
     * @param chunk 知识切片实体
     * @param key 元数据键
     * @param value 元数据值
     * @param valueType 值类型
     * @param sourceType 来源类型
     * @param now 当前时间
     */
    private void addChunkMetadata(List<KnowledgeChunkMetadataEntity> metadataList, KnowledgeChunkEntity chunk, String key, String value, String valueType, String sourceType, LocalDateTime now) {
        // 如果元数据值为空，则不写入。
        if (value == null || value.isBlank() || "null".equals(value)) {
            // 直接返回。
            return;
        }
        // 添加切片元数据实体。
        metadataList.add(KnowledgeChunkMetadataEntity.builder()
                // 设置数据库主键。
                .id(newId("knowledge_chunk_metadata_pk"))
                // 设置对外元数据编号。
                .metadataId(newId("knowledge_chunk_metadata"))
                // 设置知识库编号。
                .knowledgeBaseId(chunk.getKnowledgeBaseId())
                // 设置文档编号。
                .documentId(chunk.getDocumentId())
                // 设置切片编号。
                .chunkId(chunk.getChunkId())
                // 设置元数据键。
                .metadataKey(key)
                // 设置元数据值。
                .metadataValue(value)
                // 设置值类型。
                .valueType(valueType)
                // 设置来源类型。
                .sourceType(sourceType)
                // 当前阶段不设置置信度。
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
     * parseChunkMetadataJson（解析切片元数据 JSON）用于从切片 JSONB（JSON 二进制类型）中读取补充字段。
     *
     * @param metadataJson 元数据 JSON 字符串
     * @return JsonNode（JSON 节点）
     */
    private JsonNode parseChunkMetadataJson(String metadataJson) {
        try {
            // 如果元数据为空，则返回空对象。
            if (metadataJson == null || metadataJson.isBlank()) {
                // 返回空 JSON 对象。
                return objectMapper.createObjectNode();
            }
            // 解析 JSON（对象表示格式）。
            return objectMapper.readTree(metadataJson);
        } catch (JsonProcessingException exception) {
            // 元数据解析失败时返回空对象，避免影响主切片链路。
            return objectMapper.createObjectNode();
        }
    }

    /**
     * textValue（文本值）用于安全读取 JSON（对象表示格式）字段。
     *
     * @param node JSON 节点
     * @param fieldName 字段名
     * @return 文本值
     */
    private String textValue(JsonNode node, String fieldName) {
        // 如果节点为空或字段缺失，则返回 null。
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            // 返回空引用。
            return null;
        }
        // 返回字段文本值。
        return node.get(fieldName).asText();
    }

    /**
     * requiredDocument（获取必需文档）用于校验文档存在。
     *
     * @param documentId 知识文档编号
     * @return 知识文档实体
     */
    private KnowledgeDocumentEntity requiredDocument(String documentId) {
        // 查询文档实体，不存在时抛出数据不存在异常。
        return requiredKnowledgeDocumentRepository().findByDocumentId(documentId)
                // 文档不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识文档，documentId：" + documentId));
    }

    /**
     * isMarkdownDocument（是否 Markdown 文档）用于判断是否启用结构化 Markdown（标记文档）章节切片。
     *
     * @param documentEntity 知识文档实体
     * @return 是否 Markdown（标记文档）
     */
    private boolean isMarkdownDocument(KnowledgeDocumentEntity documentEntity) {
        // 读取原始文件名。
        String originalFilename = documentEntity.getOriginalFilename();
        // 如果原始文件名以 .md 或 .markdown 结尾，则视为 Markdown（标记文档）。
        if (originalFilename != null && (originalFilename.toLowerCase().endsWith(".md") || originalFilename.toLowerCase().endsWith(".markdown"))) {
            // 返回是 Markdown。
            return true;
        }
        // 读取文件路径。
        String filePath = documentEntity.getFilePath();
        // 如果文件路径以 .md 或 .markdown 结尾，也视为 Markdown（标记文档）。
        return filePath != null && (filePath.toLowerCase().endsWith(".md") || filePath.toLowerCase().endsWith(".markdown"));
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档处理接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentStatusUseCase（获取必需知识文档状态用例）用于处理未启用数据库的场景。
     *
     * @return 知识文档状态用例
     */
    private KnowledgeDocumentStatusUseCase requiredKnowledgeDocumentStatusUseCase() {
        // 从 Spring（Spring 框架）容器中获取状态用例。
        KnowledgeDocumentStatusUseCase useCase = knowledgeDocumentStatusUseCaseProvider.getIfAvailable();
        // 如果状态用例不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (useCase == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档状态更新需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用状态用例。
        return useCase;
    }

    /**
     * requiredKnowledgeChunkRepository（获取必需知识切片仓储）用于处理未启用数据库的场景。
     *
     * @return 知识切片仓储
     */
    private KnowledgeChunkRepository requiredKnowledgeChunkRepository() {
        // 从 Spring（Spring 框架）容器中获取知识切片仓储。
        KnowledgeChunkRepository repository = knowledgeChunkRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识切片接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * requiredKnowledgeDocumentMetadataRepository（获取必需文档元数据仓储）用于保存解析阶段元数据。
     *
     * @return 文档元数据仓储
     */
    private KnowledgeDocumentMetadataRepository requiredKnowledgeDocumentMetadataRepository() {
        // 从 Spring（Spring 框架）容器中获取文档元数据仓储。
        KnowledgeDocumentMetadataRepository repository = knowledgeDocumentMetadataRepositoryProvider.getIfAvailable();
        // 如果仓储不存在，说明没有启用 db（数据库）Profile（配置环境）。
        if (repository == null) {
            // 抛出持久化未启用异常。
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识文档解析元数据需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * estimateTokenCount（估算 Token 数）用于在未调用真实 Tokenizer（分词器）时提供粗略成本参考。
     *
     * @param text 文本
     * @return 估算 Token（词元）数量
     */
    private int estimateTokenCount(String text) {
        // 如果文本为空，则返回 0。
        if (text == null || text.isBlank()) {
            // 返回 0。
            return 0;
        }
        // 中文场景粗略按 2 个字符约等于 1 个 Token（词元）估算，后续可替换为模型 Tokenizer（分词器）。
        return Math.max(1, (int) Math.ceil(text.length() / 2.0));
    }

    /**
     * sha256（计算 SHA-256）用于计算切片内容哈希。
     *
     * @param content 切片正文
     * @return 十六进制哈希字符串
     */
    private String sha256(String content) {
        try {
            // 创建 SHA-256（安全哈希算法）摘要器。
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            // 计算 UTF-8（统一字符编码）字节哈希。
            byte[] digest = messageDigest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // 转换为十六进制字符串。
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            // 理论上 JVM（Java 虚拟机）都支持 SHA-256，这里仍做兜底。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "SHA-256 哈希算法不可用", exception);
        }
    }

    /**
     * blankToNull（空白转空引用）用于避免数据库保存无意义空字符串。
     *
     * @param value 原始字符串
     * @return 空白时返回 null，否则返回原值
     */
    private String blankToNull(String value) {
        // 如果字符串为空或空白，则返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 返回原值。
        return value;
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
