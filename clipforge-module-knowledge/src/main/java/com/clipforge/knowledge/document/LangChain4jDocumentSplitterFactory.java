package com.clipforge.knowledge.document;

import com.clipforge.knowledge.document.ChunkConfig;
import com.clipforge.knowledge.document.DocumentParseResult;
import com.clipforge.knowledge.document.MarkdownSection;
import com.clipforge.knowledge.document.MarkdownSectionParser;
import com.clipforge.knowledge.document.MarkdownTitlePathResolver;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeChunkStrategy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentByCharacterSplitter;
import dev.langchain4j.data.document.splitter.DocumentByLineSplitter;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LangChain4jDocumentSplitterFactory（LangChain4j 文档切分器工厂）。
 *
 * <p>该工厂根据文档的 chunkStrategy（切片策略）和 chunkConfigJson（切片配置 JSON）创建 LangChain4j（LangChain 的 Java 封装）的 DocumentSplitter（文档切分器）。
 */
@Component
@RequiredArgsConstructor
public class LangChain4jDocumentSplitterFactory {

    /** DEFAULT_MAX_SEGMENT_SIZE（默认最大切片大小）用于控制每个切片的最大字符数。 */
    private static final int DEFAULT_MAX_SEGMENT_SIZE = 1000;

    /** DEFAULT_MAX_OVERLAP_SIZE（默认最大重叠大小）用于控制相邻切片的重叠字符数。 */
    private static final int DEFAULT_MAX_OVERLAP_SIZE = 150;

    /** objectMapper（对象映射器）用于解析 chunkConfigJson（切片配置 JSON）。 */
    private final ObjectMapper objectMapper;

    /**
     * createSplitter（创建切分器）用于根据策略和配置返回 LangChain4j DocumentSplitter（文档切分器）。
     *
     * @param chunkStrategy 切片策略
     * @param chunkConfigJson 切片配置 JSON
     * @return 文档切分器
     */
    public DocumentSplitter createSplitter(String chunkStrategy, String chunkConfigJson) {
        // 解析切片配置。
        ChunkConfig config = parseConfig(chunkConfigJson);
        // 读取最大切片大小。
        int maxSegmentSize = defaultNumber(firstNonNull(config.getMaxSegmentSize(), config.getChunkSize()), DEFAULT_MAX_SEGMENT_SIZE);
        // 读取最大重叠大小。
        int maxOverlapSize = defaultNumber(firstNonNull(config.getMaxOverlapSize(), config.getOverlap()), DEFAULT_MAX_OVERLAP_SIZE);
        // 校验切片参数。
        validateSize(maxSegmentSize, maxOverlapSize);
        // 标准化切片策略。
        KnowledgeChunkStrategy normalizedStrategy = normalizeStrategy(chunkStrategy);
        // 根据策略创建 LangChain4j（LangChain 的 Java 封装）切分器。
        return switch (normalizedStrategy) {
            // RECURSIVE（递归切片）是默认推荐策略。
            case RECURSIVE, MARKDOWN_SECTION -> DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
            // PARAGRAPH（段落切片）优先按段落边界切分。
            case PARAGRAPH -> new DocumentByParagraphSplitter(maxSegmentSize, maxOverlapSize);
            // LINE（行切片）优先按换行切分。
            case LINE -> new DocumentByLineSplitter(maxSegmentSize, maxOverlapSize);
            // SENTENCE（句子切片）优先按句子切分。
            case SENTENCE -> new DocumentBySentenceSplitter(maxSegmentSize, maxOverlapSize);
            // CHARACTER（字符切片）按字符长度切分，稳定但语义边界较弱。
            case CHARACTER -> new DocumentByCharacterSplitter(maxSegmentSize, maxOverlapSize);
            // REGEX（正则切片）按用户提供的正则表达式切分。
            case REGEX -> new DocumentByRegexSplitter(requireRegex(config), "\n", maxSegmentSize, maxOverlapSize);
        };
    }

    /**
     * parseConfig（解析配置）用于把 JSON（对象表示格式）字符串转换为切片配置对象。
     *
     * @param chunkConfigJson 切片配置 JSON
     * @return 切片配置
     */
    private ChunkConfig parseConfig(String chunkConfigJson) {
        // 如果配置为空，则返回默认空配置。
        if (chunkConfigJson == null || chunkConfigJson.isBlank()) {
            // 返回空配置。
            return ChunkConfig.builder().build();
        }
        try {
            // 解析 JSON（对象表示格式）为配置对象。
            return objectMapper.readValue(chunkConfigJson, ChunkConfig.class);
        } catch (JsonProcessingException exception) {
            // 配置解析失败时返回明确参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "切片配置 JSON 不合法", exception);
        }
    }

    /**
     * normalizeStrategy（标准化策略）用于处理空策略和大小写差异。
     *
     * @param chunkStrategy 原始切片策略
     * @return 标准化策略
     */
    private KnowledgeChunkStrategy normalizeStrategy(String chunkStrategy) {
        // 把原始字符串解析成切片策略枚举。
        KnowledgeChunkStrategy strategy = KnowledgeChunkStrategy.fromValue(chunkStrategy);
        // 如果策略无法识别，则返回明确错误。
        if (strategy == null) {
            // 抛出参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的切片策略：" + chunkStrategy);
        }
        // 返回切片策略枚举。
        return strategy;
    }

    /**
     * defaultNumber（默认数字）用于在配置缺失时使用默认值。
     *
     * @param value 配置值
     * @param defaultValue 默认值
     * @return 最终数值
     */
    private int defaultNumber(Integer value, int defaultValue) {
        // 如果配置为空，则返回默认值。
        if (value == null) {
            // 返回默认值。
            return defaultValue;
        }
        // 返回配置值。
        return value;
    }

    /**
     * firstNonNull（第一个非空值）用于兼容新旧切片配置字段。
     *
     * @param first 第一个值
     * @param second 第二个值
     * @return 第一个非空值，两个都为空则返回 null
     */
    private Integer firstNonNull(Integer first, Integer second) {
        // 如果第一个值非空，则优先返回。
        if (first != null) {
            // 返回第一个值。
            return first;
        }
        // 返回第二个值。
        return second;
    }

    /**
     * validateSize（校验大小）用于确保切片大小和重叠大小合理。
     *
     * @param maxSegmentSize 最大切片大小
     * @param maxOverlapSize 最大重叠大小
     */
    private void validateSize(int maxSegmentSize, int maxOverlapSize) {
        // 最大切片大小必须大于 0。
        if (maxSegmentSize <= 0) {
            // 抛出参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "maxSegmentSize 必须大于 0");
        }
        // 最大重叠大小不能小于 0。
        if (maxOverlapSize < 0) {
            // 抛出参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "maxOverlapSize 不能小于 0");
        }
        // 最大重叠大小必须小于最大切片大小。
        if (maxOverlapSize >= maxSegmentSize) {
            // 抛出参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "maxOverlapSize 必须小于 maxSegmentSize");
        }
    }

    /**
     * requireRegex（获取必需正则）用于 REGEX（正则）切片策略。
     *
     * @param config 切片配置
     * @return 正则表达式
     */
    private String requireRegex(ChunkConfig config) {
        // 如果正则为空，则拒绝请求。
        if (config.getRegex() == null || config.getRegex().isBlank()) {
            // 抛出参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "REGEX 切片策略必须配置 regex");
        }
        // 返回去掉首尾空白后的正则。
        return config.getRegex().strip();
    }
}
