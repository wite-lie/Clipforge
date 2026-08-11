package com.clipforge.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateConversationRequest（创建会话请求）。
 *
 * <p>该对象承载在 Project（项目）下创建 Conversation（会话）所需的输入。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    /** userId（用户编号）表示会话所属用户，当前学习阶段可以手动传入。 */
    private String userId;

    /** title（会话标题）表示本次连续创作讨论的主题。 */
    @NotBlank(message = "会话标题不能为空")
    private String title;
}
