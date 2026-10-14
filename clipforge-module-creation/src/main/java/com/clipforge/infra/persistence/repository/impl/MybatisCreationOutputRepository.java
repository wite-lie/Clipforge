package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.contract.creation.CreationOutputType;
import com.clipforge.memory.infrastructure.persistence.entity.CreationOutputEntity;
import com.clipforge.memory.infrastructure.persistence.mapper.CreationOutputMapper;
import com.clipforge.memory.infrastructure.persistence.repository.CreationOutputRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationOutputRepository（MyBatis 创作产物仓储实现）。
 *
 * <p>该实现只在 db（数据库）Profile（配置环境）下启用，负责把通用创作产物保存到 PostgreSQL（关系数据库）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationOutputRepository implements CreationOutputRepository {

    /** DEFAULT_SCHEMA_VERSION（默认结构版本）表示当前通用产物 JSON 的版本。 */
    private static final String DEFAULT_SCHEMA_VERSION = "creation-output.v1";

    /** creationOutputMapper（创作产物映射器）负责执行数据库 CRUD（增删改查）。 */
    private final CreationOutputMapper creationOutputMapper;

    /** objectMapper（对象映射器）负责把结构化产物序列化为 JSON。 */
    private final ObjectMapper objectMapper;

    @Override
    public CreationOutputEntity saveOutput(String taskId, String runId, CreationOutputType outputType, Object outputPayload) {
        LocalDateTime now = LocalDateTime.now();
        CreationOutputEntity entity = CreationOutputEntity.builder()
                .id(newId("creation_output_pk"))
                .outputId(newId("creation_output"))
                .taskId(taskId)
                .runId(runId)
                .outputType((outputType == null ? CreationOutputType.VIDEO_PLAN : outputType).name())
                .schemaVersion(DEFAULT_SCHEMA_VERSION)
                .payloadJson(toJson(outputPayload))
                .createTime(now)
                .updateTime(now)
                .deleted(0)
                .build();
        creationOutputMapper.insert(entity);
        return entity;
    }

    @Override
    public Optional<CreationOutputEntity> findLatestByRunId(String runId) {
        LambdaQueryWrapper<CreationOutputEntity> queryWrapper = new LambdaQueryWrapper<CreationOutputEntity>()
                .eq(CreationOutputEntity::getRunId, runId)
                .eq(CreationOutputEntity::getDeleted, 0)
                .orderByDesc(CreationOutputEntity::getCreateTime)
                .last("LIMIT 1");
        return Optional.ofNullable(creationOutputMapper.selectOne(queryWrapper));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("创作产物 JSON 序列化失败", exception);
        }
    }

    private String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
