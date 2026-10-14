package com.clipforge.material;

import lombok.Builder;
import lombok.Getter;

/**
 * MaterialQueryCondition（素材查询条件）。
 *
 * <p>该对象用于承载素材列表查询的过滤条件，避免 Controller（控制器）、Service（服务）和 Repository（仓储）
 * 之间传递过多零散参数。后续图片理解工具也可以复用该对象查询某个项目、会话或运行任务下的图片素材集合。
 */
@Getter
@Builder
public class MaterialQueryCondition {

    /** userId（用户编号）用于查询某个用户上传的素材。 */
    private final String userId;

    /** projectId（项目编号）用于查询某个项目下的素材。 */
    private final String projectId;

    /** conversationId（会话编号）用于查询某个会话下的素材。 */
    private final String conversationId;

    /** runId（运行编号）用于查询某次 Agent Run（智能体运行）关联的素材。 */
    private final String runId;

    /** materialType（素材类型）用于区分 IMAGE（图片）和后续 VIDEO（视频）等素材。 */
    private final String materialType;
}
