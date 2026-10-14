package com.clipforge.infra.redis;

import com.clipforge.task.TaskStatusValue;
import com.clipforge.material.task.cache.MaterialProcessTaskStatus;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCache;
import com.clipforge.material.task.cache.MaterialProcessTaskStatusCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * RedisMaterialProcessTaskStatusCache（Redis 素材处理任务状态缓存）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）且配置启用时生效，通过 Redis String（字符串）保存素材处理任务状态 JSON（对象表示格式）。
 */
@Component
@Profile("db")
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clipforge.task.material-process-status-cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisMaterialProcessTaskStatusCache implements MaterialProcessTaskStatusCache {

    /** stringRedisTemplate（字符串 Redis 模板）用于读写 Redis（缓存）字符串 Key（键）。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** objectMapper（对象映射器）用于把状态对象序列化为 JSON（对象表示格式）。 */
    private final ObjectMapper objectMapper;

    /** properties（配置属性）用于读取 Key（键）前缀和 TTL（生存时间）。 */
    private final MaterialProcessTaskStatusCacheProperties properties;

    /**
     * get（读取）用于按任务编号读取素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     * @return 可能存在的素材处理任务状态
     */
    @Override
    public Optional<MaterialProcessTaskStatus> get(String taskId) {
        // 如果任务编号为空，则直接返回空结果。
        if (taskId == null || taskId.isBlank()) {
            // 返回空结果。
            return Optional.empty();
        }
        try {
            // 从 Redis（缓存）读取 JSON（对象表示格式）字符串。
            String json = stringRedisTemplate.opsForValue().get(key(taskId));
            // 如果缓存不存在，则返回空。
            if (json == null || json.isBlank()) {
                // 返回空结果。
                return Optional.empty();
            }
            // 反序列化为素材处理任务状态对象。
            return Optional.of(objectMapper.readValue(json, MaterialProcessTaskStatus.class));
        } catch (JsonProcessingException exception) {
            // 反序列化失败说明缓存内容异常，直接抛出运行时异常，便于测试阶段发现问题。
            throw new IllegalStateException("素材处理任务状态反序列化失败，taskId：" + taskId, exception);
        }
    }

    /**
     * save（保存）用于写入或刷新素材处理任务缓存状态。
     *
     * @param status 素材处理任务状态
     */
    @Override
    public void save(MaterialProcessTaskStatus status) {
        // 校验状态对象不为空。
        if (status == null) {
            // 空状态不写入缓存。
            return;
        }
        // 校验任务编号不为空。
        if (status.getTaskId() == null || status.getTaskId().isBlank()) {
            // 没有任务编号无法构造 Redis（缓存）Key（键）。
            return;
        }
        try {
            // 把状态对象序列化为 JSON（对象表示格式）字符串。
            String json = objectMapper.writeValueAsString(status);
            // 根据状态选择 TTL（生存时间）。
            Duration ttl = resolveTtl(status);
            // 使用 String（字符串）类型写入 Redis（缓存）。
            stringRedisTemplate.opsForValue().set(key(status.getTaskId()), json, ttl);
        } catch (JsonProcessingException exception) {
            // 状态序列化失败属于程序内部错误，直接抛出运行时异常，便于测试阶段发现问题。
            throw new IllegalStateException("素材处理任务状态序列化失败", exception);
        }
    }

    /**
     * delete（删除）用于清理素材处理任务缓存状态。
     *
     * @param taskId 任务编号
     */
    @Override
    public void delete(String taskId) {
        // 如果任务编号为空，则无需删除。
        if (taskId == null || taskId.isBlank()) {
            // 直接返回。
            return;
        }
        // 删除 Redis（缓存）Key（键）。
        stringRedisTemplate.delete(key(taskId));
    }

    /**
     * resolveTtl（解析生存时间）用于按任务状态选择 Redis（缓存）TTL（生存时间）。
     *
     * @param status 素材处理任务状态
     * @return TTL（生存时间）
     */
    private Duration resolveTtl(MaterialProcessTaskStatus status) {
        // 成功状态使用成功 TTL（生存时间）。
        if (TaskStatusValue.SUCCEEDED.value().equals(status.getStatus()) || TaskStatusValue.SKIPPED.value().equals(status.getStatus())) {
            // 返回成功 TTL。
            return properties.getSuccessTtl();
        }
        // 失败状态使用失败 TTL（生存时间）。
        if (TaskStatusValue.FAILED.value().equals(status.getStatus())) {
            // 返回失败 TTL。
            return properties.getFailedTtl();
        }
        // 处理中状态使用处理中 TTL（生存时间）。
        if (TaskStatusValue.PROCESSING.value().equals(status.getStatus())) {
            // 返回处理中 TTL。
            return properties.getProcessingTtl();
        }
        // 等待状态使用等待 TTL（生存时间）。
        return properties.getPendingTtl();
    }

    /**
     * key（缓存键）用于生成素材处理任务状态 Redis（缓存）Key（键）。
     *
     * @param taskId 任务编号
     * @return Redis Key（缓存键）
     */
    private String key(String taskId) {
        // 拼接任务维度 Key（键）。
        return properties.getKeyPrefix() + ":" + taskId;
    }
}
