package com.clipforge.knowledge.infrastructure.redis;

import com.clipforge.knowledge.infrastructure.persistence.enums.KnowledgeDocumentProcessLogStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatus;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCache;
import com.clipforge.rag.process.KnowledgeDocumentProcessStatusCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * RedisKnowledgeDocumentProcessStatusCache（Redis 知识文档处理状态缓存）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，通过 Redis（缓存）保存知识文档异步处理状态。
 */
@Component
@Profile("db")
@RequiredArgsConstructor
public class RedisKnowledgeDocumentProcessStatusCache implements KnowledgeDocumentProcessStatusCache {

    /** stringRedisTemplate（字符串 Redis 模板）用于读写 Redis（缓存）字符串 Key（键）。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** objectMapper（对象映射器）用于把状态对象序列化为 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /** properties（配置属性）用于读取 Key（键）前缀和 TTL（生存时间）。 */
    private final KnowledgeDocumentProcessStatusCacheProperties properties;

    /**
     * save（保存）用于写入或刷新知识文档处理状态。
     *
     * @param status 处理状态
     */
    @Override
    public void save(KnowledgeDocumentProcessStatus status) {
        // 校验处理状态不为空。
        if (status == null) {
            // 空状态不写入缓存。
            return;
        }
        // 校验知识文档编号不为空。
        if (status.getDocumentId() == null || status.getDocumentId().isBlank()) {
            // 没有文档编号无法构造按文档查询的 Key（键）。
            return;
        }
        try {
            // 把状态对象序列化为 JSON（对象表示格式）字符串。
            String json = objectMapper.writeValueAsString(status);
            // 根据状态选择 TTL（生存时间）。
            Duration ttl = resolveTtl(status);
            // 写入按文档编号查询的缓存 Key（键）。
            stringRedisTemplate.opsForValue().set(documentKey(status.getDocumentId()), json, ttl);
            // 如果处理日志编号存在，同时写入按处理日志编号查询的缓存 Key（键）。
            if (status.getProcessLogId() != null && !status.getProcessLogId().isBlank()) {
                // 写入按处理日志编号查询的缓存 Key（键）。
                stringRedisTemplate.opsForValue().set(processLogKey(status.getProcessLogId()), json, ttl);
            }
        } catch (JsonProcessingException exception) {
            // 状态序列化失败属于程序内部错误，直接抛出运行时异常，便于测试阶段发现问题。
            throw new IllegalStateException("知识文档处理状态序列化失败", exception);
        }
    }

    /**
     * findByDocumentId（按文档编号查询）用于读取最近一次处理状态。
     *
     * @param documentId 知识文档编号
     * @return 可能存在的处理状态
     */
    @Override
    public Optional<KnowledgeDocumentProcessStatus> findByDocumentId(String documentId) {
        // 如果文档编号为空，则返回空。
        if (documentId == null || documentId.isBlank()) {
            // 返回空结果。
            return Optional.empty();
        }
        // 从 Redis（缓存）读取并反序列化状态。
        return readStatus(documentKey(documentId));
    }

    /**
     * findByProcessLogId（按处理日志编号查询）用于读取指定处理日志对应的状态。
     *
     * @param processLogId 处理日志编号
     * @return 可能存在的处理状态
     */
    @Override
    public Optional<KnowledgeDocumentProcessStatus> findByProcessLogId(String processLogId) {
        // 如果处理日志编号为空，则返回空。
        if (processLogId == null || processLogId.isBlank()) {
            // 返回空结果。
            return Optional.empty();
        }
        // 从 Redis（缓存）读取并反序列化状态。
        return readStatus(processLogKey(processLogId));
    }

    /**
     * deleteByDocumentId（按文档编号删除）用于清理过期或无效的处理状态缓存。
     *
     * @param documentId 知识文档编号
     */
    @Override
    public void deleteByDocumentId(String documentId) {
        // 如果文档编号为空，则无需删除。
        if (documentId == null || documentId.isBlank()) {
            // 直接返回。
            return;
        }
        // 先读取状态，便于同时删除 processLogId（处理日志编号）索引。
        Optional<KnowledgeDocumentProcessStatus> statusOptional = findByDocumentId(documentId);
        // 删除按文档编号查询的 Key（键）。
        stringRedisTemplate.delete(documentKey(documentId));
        // 如果状态存在且处理日志编号不为空，则删除按处理日志编号查询的 Key（键）。
        statusOptional.map(KnowledgeDocumentProcessStatus::getProcessLogId)
                // 过滤空白处理日志编号。
                .filter(processLogId -> processLogId != null && !processLogId.isBlank())
                // 删除处理日志 Key（键）。
                .ifPresent(processLogId -> stringRedisTemplate.delete(processLogKey(processLogId)));
    }

    /**
     * readStatus（读取状态）用于从 Redis（缓存）读取 JSON（对象表示格式）并转回状态对象。
     *
     * @param key Redis（缓存）Key（键）
     * @return 可能存在的处理状态
     */
    private Optional<KnowledgeDocumentProcessStatus> readStatus(String key) {
        try {
            // 读取 JSON（对象表示格式）字符串。
            String json = stringRedisTemplate.opsForValue().get(key);
            // 如果缓存不存在，则返回空。
            if (json == null || json.isBlank()) {
                // 返回空结果。
                return Optional.empty();
            }
            // 反序列化为处理状态对象。
            return Optional.of(objectMapper.readValue(json, KnowledgeDocumentProcessStatus.class));
        } catch (JsonProcessingException exception) {
            // 反序列化失败说明缓存内容异常，直接抛出运行时异常，便于测试阶段发现问题。
            throw new IllegalStateException("知识文档处理状态反序列化失败，key：" + key, exception);
        }
    }

    /**
     * resolveTtl（解析生存时间）用于按处理状态选择 Redis（缓存）TTL（生存时间）。
     *
     * @param status 处理状态
     * @return TTL（生存时间）
     */
    private Duration resolveTtl(KnowledgeDocumentProcessStatus status) {
        // 成功或跳过状态使用成功 TTL（生存时间）。
        if (status.getStatus() == KnowledgeDocumentProcessLogStatus.SUCCESS || status.getStatus() == KnowledgeDocumentProcessLogStatus.SKIPPED) {
            // 返回成功 TTL。
            return properties.getSuccessTtl();
        }
        // 失败状态使用失败 TTL（生存时间）。
        if (status.getStatus() == KnowledgeDocumentProcessLogStatus.FAILED) {
            // 返回失败 TTL。
            return properties.getFailedTtl();
        }
        // 其他状态默认按运行中处理。
        return properties.getRunningTtl();
    }

    /**
     * documentKey（文档状态键）用于生成按文档编号查询的 Redis（缓存）Key（键）。
     *
     * @param documentId 知识文档编号
     * @return Redis Key（缓存键）
     */
    private String documentKey(String documentId) {
        // 拼接文档维度 Key（键）。
        return properties.getKeyPrefix() + ":document:" + documentId;
    }

    /**
     * processLogKey（处理日志状态键）用于生成按处理日志编号查询的 Redis（缓存）Key（键）。
     *
     * @param processLogId 处理日志编号
     * @return Redis Key（缓存键）
     */
    private String processLogKey(String processLogId) {
        // 拼接处理日志维度 Key（键）。
        return properties.getKeyPrefix() + ":log:" + processLogId;
    }
}
