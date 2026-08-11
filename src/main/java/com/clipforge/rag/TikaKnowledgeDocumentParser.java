package com.clipforge.rag;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.KnowledgeDocumentEntity;
import com.clipforge.rag.ocr.NoOpOcrEngineClient;
import com.clipforge.rag.ocr.OcrEngineClient;
import com.clipforge.rag.ocr.OcrEngineRequest;
import com.clipforge.rag.ocr.OcrEngineResponse;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * TikaKnowledgeDocumentParser（Tika 知识文档解析器）。
 *
 * <p>该实现对 Markdown（标记文档）和 TXT（纯文本）继续使用 LangChain4j TextDocumentParser（文本解析器），对 PDF（便携式文档格式）和 Word（文字处理文档）使用 Apache Tika（通用文档解析工具）抽取文本。
 */
@Component
public class TikaKnowledgeDocumentParser implements KnowledgeDocumentParser {

    /** TEXT_PARSER_NAME（文本解析器名称）用于写入文档和切片元数据。 */
    private static final String TEXT_PARSER_NAME = "LangChain4j TextDocumentParser";

    /** TIKA_PARSER_NAME（Tika 解析器名称）用于写入文档和切片元数据。 */
    private static final String TIKA_PARSER_NAME = "Apache Tika AutoDetectParser";

    /** OCR_PARSER_NAME（OCR 解析器名称）用于标记扫描件走过 OCR 入口。 */
    private static final String OCR_PARSER_NAME = "OCR Fallback";

    /** ocrEngineClient（OCR 客户端）用于屏蔽 OCR（光学字符识别）供应商差异。 */
    private final OcrEngineClient ocrEngineClient;

    /**
     * TikaKnowledgeDocumentParser（Tika 知识文档解析器）默认构造器。
     *
     * <p>该构造器用于测试场景和非 Spring 直接创建场景，默认挂载空实现 OCR 客户端。
     */
    public TikaKnowledgeDocumentParser() {
        // 默认使用空实现 OCR 客户端，保证扫描件会得到清晰的“未启用”提示。
        this(new NoOpOcrEngineClient());
    }

    /**
     * TikaKnowledgeDocumentParser（Tika 知识文档解析器）注入构造器。
     *
     * @param ocrEngineClientProvider OCR 客户端提供器
     */
    @Autowired
    public TikaKnowledgeDocumentParser(ObjectProvider<OcrEngineClient> ocrEngineClientProvider) {
        // 如果 Spring 容器里没有真实 OCR 客户端，则回退为空实现。
        this.ocrEngineClient = ocrEngineClientProvider.getIfAvailable(NoOpOcrEngineClient::new);
    }

    /**
     * TikaKnowledgeDocumentParser（Tika 知识文档解析器）测试构造器。
     *
     * @param ocrEngineClient OCR 客户端
     */
    public TikaKnowledgeDocumentParser(OcrEngineClient ocrEngineClient) {
        // 如果调用方传入空引用，则回退为空实现。
        this.ocrEngineClient = ocrEngineClient == null ? new NoOpOcrEngineClient() : ocrEngineClient;
    }

    /**
     * parse（解析）用于按文件类型选择具体解析器。
     *
     * @param documentEntity 知识文档实体
     * @return 文档解析结果
     */
    @Override
    public DocumentParseResult parse(KnowledgeDocumentEntity documentEntity) {
        // 解析本地文件路径。
        Path filePath = resolveFilePath(documentEntity);
        // 提取文件扩展名。
        String extension = resolveExtension(documentEntity);
        // 根据扩展名选择解析方式。
        if (isTextLike(extension)) {
            // 返回文本类文档解析结果。
            return parseTextDocument(filePath, extension);
        }
        // 根据扩展名判断是否为 Tika（通用文档解析工具）支持的办公文档。
        if (isTikaDocument(extension)) {
            // 返回 Tika（通用文档解析工具）解析结果。
            return parseTikaDocument(filePath, extension);
        }
        // 图片类文件优先交给 OCR（光学字符识别）入口处理，后续再接真实引擎。
        if (isOcrImageLikeDocument(extension)) {
            // 直接走 OCR 入口。
            return parseWithOcr(filePath, documentEntity, extension, "图片型知识文档");
        }
        // 不支持的扩展名直接拒绝，避免误把二进制文件当文本切片。
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的知识文档类型：" + extension);
    }

    /**
     * parseTextDocument（解析文本类文档）用于处理 Markdown（标记文档）和 TXT（纯文本）。
     *
     * @param filePath 文件路径
     * @param extension 文件扩展名
     * @return 文档解析结果
     */
    private DocumentParseResult parseTextDocument(Path filePath, String extension) {
        // 创建 LangChain4j TextDocumentParser（文本解析器）。
        DocumentParser parser = new TextDocumentParser(StandardCharsets.UTF_8);
        // 打开文件输入流。
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            // 解析为 LangChain4j Document（文档）。
            Document document = parser.parse(inputStream);
            // 返回统一解析结果。
            return buildResult(document, TEXT_PARSER_NAME, extension, null, null);
        } catch (IOException exception) {
            // 读取失败时抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档读取失败：" + filePath, exception);
        }
    }

    /**
     * parseTikaDocument（解析 Tika 文档）用于处理 PDF（便携式文档格式）和 Word（文字处理文档）。
     *
     * @param filePath 文件路径
     * @param extension 文件扩展名
     * @return 文档解析结果
     */
    private DocumentParseResult parseTikaDocument(Path filePath, String extension) {
        // 创建 Tika（通用文档解析工具）自动检测解析器。
        AutoDetectParser parser = new AutoDetectParser();
        // 创建元数据容器，用于接收页数等解析信息。
        Metadata metadata = new Metadata();
        // 创建解析上下文。
        ParseContext parseContext = new ParseContext();
        // 创建不限制长度的正文处理器，避免长文被 Tika 默认截断。
        BodyContentHandler handler = new BodyContentHandler(-1);
        // 打开文件输入流。
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            // 执行 Tika（通用文档解析工具）解析。
            parser.parse(inputStream, handler, metadata, parseContext);
            // 读取抽取后的正文。
            String text = normalizeExtractedText(handler.toString());
            // 如果正文为空，说明可能是扫描版 PDF 或不可解析文档。
            if (text.isBlank()) {
                // 扫描版 PDF 或无文本层文档切换到 OCR（光学字符识别）入口。
                return parseWithOcr(filePath, null, extension, "Tika 抽取文本为空");
            }
            // 构造 LangChain4j Document（文档）。
            Document document = Document.from(text);
            // 读取页数。
            Integer pageCount = readPageCount(metadata);
            // 返回统一解析结果。
            return buildResult(document, TIKA_PARSER_NAME, extension, pageCount, null);
        } catch (IOException | TikaException | SAXException exception) {
            // 解析异常统一包装为知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "PDF / Word 知识文档解析失败：" + filePath, exception);
        }
    }

    /**
     * parseWithOcr（通过 OCR 解析）用于扫描版 PDF（便携式文档格式）和图片型文件。
     *
     * @param filePath 文件路径
     * @param documentEntity 知识文档实体
     * @param extension 文件扩展名
     * @param reason 触发 OCR 的原因
     * @return 文档解析结果
     */
    private DocumentParseResult parseWithOcr(Path filePath, KnowledgeDocumentEntity documentEntity, String extension, String reason) {
        // 构造 OCR（光学字符识别）请求。
        OcrEngineRequest request = OcrEngineRequest.builder()
                // 设置文档编号。
                .documentId(documentEntity == null ? null : documentEntity.getDocumentId())
                // 设置原始文件名。
                .originalFilename(documentEntity == null ? null : documentEntity.getOriginalFilename())
                // 设置文件路径。
                .filePath(filePath.toString())
                // 设置文件扩展名。
                .fileExtension(extension)
                // 设置来源类型。
                .sourceType(isTikaDocument(extension) ? "PDF" : "IMAGE")
                // 设置触发原因。
                .reason(reason)
                // 构建请求。
                .build();
        // 调用 OCR 客户端。
        OcrEngineResponse response = ocrEngineClient.recognize(request);
        // 规范化 OCR 输出文本。
        String text = normalizeExtractedText(response == null ? null : response.getText());
        // 如果真实 OCR 也没有返回文本，则直接报错。
        if (text.isBlank()) {
            // 抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "OCR 识别后文本为空：" + filePath);
        }
        // 构造 LangChain4j Document（文档）。
        Document document = Document.from(text);
        // 组装 OCR 解析警告。
        String warnings = buildOcrWarnings(reason, response);
        // 返回统一解析结果。
        return buildResult(document, response == null ? OCR_PARSER_NAME : safeText(response.getEngineName(), OCR_PARSER_NAME), extension, null, warnings);
    }

    /**
     * buildResult（构建结果）用于生成统一解析结果对象。
     *
     * @param document LangChain4j 文档
     * @param parserName 解析器名称
     * @param extension 文件扩展名
     * @param pageCount 页数
     * @param parserWarnings 解析警告
     * @return 文档解析结果
     */
    private DocumentParseResult buildResult(Document document, String parserName, String extension, Integer pageCount, String parserWarnings) {
        // 读取文档正文。
        String text = document == null ? "" : document.text();
        // 构建解析结果。
        return DocumentParseResult.builder()
                // 设置文档对象。
                .document(document)
                // 设置解析器名称。
                .parserName(parserName)
                // 设置扩展名。
                .fileExtension(extension)
                // 设置抽取文本长度。
                .extractedTextLength(text == null ? 0 : text.length())
                // 设置页数。
                .pageCount(pageCount)
                // 设置解析警告。
                .parserWarnings(parserWarnings)
                // 构建对象。
                .build();
    }

    /**
     * resolveFilePath（解析文件路径）用于校验知识文档文件存在。
     *
     * @param documentEntity 知识文档实体
     * @return 文件路径
     */
    private Path resolveFilePath(KnowledgeDocumentEntity documentEntity) {
        // 校验文件路径。
        if (documentEntity == null || documentEntity.getFilePath() == null || documentEntity.getFilePath().isBlank()) {
            // 抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档文件路径为空");
        }
        // 构造本地文件路径。
        Path filePath = Path.of(documentEntity.getFilePath());
        // 如果文件不存在，则抛出明确错误。
        if (!Files.exists(filePath)) {
            // 抛出知识文件错误。
            throw new BusinessException(ErrorCode.KNOWLEDGE_FILE_ERROR, "知识文档文件不存在：" + filePath);
        }
        // 返回文件路径。
        return filePath;
    }

    /**
     * resolveExtension（解析扩展名）用于从原始文件名或存储路径读取文件类型。
     *
     * @param documentEntity 知识文档实体
     * @return 小写扩展名
     */
    private String resolveExtension(KnowledgeDocumentEntity documentEntity) {
        // 优先从原始文件名读取扩展名。
        String extension = extractExtension(documentEntity.getOriginalFilename());
        // 如果原始文件名没有扩展名，则从文件路径读取。
        if (extension.isBlank()) {
            // 使用文件路径兜底。
            extension = extractExtension(documentEntity.getFilePath());
        }
        // 返回扩展名。
        return extension;
    }

    /**
     * extractExtension（提取扩展名）用于返回文件名最后一个点号后的后缀。
     *
     * @param filename 文件名
     * @return 小写扩展名
     */
    private String extractExtension(String filename) {
        // 如果文件名为空，则返回空字符串。
        if (filename == null || filename.isBlank()) {
            // 返回空字符串。
            return "";
        }
        // 找到最后一个点号。
        int index = filename.lastIndexOf('.');
        // 如果没有后缀，则返回空字符串。
        if (index < 0 || index == filename.length() - 1) {
            // 返回空字符串。
            return "";
        }
        // 返回小写后缀。
        return filename.substring(index).toLowerCase();
    }

    /**
     * isTextLike（是否文本类文档）用于识别 Markdown（标记文档）和 TXT（纯文本）。
     *
     * @param extension 文件扩展名
     * @return true 表示文本类文档
     */
    private boolean isTextLike(String extension) {
        // 判断是否为 Markdown（标记文档）或 TXT（纯文本）。
        return ".md".equals(extension) || ".markdown".equals(extension) || ".txt".equals(extension);
    }

    /**
     * isTikaDocument（是否 Tika 文档）用于识别 PDF（便携式文档格式）和 Word（文字处理文档）。
     *
     * @param extension 文件扩展名
     * @return true 表示交给 Tika（通用文档解析工具）处理
     */
    private boolean isTikaDocument(String extension) {
        // 判断是否为 PDF（便携式文档格式）、DOC（旧版 Word 文档）或 DOCX（新版 Word 文档）。
        return ".pdf".equals(extension) || ".doc".equals(extension) || ".docx".equals(extension);
    }

    /**
     * isOcrImageLikeDocument（是否图片型文档）用于识别可交给 OCR（光学字符识别）处理的文件。
     *
     * @param extension 文件扩展名
     * @return true 表示图片型文件
     */
    private boolean isOcrImageLikeDocument(String extension) {
        // 判断常见图片格式。
        return ".png".equals(extension)
                || ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".bmp".equals(extension)
                || ".gif".equals(extension)
                || ".tif".equals(extension)
                || ".tiff".equals(extension)
                || ".webp".equals(extension);
    }

    /**
     * normalizeExtractedText（规范化抽取文本）用于减少 Tika（通用文档解析工具）输出中的多余空白。
     *
     * @param text 原始抽取文本
     * @return 规范化文本
     */
    private String normalizeExtractedText(String text) {
        // 如果文本为空，则返回空字符串。
        if (text == null) {
            // 返回空字符串。
            return "";
        }
        // 统一换行并去掉首尾空白。
        return text.replace("\r\n", "\n").replace('\r', '\n').strip();
    }

    /**
     * readPageCount（读取页数）用于从 Tika 元数据中尽量提取页数。
     *
     * @param metadata Tika 元数据
     * @return 页数，无法读取时返回空引用
     */
    private Integer readPageCount(Metadata metadata) {
        // 常见页数字段候选。
        String[] keys = {"xmpTPg:NPages", "Page-Count", "meta:page-count", "slide-count"};
        // 遍历候选字段。
        for (String key : keys) {
            // 读取字段值。
            String value = metadata.get(key);
            // 如果字段有值，则尝试转数字。
            if (value != null && !value.isBlank()) {
                // 返回解析后的页数。
                return parseInteger(value);
            }
        }
        // 未找到页数时返回空引用。
        return null;
    }

    /**
     * parseInteger（解析整数）用于安全读取页数字段。
     *
     * @param value 原始字符串
     * @return 整数或空引用
     */
    private Integer parseInteger(String value) {
        try {
            // 转换为整数。
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException exception) {
            // 无法解析时返回空引用。
            return null;
        }
    }

    /**
     * buildOcrWarnings（构建 OCR 警告）用于记录触发 OCR 的原因和引擎提示。
     *
     * @param reason 触发原因
     * @param response OCR 响应
     * @return 警告文本
     */
    private String buildOcrWarnings(String reason, OcrEngineResponse response) {
        // 保存 OCR 识别过程中的说明信息。
        StringBuilder builder = new StringBuilder("OCR 入口已触发，原因：").append(reason);
        // 如果 OCR 响应里有警告，则追加。
        if (response != null && response.getWarnings() != null && !response.getWarnings().isBlank()) {
            // 追加引擎警告。
            builder.append("；").append(response.getWarnings().strip());
        }
        // 返回合并后的警告。
        return builder.toString();
    }

    /**
     * safeText（安全文本）用于在字符串为空时返回默认值。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 非空文本
     */
    private String safeText(String value, String defaultValue) {
        // 如果值为空，则使用默认值。
        if (value == null || value.isBlank()) {
            // 返回默认值。
            return defaultValue;
        }
        // 返回去空白后的文本。
        return value.strip();
    }
}
