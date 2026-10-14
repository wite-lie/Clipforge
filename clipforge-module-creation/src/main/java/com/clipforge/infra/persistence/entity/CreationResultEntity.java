package com.clipforge.memory.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.clipforge.memory.infrastructure.persistence.typehandler.JsonbStringTypeHandler;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreationResultEntity（创作结果实体）。
 *
 * <p>该实体映射数据库表 cf_creation_result（创作方案结果表），用于保存结构化创作方案。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "cf_creation_result", autoResultMap = true)
public class CreationResultEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** resultId（结果编号）是对外查询创作结果时使用的编号。 */
    private String resultId;

    /** taskId（任务编号）用于关联创作任务。 */
    private String taskId;

    /** runId（运行编号）用于关联 Agent（智能体）执行链路。 */
    private String runId;

    /** theme（创作主题）保存方案的核心表达方向。 */
    private String theme;

    /** targetAudience（目标受众）保存内容主要面向的人群。 */
    private String targetAudience;

    /** hook（开头钩子）保存内容开头吸引用户注意的句子。 */
    private String hook;

    /** storyboardJson（分镜脚本 JSON）保存分镜列表的 JSON（对象表示格式）字符串。 */
    @TableField(value = "storyboard_json", typeHandler = JsonbStringTypeHandler.class)
    private String storyboardJson;

    /** voiceoverScript（旁白脚本）保存视频口播或旁白内容。 */
    private String voiceoverScript;

    /** subtitleSuggestionsJson（字幕建议 JSON）保存字幕建议列表。 */
    @TableField(value = "subtitle_suggestions_json", typeHandler = JsonbStringTypeHandler.class)
    private String subtitleSuggestionsJson;

    /** titleCandidatesJson（标题候选 JSON）保存标题候选列表。 */
    @TableField(value = "title_candidates_json", typeHandler = JsonbStringTypeHandler.class)
    private String titleCandidatesJson;

    /** publishCopy（发布文案）保存发布正文区域使用的文案。 */
    private String publishCopy;

    /** hashtagsJson（话题标签 JSON）保存话题标签列表。 */
    @TableField(value = "hashtags_json", typeHandler = JsonbStringTypeHandler.class)
    private String hashtagsJson;

    /** score（效果评分）保存方案综合评分。 */
    private Integer score;

    /** improvementSuggestionsJson（改进建议 JSON）保存改进建议列表。 */
    @TableField(value = "improvement_suggestions_json", typeHandler = JsonbStringTypeHandler.class)
    private String improvementSuggestionsJson;

    /** createTime（创建时间）保存结果创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存结果最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
