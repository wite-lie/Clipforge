package com.clipforge.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AppendConversationMessageRequest（追加会话消息请求）。
 *
 * <p>该对象承载手动向 Conversation（会话）追加 Message（消息）时提交的角色、内容和可选运行编号。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendConversationMessageRequest {

    /** runId（运行编号）用于把消息关联到某次 Agent Run（智能体运行）。 */
    private String runId;

    /** role（消息角色）表示消息来自 system（系统）、user（用户）、assistant（助手）或 tool（工具）。 */
    @NotBlank(message = "消息角色不能为空")
    private String role;

    /** content（消息内容）表示本次追加的真实文本。 */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
