package com.clipforge.contract.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AppendConversationMessageRequest（追加会话消息请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppendConversationMessageRequest {

    /** runId（运行编号）用于关联一次创作运行，可为空。 */
    private String runId;

    /** role（消息角色）表示 system、user、assistant 或 tool。 */
    @NotBlank(message = "消息角色不能为空")
    private String role;

    /** content（消息内容）表示本轮会话文本。 */
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
