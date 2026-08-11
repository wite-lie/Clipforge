package com.clipforge.infra.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clipforge.agent.CreationPlanResult;
import com.clipforge.infra.persistence.entity.CreationResultEntity;
import com.clipforge.infra.persistence.mapper.CreationResultMapper;
import com.clipforge.infra.persistence.repository.CreationResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * MybatisCreationResultRepository（MyBatis 创作结果仓储实现）。
 *
 * <p>该实现类只在 db（数据库）Profile（配置环境）下启用，负责把创作方案保存到 PostgreSQL（关系数据库）。
 */
@Repository
@Profile("db")
@RequiredArgsConstructor
public class MybatisCreationResultRepository implements CreationResultRepository {

    /** creationResultMapper（创作结果映射器）负责执行创作结果表 CRUD（增删改查）。 */
    private final CreationResultMapper creationResultMapper;

    /** objectMapper（对象映射器）负责把 Java（爪哇语言）对象序列化为 JSON（对象表示格式）字符串。 */
    private final ObjectMapper objectMapper;

    /**
     * saveResult（保存结果）用于把创作方案结果保存到数据库。
     *
     * @param taskId 创作任务编号
     * @param runId Agent（智能体）运行编号
     * @param result 创作方案结果
     * @return 已保存的创作结果实体
     */
    @Override
    public CreationResultEntity saveResult(String taskId, String runId, CreationPlanResult result) {
        // 获取当前时间，统一填充创建时间和更新时间。
        LocalDateTime now = LocalDateTime.now();
        // 使用 Builder（建造者模式）组装创作结果实体。
        CreationResultEntity entity = CreationResultEntity.builder()
                // 设置数据库内部主键。
                .id(newId("result_pk"))
                // 设置对外结果编号。
                .resultId(newId("result"))
                // 设置关联任务编号。
                .taskId(taskId)
                // 设置 Agent（智能体）运行编号。
                .runId(runId)
                // 设置创作主题。
                .theme(result.getTheme())
                // 设置目标受众。
                .targetAudience(result.getTargetAudience())
                // 设置开头钩子。
                .hook(result.getHook())
                // 设置分镜脚本 JSON（对象表示格式）。
                .storyboardJson(toJson(result.getStoryboard()))
                // 设置旁白脚本。
                .voiceoverScript(result.getVoiceoverScript())
                // 设置字幕建议 JSON（对象表示格式）。
                .subtitleSuggestionsJson(toJson(result.getSubtitleSuggestions()))
                // 设置标题候选 JSON（对象表示格式）。
                .titleCandidatesJson(toJson(result.getTitleCandidates()))
                // 设置发布文案。
                .publishCopy(result.getPublishCopy())
                // 设置话题标签 JSON（对象表示格式）。
                .hashtagsJson(toJson(result.getHashtags()))
                // 设置效果评分。
                .score(result.getScore())
                // 设置改进建议 JSON（对象表示格式）。
                .improvementSuggestionsJson(toJson(result.getImprovementSuggestions()))
                // 设置创建时间。
                .createTime(now)
                // 设置更新时间。
                .updateTime(now)
                // 设置逻辑删除标记为正常。
                .deleted(0)
                // 构建实体对象。
                .build();
        // 调用 Mapper（映射器）插入数据库。
        creationResultMapper.insert(entity);
        // 返回已保存实体，供后续接口响应或测试观察。
        return entity;
    }

    /**
     * findByRunId（按运行编号查询）用于查询一次 Agent（智能体）运行的创作结果。
     *
     * @param runId Agent（智能体）运行编号
     * @return 可能存在的创作结果实体
     */
    @Override
    public Optional<CreationResultEntity> findByRunId(String runId) {
        // 创建按 runId（运行编号）查询且排除逻辑删除数据的条件。
        LambdaQueryWrapper<CreationResultEntity> queryWrapper = new LambdaQueryWrapper<CreationResultEntity>()
                // runId（运行编号）必须匹配。
                .eq(CreationResultEntity::getRunId, runId)
                // deleted（逻辑删除）必须为正常。
                .eq(CreationResultEntity::getDeleted, 0);
        // 执行数据库查询，并用 Optional（可选值）包装可能为空的结果。
        return Optional.ofNullable(creationResultMapper.selectOne(queryWrapper));
    }

    /**
     * toJson（转 JSON）用于把列表类字段转换为 JSON（对象表示格式）字符串。
     *
     * @param value 待序列化对象
     * @return JSON（对象表示格式）字符串
     */
    private String toJson(Object value) {
        try {
            // 使用 Jackson（JSON 处理工具）序列化对象。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            // 序列化失败说明对象结构异常，直接抛出非法状态异常，避免保存半成品数据。
            throw new IllegalStateException("创作结果 JSON 序列化失败", exception);
        }
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
