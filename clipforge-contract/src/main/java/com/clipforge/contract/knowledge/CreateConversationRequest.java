package com.clipforge.contract.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CreateConversationRequest（创建会话请求）。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "用户编号不能为空")
    private String userId;

    @NotBlank(message = "会话标题不能为空")
    private String title;
}
