package com.clipforge.contract.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UpdateConversationRequest（更新会话请求）。
 *
 * <p>当前用于修改用户可见的会话标题。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateConversationRequest {

    /** title（会话标题）是用户在会话列表中看到的名称。 */
    @NotBlank(message = "会话标题不能为空")
    private String title;
}
