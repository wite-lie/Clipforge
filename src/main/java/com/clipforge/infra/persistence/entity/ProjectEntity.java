package com.clipforge.infra.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ProjectEntity（项目实体）。
 *
 * <p>该实体映射 cf_project（项目表），用于保存用户的长期创作项目。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_project")
public class ProjectEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** projectId（项目编号）是对外使用的项目唯一编号。 */
    private String projectId;

    /** userId（用户编号）表示项目所属用户，当前学习阶段可以为空。 */
    private String userId;

    /** name（项目名称）表示项目的人类可读名称。 */
    private String name;

    /** description（项目描述）用于说明项目目标和背景。 */
    private String description;

    /** createTime（创建时间）保存项目创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）保存项目最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
