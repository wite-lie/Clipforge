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
 * MaterialEntity（素材实体）。
 *
 * <p>该实体映射 cf_material（素材表），用于保存图片、视频等创作素材元数据。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_material")
public class MaterialEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** materialId（素材编号）是对外使用的素材唯一编号。 */
    private String materialId;

    /** userId（用户编号）表示素材所属用户。 */
    private String userId;

    /** projectId（项目编号）表示素材所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示素材所属会话。 */
    private String conversationId;

    /** runId（运行编号）表示素材关联的 Agent Run（智能体运行）。 */
    private String runId;

    /** materialType（素材类型）支持 IMAGE（图片）和 VIDEO（视频）。 */
    private String materialType;

    /** originalFilename（原始文件名）表示用户上传时的文件名。 */
    private String originalFilename;

    /** storedFilename（存储文件名）表示系统保存时生成的文件名。 */
    private String storedFilename;

    /** filePath（文件路径）表示文件在本地磁盘或对象存储中的位置。 */
    private String filePath;

    /** contentType（内容类型）表示文件 MIME（媒体类型）。 */
    private String contentType;

    /** fileSize（文件大小）单位为字节。 */
    private Long fileSize;

    /** status（素材状态）表示 UPLOADED（已上传）等处理状态。 */
    private String status;

    /** createTime（创建时间）表示记录创建时间。 */
    private LocalDateTime createTime;

    /** updateTime（更新时间）表示记录最近更新时间。 */
    private LocalDateTime updateTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
