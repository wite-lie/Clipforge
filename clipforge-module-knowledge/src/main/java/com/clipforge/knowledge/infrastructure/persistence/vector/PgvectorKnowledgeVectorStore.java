package com.clipforge.knowledge.infrastructure.persistence.vector;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.knowledge.vector.VectorStore;
import com.clipforge.knowledge.vector.VectorStoreReplaceDocumentRequest;
import com.clipforge.knowledge.vector.VectorStoreSearchRequest;
import com.clipforge.knowledge.vector.VectorStoreSearchResult;
import com.clipforge.knowledge.vector.VectorStoreUpsertItem;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * PgvectorKnowledgeVectorStore（pgvector 知识向量存储）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 PostgreSQL（关系数据库）和 pgvector（PostgreSQL 向量扩展）保存、替换和检索知识向量。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class PgvectorKnowledgeVectorStore implements VectorStore {

    /** DEFAULT_DISTANCE_METRIC（默认距离度量）当前使用 COSINE（余弦距离）。 */
    private static final String DEFAULT_DISTANCE_METRIC = "COSINE";

    /** FIXED_VECTOR_DIMENSION（固定向量维度）必须和 cf_knowledge_embedding.embedding VECTOR(1024) 保持一致。 */
    private static final int FIXED_VECTOR_DIMENSION = 1024;

    /** jdbcTemplate（数据库操作模板）用于执行 pgvector（PostgreSQL 向量扩展）SQL（结构化查询语言）。 */
    private final JdbcTemplate jdbcTemplate;

    /**
     * replaceByDocumentId（按文档编号替换向量）用于在短事务中逻辑删除旧向量并插入新向量。
     *
     * @param request 替换文档向量请求
     */
    @Override
    @Transactional
    public void replaceByDocumentId(VectorStoreReplaceDocumentRequest request) {
        // 校验写入请求。
        validateReplaceRequest(request);
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 先逻辑删除该文档旧向量，避免同一个 chunkId（切片编号）唯一约束冲突。
        jdbcTemplate.update(
                // 按文档编号更新旧向量删除标记。
                "update cf_knowledge_embedding set deleted = 1, update_time = ? where document_id = ? and deleted = 0",
                // 设置更新时间。
                now,
                // 设置文档编号。
                request.getDocumentId()
        );
        // 批量写入新向量；如果 chunkId（切片编号）已经存在，则更新原记录并恢复为有效状态。
        jdbcTemplate.batchUpdate(
                // embedding（向量）字段需要显式转换为 pgvector 的 vector 类型；ON CONFLICT（冲突处理）用于兼容同一 chunkId 重复向量化。
                """
                        insert into cf_knowledge_embedding (id, embedding_id, knowledge_base_id, document_id, chunk_id, provider, model_name, dimension, distance_metric, embedding, create_time, update_time, deleted)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?, ?, 0)
                        on conflict (chunk_id) do update set
                            knowledge_base_id = excluded.knowledge_base_id,
                            document_id = excluded.document_id,
                            provider = excluded.provider,
                            model_name = excluded.model_name,
                            dimension = excluded.dimension,
                            distance_metric = excluded.distance_metric,
                            embedding = excluded.embedding,
                            update_time = excluded.update_time,
                            deleted = 0
                        """,
                // 创建批量参数设置器。
                new BatchPreparedStatementSetter() {
                    /**
                     * setValues（设置参数）用于给每一条向量绑定 SQL（结构化查询语言）参数。
                     *
                     * @param ps 预编译语句
                     * @param index 当前批次索引
                     * @throws java.sql.SQLException 数据库参数绑定异常
                     */
                    @Override
                    public void setValues(PreparedStatement ps, int index) throws java.sql.SQLException {
                        // 读取当前写入条目。
                        VectorStoreUpsertItem item = request.getItems().get(index);
                        // 设置数据库主键。
                        ps.setString(1, newId("knowledge_embedding_pk"));
                        // 设置对外向量编号。
                        ps.setString(2, newId("knowledge_embedding"));
                        // 设置知识库编号。
                        ps.setString(3, request.getKnowledgeBaseId());
                        // 设置文档编号。
                        ps.setString(4, request.getDocumentId());
                        // 设置切片编号。
                        ps.setString(5, item.getChunkId());
                        // 设置供应商。
                        ps.setString(6, request.getProvider());
                        // 设置模型名称。
                        ps.setString(7, request.getModelName());
                        // 设置向量维度。
                        ps.setInt(8, request.getDimension());
                        // 设置距离度量。
                        ps.setString(9, defaultText(request.getDistanceMetric(), DEFAULT_DISTANCE_METRIC));
                        // 设置 pgvector 文本表示。
                        ps.setString(10, toVectorLiteral(item.getVector()));
                        // 设置创建时间。
                        ps.setObject(11, now);
                        // 设置更新时间。
                        ps.setObject(12, now);
                    }

                    /**
                     * getBatchSize（获取批次大小）用于告诉 JdbcTemplate（数据库操作模板）本次写入多少条。
                     *
                     * @return 批次大小
                     */
                    @Override
                    public int getBatchSize() {
                        // 返回向量条目数量。
                        return request.getItems().size();
                    }
                }
        );
    }

    /**
     * search（检索）用于按知识库范围执行余弦 TopK（前 K 个）向量检索。
     *
     * @param request 向量检索请求
     * @return 检索结果列表
     */
    @Override
    public List<VectorStoreSearchResult> search(VectorStoreSearchRequest request) {
        // 校验检索请求。
        validateSearchRequest(request);
        // 读取 TopK（前 K 个）参数。
        int topK = request.getTopK() == null || request.getTopK() <= 0 ? 5 : request.getTopK();
        // 转换查询向量为 pgvector 文本表示。
        String queryVector = toVectorLiteral(request.getQueryVector());
        // 如果请求开启全库检索，则不按知识库编号过滤。
        if (Boolean.TRUE.equals(request.getSearchAllKnowledgeBases())) {
            // 执行全库向量检索。
            return searchAllKnowledgeBases(queryVector, topK);
        }
        // 执行向量检索并关联切片表返回正文。
        return jdbcTemplate.query(
                """
                        select e.knowledge_base_id,
                               e.document_id,
                               e.chunk_id,
                               c.chunk_index,
                               c.title_path,
                               c.content,
                               c.metadata_json::text as metadata_json,
                               (e.embedding <=> ?::vector) as distance,
                               (1 - (e.embedding <=> ?::vector)) as score
                        from cf_knowledge_embedding e
                        join cf_knowledge_chunk c on c.chunk_id = e.chunk_id
                        join cf_knowledge_document d on d.document_id = e.document_id
                        where e.knowledge_base_id = ?
                          and e.deleted = 0
                          and c.deleted = 0
                          and c.enabled = 1
                          and d.deleted = 0
                          and d.enabled = 1
                          and d.overall_status <> 'PENDING_REINDEX'
                        order by e.embedding <=> ?::vector
                        limit ?
                        """,
                // 映射单条检索结果。
                (rs, rowNum) -> VectorStoreSearchResult.builder()
                        // 设置知识库编号。
                        .knowledgeBaseId(rs.getString("knowledge_base_id"))
                        // 设置文档编号。
                        .documentId(rs.getString("document_id"))
                        // 设置切片编号。
                        .chunkId(rs.getString("chunk_id"))
                        // 设置切片序号。
                        .chunkIndex(rs.getInt("chunk_index"))
                        // 设置标题路径。
                        .titlePath(rs.getString("title_path"))
                        // 设置正文。
                        .content(rs.getString("content"))
                        // 设置元数据 JSON。
                        .metadataJson(rs.getString("metadata_json"))
                        // 设置距离。
                        .distance(rs.getDouble("distance"))
                        // 设置相似度分数。
                        .score(rs.getDouble("score"))
                        // 构建结果。
                        .build(),
                // 第一次距离计算向量。
                queryVector,
                // score（分数）计算向量。
                queryVector,
                // 知识库编号。
                request.getKnowledgeBaseId(),
                // 排序计算向量。
                queryVector,
                // 返回数量。
                topK
        );
    }

    /**
     * searchAllKnowledgeBases（检索全部知识库）用于在没有具体知识库编号时执行全库 TopK（前 K 个）向量检索。
     *
     * @param queryVector 查询向量文本
     * @param topK 返回数量
     * @return 检索结果列表
     */
    private List<VectorStoreSearchResult> searchAllKnowledgeBases(String queryVector, int topK) {
        // 执行不限制 knowledge_base_id（知识库编号）的向量检索。
        return jdbcTemplate.query(
                """
                        select e.knowledge_base_id,
                               e.document_id,
                               e.chunk_id,
                               c.chunk_index,
                               c.title_path,
                               c.content,
                               c.metadata_json::text as metadata_json,
                               (e.embedding <=> ?::vector) as distance,
                               (1 - (e.embedding <=> ?::vector)) as score
                        from cf_knowledge_embedding e
                        join cf_knowledge_chunk c on c.chunk_id = e.chunk_id
                        join cf_knowledge_document d on d.document_id = e.document_id
                        where e.deleted = 0
                          and c.deleted = 0
                          and c.enabled = 1
                          and d.deleted = 0
                          and d.enabled = 1
                          and d.overall_status <> 'PENDING_REINDEX'
                        order by e.embedding <=> ?::vector
                        limit ?
                        """,
                // 映射单条检索结果。
                (rs, rowNum) -> VectorStoreSearchResult.builder()
                        .knowledgeBaseId(rs.getString("knowledge_base_id"))
                        .documentId(rs.getString("document_id"))
                        .chunkId(rs.getString("chunk_id"))
                        .chunkIndex(rs.getInt("chunk_index"))
                        .titlePath(rs.getString("title_path"))
                        .content(rs.getString("content"))
                        .metadataJson(rs.getString("metadata_json"))
                        .distance(rs.getDouble("distance"))
                        .score(rs.getDouble("score"))
                        .build(),
                queryVector,
                queryVector,
                queryVector,
                topK
        );
    }

    /**
     * validateReplaceRequest（校验替换请求）用于避免写入不完整或维度不一致的向量。
     *
     * @param request 替换请求
     */
    private void validateReplaceRequest(VectorStoreReplaceDocumentRequest request) {
        // 校验基础字段。
        if (request == null || isBlank(request.getKnowledgeBaseId()) || isBlank(request.getDocumentId())) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量替换请求缺少 knowledgeBaseId（知识库编号）或 documentId（知识文档编号）");
        }
        // 校验向量维度。
        if (request.getDimension() == null || request.getDimension() <= 0) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量维度必须大于 0");
        }
        // 当前数据库字段固定为 VECTOR(1024)，维度不一致会导致 pgvector（PostgreSQL 向量扩展）写入失败。
        if (request.getDimension() != FIXED_VECTOR_DIMENSION) {
            // 抛出明确错误，提示检查嵌入模型配置。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量维度必须等于 " + FIXED_VECTOR_DIMENSION + "，请检查 clipforge.embedding.dimension 和真实模型输出维度");
        }
        // 校验写入条目。
        if (request.getItems() == null || request.getItems().isEmpty()) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量替换请求 items（向量条目）不能为空");
        }
        // 校验每条向量。
        for (VectorStoreUpsertItem item : request.getItems()) {
            // 校验切片编号和向量。
            if (item == null || isBlank(item.getChunkId()) || item.getVector() == null || item.getVector().isEmpty()) {
                // 抛出请求错误。
                throw new BusinessException(ErrorCode.BAD_REQUEST, "向量条目缺少 chunkId（切片编号）或 vector（向量）");
            }
            // 校验向量维度一致。
            if (item.getVector().size() != request.getDimension()) {
                // 抛出请求错误。
                throw new BusinessException(ErrorCode.BAD_REQUEST, "向量维度不一致，chunkId：" + item.getChunkId());
            }
        }
    }

    /**
     * validateSearchRequest（校验检索请求）用于避免无效向量检索。
     *
     * @param request 检索请求
     */
    private void validateSearchRequest(VectorStoreSearchRequest request) {
        // 校验查询向量。
        if (request == null || request.getQueryVector() == null || request.getQueryVector().isEmpty()) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量检索请求缺少 queryVector（查询向量）");
        }
        // 普通检索必须指定知识库编号；全库检索允许知识库编号为空。
        if (!Boolean.TRUE.equals(request.getSearchAllKnowledgeBases()) && isBlank(request.getKnowledgeBaseId())) {
            // 抛出请求错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "向量检索请求缺少 knowledgeBaseId（知识库编号），如需全库检索请开启 searchAllKnowledgeBases（检索全部知识库）");
        }
        // 校验查询向量维度和数据库字段一致。
        if (request.getQueryVector().size() != FIXED_VECTOR_DIMENSION) {
            // 抛出明确错误。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "查询向量维度必须等于 " + FIXED_VECTOR_DIMENSION + "，请检查 Embedding（嵌入）模型配置");
        }
    }

    /**
     * toVectorLiteral（转换向量字面量）用于把 Java（爪哇语言）浮点列表转成 pgvector（PostgreSQL 向量扩展）文本格式。
     *
     * @param vector 向量
     * @return pgvector 文本格式
     */
    private String toVectorLiteral(List<Float> vector) {
        // 创建拼接器，pgvector 文本格式形如 [0.1,0.2,0.3]。
        StringBuilder builder = new StringBuilder("[");
        // 遍历向量元素。
        for (int index = 0; index < vector.size(); index++) {
            // 非首元素前添加逗号。
            if (index > 0) {
                // 添加逗号。
                builder.append(',');
            }
            // 添加浮点数。
            builder.append(vector.get(index));
        }
        // 添加结尾方括号。
        builder.append(']');
        // 返回向量字面量。
        return builder.toString();
    }

    /**
     * defaultText（默认文本）用于处理空字符串。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 最终文本
     */
    private String defaultText(String value, String defaultValue) {
        // 如果原始值为空，返回默认值。
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * isBlank（是否空白）用于判断字符串是否为空引用或空白。
     *
     * @param value 原始值
     * @return 是否空白
     */
    private boolean isBlank(String value) {
        // 判断字符串是否为空引用或空白字符串。
        return value == null || value.isBlank();
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
