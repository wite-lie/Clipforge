package com.clipforge.agent.intent;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ConversationIntentRequest（会话意图识别请求）。
 *
 * <p>该对象把前端聊天框的一轮输入、项目归属、会话归属、素材编号和任务编号统一传给
 * ConversationIntentAgent（会话意图智能体），避免意图识别只能依赖创作方案请求。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationIntentRequest {

    /** userId（用户编号）表示当前发起对话的用户。 */
    private String userId;

    /** projectId（项目编号）表示当前对话所属项目。 */
    private String projectId;

    /** conversationId（会话编号）表示当前多轮对话编号。 */
    private String conversationId;

    /** runId（运行编号）表示用户显式查询某次 Agent Run（智能体运行）时携带的编号。 */
    private String runId;

    /** message（用户消息）表示本轮对话的自然语言输入。 */
    private String message;

    /** platform（目标平台）表示用户希望生成内容的平台，例如 douyin（抖音）。 */
    private String platform;

    /** stylePreference（风格偏好）表示用户希望保持的创作风格。 */
    private String stylePreference;

    /** materialIds（素材编号列表）表示用户本轮显式选择或补充的图片、视频素材。 */
    private List<String> materialIds;

    /** processTaskIds（处理任务编号列表）表示前端上传素材后获得的异步处理任务编号。 */
    private List<String> processTaskIds;

    /** knowledgeBaseIds（知识库编号列表）表示用户本轮显式选择的知识库范围。 */
    private List<String> knowledgeBaseIds;

    /** recentMessages（最近消息）表示用于意图识别的短历史上下文，不承载完整长会话。 */
    private List<String> recentMessages;
}
