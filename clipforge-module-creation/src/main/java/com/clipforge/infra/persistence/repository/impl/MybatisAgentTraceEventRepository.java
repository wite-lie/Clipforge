package com.clipforge.memory.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.memory.infrastructure.persistence.entity.AgentTraceEventEntity;
import com.clipforge.memory.infrastructure.persistence.mapper.AgentTraceEventMapper;
import com.clipforge.memory.infrastructure.persistence.repository.AgentTraceEventRepository;
import com.clipforge.observability.trace.TraceEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisAgentTraceEventRepository（MyBatis 智能体执行链路事件仓储实现）。
 *
 * <p>该实现类只在 db（数据库）Profile（配置环境）下启用，负责保存和查询 Trace（执行链路追踪）事件。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisAgentTraceEventRepository implements AgentTraceEventRepository {

    /** agentTraceEventMapper（智能体执行链路事件映射器）负责执行事件表 CRUD（增删改查）。 */
    private final AgentTraceEventMapper agentTraceEventMapper;

    /**
     * saveEvents（保存事件列表）用于批量保存一次运行产生的执行链路事件。
     *
     * @param traceEvents 执行链路事件列表
     * @return 已保存的执行链路事件实体列表
     */
    @Override
    public List<AgentTraceEventEntity> saveEvents(List<TraceEvent> traceEvents) {
        // 把领域层 TraceEvent（执行链路事件）转换为数据库实体列表。
        List<AgentTraceEventEntity> entities = traceEvents.stream()
                // 将单条执行链路事件映射为数据库实体。
                .map(this::toEntity)
                // 收集为列表，便于后续返回和测试观察。
                .toList();
        // 遍历保存每一条执行链路事件。
        for (AgentTraceEventEntity entity : entities) {
            // 调用 Mapper（映射器）插入数据库。
            agentTraceEventMapper.insert(entity);
        }
        // 返回已保存实体列表。
        return entities;
    }

    /**
     * listByRunId（按运行编号查询列表）用于按发生时间升序查询执行链路事件。
     *
     * @param runId Agent（智能体）运行编号
     * @return 执行链路事件实体列表
     */
    @Override
    public List<AgentTraceEventEntity> listByRunId(String runId) {
        // 创建按 runId（运行编号）查询、排除逻辑删除并按发生时间升序排列的条件。
        LambdaQueryWrapper<AgentTraceEventEntity> queryWrapper = new LambdaQueryWrapper<AgentTraceEventEntity>()
                // runId（运行编号）必须匹配。
                .eq(AgentTraceEventEntity::getRunId, runId)
                // deleted（逻辑删除）必须为正常。
                .eq(AgentTraceEventEntity::getDeleted, 0)
                // 按事件发生时间升序排列。
                .orderByAsc(AgentTraceEventEntity::getOccurredAt);
        // 执行数据库查询并返回事件列表。
        return agentTraceEventMapper.selectList(queryWrapper);
    }

    /**
     * toEntity（转实体）用于把 TraceEvent（执行链路事件）转换为数据库实体。
     *
     * @param traceEvent 执行链路事件
     * @return 执行链路事件实体
     */
    private AgentTraceEventEntity toEntity(TraceEvent traceEvent) {
        // 使用 Builder（建造者模式）组装执行链路事件实体。
        return AgentTraceEventEntity.builder()
                // 设置数据库内部主键。
                .id(newId("trace_pk"))
                // 设置事件编号。
                .eventId(newId("trace"))
                // 设置 Agent（智能体）运行编号。
                .runId(traceEvent.getRunId())
                // 设置事件类型。
                .eventType(traceEvent.getEventType())
                // 设置事件消息。
                .message(traceEvent.getMessage())
                // 设置事件详情。
                .detail(traceEvent.getDetail())
                // 设置事件发生时间，并从 OffsetDateTime（带偏移时间）转为 LocalDateTime（本地时间）。
                .occurredAt(traceEvent.getOccurredAt().toLocalDateTime())
                // 设置事件入库时间。
                .createTime(LocalDateTime.now())
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体对象。
                .build();
    }

    /**
     * newId（生成编号）用于生成带业务前缀的随机编号。
     *
     * @param prefix 编号前缀
     * @return 随机编号
     */
    private String newId(String prefix) {
        // 使用 UUID（通用唯一识别码）生成足够唯一的本地开发阶段编号。
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }
}
