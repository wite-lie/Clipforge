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
 * ModelCallRecordEntity（模型调用记录实体）。
 *
 * <p>该实体映射 cf_model_call_record（模型调用记录表），用于保存每次模型调用的 Token（词元）和耗时明细。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cf_model_call_record")
public class ModelCallRecordEntity {

    /** id（主键）是数据库内部主键。 */
    @TableId(type = IdType.INPUT)
    private String id;

    /** recordId（记录编号）是模型调用记录的业务编号。 */
    private String recordId;

    /** runId（运行编号）表示所属 Agent（智能体）运行。 */
    private String runId;

    /** projectId（项目编号）表示所属 Project（项目），当前阶段可以为空。 */
    private String projectId;

    /** conversationId（会话编号）表示所属 Conversation（会话），当前阶段可以为空。 */
    private String conversationId;

    /** provider（供应商）表示模型服务来源。 */
    private String provider;

    /** modelName（模型名称）表示实际调用的模型。 */
    private String modelName;

    /** purpose（调用用途）用于区分生成、修复、评估等场景。 */
    private String purpose;

    /** promptTokens（输入词元数）表示提示词消耗。 */
    private Integer promptTokens;

    /** completionTokens（输出词元数）表示模型回答消耗。 */
    private Integer completionTokens;

    /** totalTokens（总词元数）表示输入和输出合计消耗。 */
    private Integer totalTokens;

    /** latencyMs（调用耗时毫秒）表示模型请求耗时。 */
    private Long latencyMs;

    /** success（是否成功）表示模型调用是否成功。 */
    private Boolean success;

    /** errorCode（错误码）表示失败时的错误类型。 */
    private String errorCode;

    /** errorMessage（错误消息）表示失败时的简要原因。 */
    private String errorMessage;

    /** createTime（创建时间）保存记录入库时间。 */
    private LocalDateTime createTime;

    /** deleted（逻辑删除标记）0 表示正常，1 表示删除。 */
    private Integer deleted;
}
