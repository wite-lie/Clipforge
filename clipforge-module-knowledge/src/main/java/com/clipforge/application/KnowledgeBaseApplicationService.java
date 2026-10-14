package com.clipforge.application;

import com.clipforge.contract.knowledge.CreateKnowledgeBaseRequest;
import com.clipforge.contract.knowledge.KnowledgeBaseResponse;
import com.clipforge.knowledge.application.api.KnowledgeBaseUseCase;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeBaseStatus;
import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeChunkStrategy;
import com.clipforge.knowledge.infrastructure.persistence.repository.KnowledgeBaseRepository;
import com.clipforge.knowledge.query.KnowledgeBaseQueryCondition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * KnowledgeBaseApplicationService（知识库应用服务）。
 *
 * <p>该服务负责编排 KnowledgeBase（知识库）的创建、详情查询和列表查询，是 Phase 7.2（阶段 7.2）的 RAG（检索增强生成）入口服务。
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseApplicationService implements KnowledgeBaseUseCase {

    /** DEFAULT_EMBEDDING_DIMENSION（默认嵌入维度）必须和当前 pgvector（PostgreSQL 向量扩展）字段维度保持一致。 */
    private static final int DEFAULT_EMBEDDING_DIMENSION = 1024;

    /** knowledgeBaseRepositoryProvider（知识库仓储提供器）用于在 db（数据库）Profile（配置环境）启用时获取仓储。 */
    private final ObjectProvider<KnowledgeBaseRepository> knowledgeBaseRepositoryProvider;

    /** objectMapper（对象映射器）用于校验 JSON（对象表示格式）配置字符串是否合法。 */
    private final ObjectMapper objectMapper;

    /**
     * createKnowledgeBase（创建知识库）用于创建 RAG（检索增强生成）知识库容器。
     *
     * @param request 创建知识库请求
     * @return 知识库响应
     */
    public KnowledgeBaseResponse createKnowledgeBase(CreateKnowledgeBaseRequest request) {
        // 获取当前时间，用于填充创建时间和更新时间。
        LocalDateTime now = LocalDateTime.now();
        // 解析并校验嵌入维度，当前必须和数据库向量字段维度一致。
        Integer embeddingDimension = validateEmbeddingDimension(request.getEmbeddingDimension());
        // 校验切片配置 JSON（对象表示格式），合法时返回去掉首尾空白后的文本。
        String chunkConfigJson = validateOptionalJson(request.getChunkConfigJson(), "切片配置 JSON 不合法");
        // 校验检索配置 JSON（对象表示格式），合法时返回去掉首尾空白后的文本。
        String retrievalConfigJson = validateOptionalJson(request.getRetrievalConfigJson(), "检索配置 JSON 不合法");
        // 使用 Builder（建造者模式）组装知识库实体。
        KnowledgeBaseEntity entity = KnowledgeBaseEntity.builder()
                // 设置数据库内部主键。
                .id(newId("knowledge_base_pk"))
                // 设置对外知识库编号。
                .knowledgeBaseId(newId("knowledge_base"))
                // 设置用户编号，空白值转为空引用。
                .userId(blankToNull(request.getUserId()))
                // 设置知识库名称。
                .name(request.getName().strip())
                // 设置知识库描述。
                .description(blankToNull(request.getDescription()))
                // 设置知识库分类。
                .category(blankToNull(request.getCategory()))
                // 设置默认平台。
                .defaultPlatform(blankToNull(request.getDefaultPlatform()))
                // 设置嵌入供应商。
                .embeddingProvider(blankToNull(request.getEmbeddingProvider()))
                // 设置嵌入模型。
                .embeddingModel(blankToNull(request.getEmbeddingModel()))
                // 设置嵌入维度，当前固定为和 pgvector（PostgreSQL 向量扩展）字段匹配的 1024 维。
                .embeddingDimension(embeddingDimension)
                // 设置切片策略，未传时使用 RECURSIVE（递归切片）。
                .chunkStrategy(defaultText(request.getChunkStrategy(), KnowledgeChunkStrategy.defaultValue()))
                // 设置切片配置 JSON（对象表示格式）。
                .chunkConfigJson(chunkConfigJson)
                // 设置检索配置 JSON（对象表示格式）。
                .retrievalConfigJson(retrievalConfigJson)
                // 设置知识库状态。
                .status(KnowledgeBaseStatus.ACTIVE.value())
                // 设置启用状态。
                .enabled(1)
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记。
                .deleted(0)
                // 构建实体。
                .build();
        // 保存知识库实体。
        KnowledgeBaseEntity savedEntity = requiredKnowledgeBaseRepository().save(entity);
        // 转换为接口响应。
        return toKnowledgeBaseResponse(savedEntity);
    }

    /**
     * queryKnowledgeBase（查询知识库）用于按知识库编号读取详情。
     *
     * @param knowledgeBaseId 知识库编号
     * @return 知识库响应
     */
    public KnowledgeBaseResponse queryKnowledgeBase(String knowledgeBaseId) {
        // 查询知识库实体，不存在时抛出数据不存在异常。
        KnowledgeBaseEntity entity = requiredKnowledgeBaseRepository().findByKnowledgeBaseId(knowledgeBaseId)
                // 知识库不存在时抛出明确错误。
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到知识库，knowledgeBaseId：" + knowledgeBaseId));
        // 转换为接口响应。
        return toKnowledgeBaseResponse(entity);
    }

    /**
     * listKnowledgeBases（查询知识库列表）用于按可选条件筛选知识库。
     *
     * @param userId 用户编号
     * @param category 分类
     * @param defaultPlatform 默认平台
     * @param status 状态
     * @param enabled 是否启用
     * @return 知识库响应列表
     */
    public List<KnowledgeBaseResponse> listKnowledgeBases(String userId, String category, String defaultPlatform, String status, Integer enabled) {
        // 构造查询条件，统一把空白字符串转为空引用。
        KnowledgeBaseQueryCondition condition = KnowledgeBaseQueryCondition.builder()
                // 设置用户编号。
                .userId(blankToNull(userId))
                // 设置分类。
                .category(blankToNull(category))
                // 设置默认平台。
                .defaultPlatform(blankToNull(defaultPlatform))
                // 设置状态。
                .status(blankToNull(status))
                // 设置启用状态。
                .enabled(enabled)
                // 构建查询条件。
                .build();
        // 查询知识库实体列表并转换为响应列表。
        return requiredKnowledgeBaseRepository().listByCondition(condition).stream()
                // 转换单个知识库实体。
                .map(this::toKnowledgeBaseResponse)
                // 收集为列表。
                .toList();
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
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "知识库接口需要启用 db Profile（数据库配置环境）");
        }
        // 返回可用仓储。
        return repository;
    }

    /**
     * toKnowledgeBaseResponse（转知识库响应）用于把数据库实体转换为接口响应对象。
     *
     * @param entity 知识库实体
     * @return 知识库响应
     */
    private KnowledgeBaseResponse toKnowledgeBaseResponse(KnowledgeBaseEntity entity) {
        // 使用 Builder（建造者模式）组装知识库响应。
        return KnowledgeBaseResponse.builder()
                // 设置知识库编号。
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                // 设置用户编号。
                .userId(entity.getUserId())
                // 设置知识库名称。
                .name(entity.getName())
                // 设置知识库描述。
                .description(entity.getDescription())
                // 设置知识库分类。
                .category(entity.getCategory())
                // 设置默认平台。
                .defaultPlatform(entity.getDefaultPlatform())
                // 设置嵌入供应商。
                .embeddingProvider(entity.getEmbeddingProvider())
                // 设置嵌入模型。
                .embeddingModel(entity.getEmbeddingModel())
                // 设置嵌入维度。
                .embeddingDimension(entity.getEmbeddingDimension())
                // 设置切片策略。
                .chunkStrategy(entity.getChunkStrategy())
                // 设置切片配置 JSON。
                .chunkConfigJson(entity.getChunkConfigJson())
                // 设置检索配置 JSON。
                .retrievalConfigJson(entity.getRetrievalConfigJson())
                // 设置状态。
                .status(entity.getStatus())
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
     * validateEmbeddingDimension（校验嵌入维度）用于确保请求维度和当前向量表结构一致。
     *
     * @param embeddingDimension 请求传入维度
     * @return 可落库的维度
     */
    private Integer validateEmbeddingDimension(Integer embeddingDimension) {
        // 如果没有传维度，则使用当前表结构匹配的默认 1024 维。
        if (embeddingDimension == null) {
            // 返回默认维度。
            return DEFAULT_EMBEDDING_DIMENSION;
        }
        // 如果传入维度和当前 pgvector（PostgreSQL 向量扩展）字段不一致，则拒绝保存，避免后续向量入库失败。
        if (embeddingDimension != DEFAULT_EMBEDDING_DIMENSION) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前知识向量表固定为 1024 维，embeddingDimension 必须为 1024");
        }
        // 返回请求维度。
        return embeddingDimension;
    }

    /**
     * validateOptionalJson（校验可选 JSON）用于在写入 JSONB（JSON 二进制类型）字段前提前发现格式错误。
     *
     * @param json 原始 JSON（对象表示格式）字符串
     * @param errorMessage 错误提示
     * @return 空白时返回 null，合法时返回去掉首尾空白后的 JSON 字符串
     */
    private String validateOptionalJson(String json, String errorMessage) {
        // 如果字符串为空白，则返回 null，表示不写入该配置。
        if (json == null || json.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 去掉首尾空白，避免数据库保存多余空格。
        String trimmedJson = json.strip();
        try {
            // 尝试解析 JSON（对象表示格式），只做格式校验，不改变用户原始结构。
            objectMapper.readTree(trimmedJson);
            // 返回合法 JSON（对象表示格式）字符串。
            return trimmedJson;
        } catch (JsonProcessingException exception) {
            // 抛出业务异常，让接口返回明确的参数错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, errorMessage, exception);
        }
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
     * blankToNull（空白转空引用）用于避免数据库保存无意义空字符串。
     *
     * @param value 原始字符串
     * @return 空白时返回 null，否则返回去除首尾空白后的原值
     */
    private String blankToNull(String value) {
        // 如果字符串为空或空白，则返回 null。
        if (value == null || value.isBlank()) {
            // 返回空引用。
            return null;
        }
        // 返回去掉首尾空白后的字符串。
        return value.strip();
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
