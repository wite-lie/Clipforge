package com.clipforge.contract.conversation;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConversationRouteRequest（会话路由请求）。
 *
 * <p>该请求用于承接前端聊天框的一轮输入，可以表达创作、修正、查询素材状态、查询成本、
 * 查询质量复盘、知识问答或普通寒暄。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRouteRequest {

    /** userId（用户编号）表示当前用户。 */
    private String userId;

    /** projectId（项目编号）表示当前项目。 */
    private String projectId;

    /** conversationId（会话编号）表示当前多轮会话。 */
    private String conversationId;

    /** runId（运行编号）表示用户查询某次创作运行时携带的编号。 */
    private String runId;

    /** message（用户消息）表示本轮自然语言输入。 */
    private String message;

    /** platform（目标平台）表示生成方案时使用的平台，例如 douyin（抖音）。 */
    private String platform;

    /** stylePreference（风格偏好）表示生成或修正方案时使用的风格要求。 */
    private String stylePreference;

    /** outputType（产物类型）表示本轮希望生成 VIDEO_PLAN（短视频方案）或 ARTICLE_PLAN（图文方案）。 */
    private String outputType;

    /** materialIds（素材编号列表）表示本轮显式选择或补充的素材。 */
    private List<String> materialIds;

    /** processTaskIds（处理任务编号列表）表示上传素材后获得的处理任务编号。 */
    private List<String> processTaskIds;

    /** knowledgeBaseIds（知识库编号列表）表示本轮显式选择的知识库。 */
    private List<String> knowledgeBaseIds;

    /** recentMessages（最近消息）表示前端可选传入的短历史，用于意图识别，不承载完整上下文。 */
    private List<String> recentMessages;
}
