package com.clipforge.application;

import com.clipforge.contract.knowledge.AppendConversationMessageRequest;
import com.clipforge.contract.knowledge.ConversationMessageResponse;
import com.clipforge.contract.knowledge.ConversationResponse;
import com.clipforge.contract.knowledge.CreateConversationRequest;
import com.clipforge.contract.knowledge.UpdateConversationRequest;
import com.clipforge.contract.creation.CreatePlanRequest;
import com.clipforge.contract.knowledge.CreateProjectRequest;
import com.clipforge.contract.knowledge.ProjectResponse;
import com.clipforge.contract.knowledge.QueryConversationMessagesResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.memory.infrastructure.persistence.entity.ConversationEntity;
import com.clipforge.memory.infrastructure.persistence.entity.ConversationMessageEntity;
import com.clipforge.memory.infrastructure.persistence.entity.ProjectEntity;
import com.clipforge.memory.infrastructure.persistence.repository.ConversationMessageRepository;
import com.clipforge.memory.infrastructure.persistence.repository.ConversationRepository;
import com.clipforge.memory.infrastructure.persistence.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * ProjectConversationApplicationService（项目会话应用服务）。
 *
 * <p>负责 Project（项目）、Conversation（会话）和 Message（消息）的应用层编排。
 */
@Service
@RequiredArgsConstructor
public class ProjectConversationApplicationService {

    /** 默认项目用于承接没有显式项目编号的直接创作请求。 */
    private static final String DEFAULT_PROJECT_NAME = "默认项目";

    /** 默认项目说明。 */
    private static final String DEFAULT_PROJECT_DESCRIPTION = "系统自动创建，用于保存直接对话产生的创作记录";

    private final ObjectProvider<ProjectRepository> projectRepositoryProvider;

    private final ObjectProvider<ConversationRepository> conversationRepositoryProvider;

    private final ObjectProvider<ConversationMessageRepository> conversationMessageRepositoryProvider;

    public ProjectResponse createProject(CreateProjectRequest request) {
        ProjectEntity entity = requiredProjectRepository().create(request.getUserId(), request.getName(), request.getDescription());
        return toProjectResponse(entity);
    }

    public ProjectResponse queryProject(String projectId) {
        ProjectEntity entity = requiredProjectEntity(projectId);
        return toProjectResponse(entity);
    }

    public List<ProjectResponse> listProjects(String userId) {
        return requiredProjectRepository().listByUserId(userId).stream()
                .map(this::toProjectResponse)
                .toList();
    }

    public ConversationResponse createConversation(String projectId, CreateConversationRequest request) {
        queryProject(projectId);
        ConversationEntity entity = requiredConversationRepository().create(projectId, request.getUserId(), request.getTitle());
        return toConversationResponse(entity);
    }

    /**
     * 解析一次创作请求最终归属到哪个项目和会话。
     */
    public ConversationResolution resolveCreationConversation(CreatePlanRequest request) {
        if (request == null) {
            return ConversationResolution.builder().build();
        }

        if (hasText(request.getConversationId())) {
            ConversationEntity conversationEntity = requiredConversation(request.getConversationId());
            validateProjectMatchesConversation(request.getProjectId(), conversationEntity);
            return ConversationResolution.builder()
                    .projectId(conversationEntity.getProjectId())
                    .conversationId(conversationEntity.getConversationId())
                    .autoCreatedProject(false)
                    .autoCreatedConversation(false)
                    .build();
        }

        if (hasText(request.getProjectId())) {
            ProjectEntity projectEntity = requiredProjectEntity(request.getProjectId());
            String userId = hasText(request.getUserId()) ? request.getUserId() : projectEntity.getUserId();
            ConversationEntity conversationEntity = requiredConversationRepository()
                    .create(projectEntity.getProjectId(), userId, buildConversationTitle(request.getGoal()));
            return ConversationResolution.builder()
                    .projectId(projectEntity.getProjectId())
                    .conversationId(conversationEntity.getConversationId())
                    .autoCreatedProject(false)
                    .autoCreatedConversation(true)
                    .build();
        }

        if (!hasText(request.getUserId())) {
            return ConversationResolution.builder().build();
        }

        DefaultProjectResolution defaultProjectResolution = getOrCreateDefaultProject(request.getUserId());
        ConversationEntity conversationEntity = requiredConversationRepository()
                .create(defaultProjectResolution.projectEntity().getProjectId(), request.getUserId(), buildConversationTitle(request.getGoal()));
        return ConversationResolution.builder()
                .projectId(defaultProjectResolution.projectEntity().getProjectId())
                .conversationId(conversationEntity.getConversationId())
                .autoCreatedProject(defaultProjectResolution.created())
                .autoCreatedConversation(true)
                .build();
    }

    public List<ConversationResponse> listConversations(String projectId) {
        queryProject(projectId);
        return requiredConversationRepository().listByProjectId(projectId).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    /**
     * updateConversation（更新会话）用于修改用户可见的会话标题。
     */
    public ConversationResponse updateConversation(String conversationId, UpdateConversationRequest request) {
        ConversationEntity existing = requiredConversation(conversationId);
        String title = request == null || request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "会话标题不能为空");
        }
        requiredConversationRepository().updateTitle(conversationId, title);
        existing.setTitle(title);
        return toConversationResponse(existing);
    }

    /**
     * 逻辑删除会话，前端不再展示该会话，历史数据仍保留在数据库中。
     */
    public void deleteConversation(String conversationId) {
        requiredConversation(conversationId);
        requiredConversationRepository().logicalDeleteByConversationId(conversationId);
    }

    public QueryConversationMessagesResponse queryMessages(String conversationId) {
        requiredConversation(conversationId);
        List<ConversationMessageResponse> messages = requiredConversationMessageRepository().listByConversationId(conversationId).stream()
                .map(this::toConversationMessageResponse)
                .toList();
        return QueryConversationMessagesResponse.builder()
                .conversationId(conversationId)
                .messages(messages)
                .build();
    }

    public ConversationMessageResponse appendMessage(String conversationId, String runId, String role, String content) {
        requiredConversation(conversationId);
        String normalizedRole = validateMessageRole(role);
        ConversationMessageEntity entity = requiredConversationMessageRepository().append(conversationId, runId, normalizedRole, content);
        return toConversationMessageResponse(entity);
    }

    public ConversationMessageResponse appendMessage(String conversationId, AppendConversationMessageRequest request) {
        return appendMessage(conversationId, request.getRunId(), request.getRole(), request.getContent());
    }

    public ConversationEntity requiredConversation(String conversationId) {
        return requiredConversationRepository().findByConversationId(conversationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到会话，conversationId=" + conversationId));
    }

    public ProjectEntity requiredProjectEntity(String projectId) {
        return requiredProjectRepository().findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "未查询到项目，projectId=" + projectId));
    }

    private DefaultProjectResolution getOrCreateDefaultProject(String userId) {
        ProjectEntity existingDefaultProject = requiredProjectRepository().listByUserId(userId).stream()
                .filter(project -> DEFAULT_PROJECT_NAME.equals(project.getName()))
                .findFirst()
                .orElse(null);
        if (existingDefaultProject != null) {
            return new DefaultProjectResolution(existingDefaultProject, false);
        }
        ProjectEntity createdProject = requiredProjectRepository().create(userId, DEFAULT_PROJECT_NAME, DEFAULT_PROJECT_DESCRIPTION);
        return new DefaultProjectResolution(createdProject, true);
    }

    private void validateProjectMatchesConversation(String requestProjectId, ConversationEntity conversationEntity) {
        if (!hasText(requestProjectId)) {
            return;
        }
        if (!requestProjectId.equals(conversationEntity.getProjectId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "conversationId 所属项目与 projectId 不一致");
        }
    }

    private String buildConversationTitle(String goal) {
        if (!hasText(goal)) {
            return "新的创作方案";
        }
        String trimmedGoal = goal.trim();
        String shortGoal = trimmedGoal.length() > 48 ? trimmedGoal.substring(0, 48) : trimmedGoal;
        return "创作：" + shortGoal;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ProjectRepository requiredProjectRepository() {
        ProjectRepository repository = projectRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "项目接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private ConversationRepository requiredConversationRepository() {
        ConversationRepository repository = conversationRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "会话接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private ConversationMessageRepository requiredConversationMessageRepository() {
        ConversationMessageRepository repository = conversationMessageRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new BusinessException(ErrorCode.PERSISTENCE_DISABLED, "会话消息接口需要启用 db Profile（数据库配置环境）");
        }
        return repository;
    }

    private String validateMessageRole(String role) {
        if (role == null || role.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息角色不能为空");
        }
        String normalizedRole = role.trim().toLowerCase();
        boolean allowed = List.of("system", "user", "assistant", "tool").contains(normalizedRole);
        if (!allowed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "消息角色只能是 system、user、assistant 或 tool");
        }
        return normalizedRole;
    }

    private ProjectResponse toProjectResponse(ProjectEntity entity) {
        return ProjectResponse.builder()
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    private ConversationResponse toConversationResponse(ConversationEntity entity) {
        return ConversationResponse.builder()
                .conversationId(entity.getConversationId())
                .projectId(entity.getProjectId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .build();
    }

    private ConversationMessageResponse toConversationMessageResponse(ConversationMessageEntity entity) {
        return ConversationMessageResponse.builder()
                .messageId(entity.getMessageId())
                .conversationId(entity.getConversationId())
                .runId(entity.getRunId())
                .role(entity.getRole())
                .content(entity.getContent())
                .build();
    }

    private record DefaultProjectResolution(ProjectEntity projectEntity, boolean created) {
    }
}
