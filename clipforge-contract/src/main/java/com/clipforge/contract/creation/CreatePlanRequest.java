package com.clipforge.contract.creation;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreatePlanRequest（创建方案请求）。
 *
 * <p>该对象承载用户创建创作方案时提交的基础输入。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlanRequest {

    /** userId（用户编号）表示当前创作请求所属用户，当前学习阶段可以手动传入。 */
    private String userId;

    /** projectId（项目编号）表示当前创作请求所属项目，传入后便于成本统计、会话管理和项目默认知识库选择。 */
    private String projectId;

    /** conversationId（会话编号）表示当前创作请求所属会话，传入后系统会保存用户消息和助手消息摘要。 */
    private String conversationId;

    /** goal（创作目标）描述用户希望完成的内容创作任务。 */
    @NotBlank(message = "创作目标不能为空")
    private String goal;

    /** platform（目标平台）表示内容要发布到哪个平台，例如 douyin（抖音）或 xiaohongshu（小红书）。 */
    @NotBlank(message = "目标平台不能为空")
    private String platform;

    /** stylePreference（风格偏好）表示用户对表达风格、语气或节奏的偏好。 */
    private String stylePreference;

    /** outputType（产物类型）表示本次希望生成的内容形态；为空时默认 VIDEO_PLAN（短视频方案）。 */
    private String outputType;

    /** materialIds（素材编号列表）表示用户本次创作明确选择使用哪些图片或视频素材。 */
    private List<String> materialIds;

    /** knowledgeBaseIds（知识库编号列表）表示本次创作要检索哪些知识库；为空时系统会自动选择项目默认、平台默认或全库检索范围。 */
    private List<String> knowledgeBaseIds;
}
