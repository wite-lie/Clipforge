package com.clipforge.server.api;

import com.clipforge.contract.knowledge.AppendConversationMessageRequest;
import com.clipforge.contract.knowledge.ConversationMessageResponse;
import com.clipforge.contract.conversation.ConversationRouteRequest;
import com.clipforge.contract.knowledge.ConversationRouteResponse;
import com.clipforge.contract.knowledge.QueryConversationMessagesResponse;
import com.clipforge.contract.knowledge.ConversationResponse;
import com.clipforge.contract.knowledge.UpdateConversationRequest;
import com.clipforge.application.ConversationRouteApplicationService;
import com.clipforge.application.ProjectConversationApplicationService;
import com.clipforge.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ConversationController（会话控制器）。
 *
 * <p>该控制器负责 Conversation（会话）消息查询、消息追加，以及面向前端聊天框的统一会话路由入口。
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    /** projectConversationApplicationService（项目会话应用服务）负责会话消息查询和追加。 */
    private final ProjectConversationApplicationService projectConversationApplicationService;

    /** conversationRouteApplicationService（会话路由应用服务）负责识别意图并选择后端处理流程。 */
    private final ConversationRouteApplicationService conversationRouteApplicationService;

    /**
     * queryMessages（查询会话消息）接口用于读取一次会话中的多轮消息。
     *
     * @param conversationId 会话编号
     * @return 查询会话消息响应
     */
    @GetMapping("/{conversationId}/messages")
    public ApiResponse<QueryConversationMessagesResponse> queryMessages(@PathVariable String conversationId) {
        QueryConversationMessagesResponse response = projectConversationApplicationService.queryMessages(conversationId);
        return ApiResponse.ok(response);
    }

    /**
     * appendMessage（追加会话消息）接口用于向一次会话中手动写入消息。
     *
     * @param conversationId 会话编号
     * @param request 追加消息请求
     * @return 已保存消息响应
     */
    @PostMapping("/{conversationId}/messages")
    public ApiResponse<ConversationMessageResponse> appendMessage(@PathVariable String conversationId, @Valid @RequestBody AppendConversationMessageRequest request) {
        ConversationMessageResponse response = projectConversationApplicationService.appendMessage(conversationId, request);
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable String conversationId) {
        projectConversationApplicationService.deleteConversation(conversationId);
        return ApiResponse.ok(null);
    }

    /**
     * updateConversation（更新会话）接口用于修改会话标题。
     */
    @PatchMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> updateConversation(
            @PathVariable String conversationId,
            @Valid @RequestBody UpdateConversationRequest request) {
        return ApiResponse.ok(projectConversationApplicationService.updateConversation(conversationId, request));
    }

    /**
     * route（会话路由）接口用于根据用户本轮消息自动选择创作、查询状态、查询成本或质量复盘流程。
     *
     * @param request 会话路由请求
     * @return 会话路由响应
     */
    @PostMapping("/route")
    public ApiResponse<ConversationRouteResponse> route(@RequestBody ConversationRouteRequest request) {
        ConversationRouteResponse response = conversationRouteApplicationService.route(request);
        return ApiResponse.ok(response);
    }
}
