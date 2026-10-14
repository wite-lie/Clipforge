package com.clipforge.ai.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChatMessage（聊天消息）。
 *
 * <p>该对象表示一次 Chat Model（聊天模型）调用中的一条消息，结构参考 OpenAI-Compatible API（OpenAI 兼容接口）的 role/content（角色/内容）格式。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /** role（角色）表示消息身份，例如 system（系统角色）、user（用户角色）、assistant（助手角色）。 */
    private String role;

    /** content（内容）表示消息文本。 */
    private String content;

    /**
     * system（系统消息）用于快速创建 system（系统角色）消息。
     *
     * @param content 消息内容
     * @return 聊天消息
     */
    public static ChatMessage system(String content) {
        // 使用 Builder（建造者模式）创建系统消息。
        return ChatMessage.builder()
                // 设置角色为 system（系统角色）。
                .role("system")
                // 设置消息内容。
                .content(content)
                // 构建聊天消息。
                .build();
    }

    /**
     * user（用户消息）用于快速创建 user（用户角色）消息。
     *
     * @param content 消息内容
     * @return 聊天消息
     */
    public static ChatMessage user(String content) {
        // 使用 Builder（建造者模式）创建用户消息。
        return ChatMessage.builder()
                // 设置角色为 user（用户角色）。
                .role("user")
                // 设置消息内容。
                .content(content)
                // 构建聊天消息。
                .build();
    }
}
