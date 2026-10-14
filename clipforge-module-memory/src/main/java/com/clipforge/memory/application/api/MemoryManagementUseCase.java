package com.clipforge.memory.application.api;

import com.clipforge.contract.knowledge.CreationHistoryMemoryResponse;
import com.clipforge.contract.knowledge.DisableMemoryResponse;
import com.clipforge.contract.knowledge.ProjectMemoryResponse;
import com.clipforge.contract.knowledge.UserPreferenceMemoryResponse;
import java.util.List;

/**
 * MemoryManagementUseCase（记忆管理用例）定义前端可调用的记忆查询和人工停用能力。
 *
 * <p>该接口属于 Memory 模块应用边界，Controller 面向该接口而不是 legacy 具体应用服务。
 */
public interface MemoryManagementUseCase {

    List<UserPreferenceMemoryResponse> listUserPreferenceMemories(String userId);

    DisableMemoryResponse disableUserPreferenceMemory(String memoryId);

    ProjectMemoryResponse queryProjectMemory(String projectId);

    DisableMemoryResponse disableProjectMemory(String memoryId);

    List<CreationHistoryMemoryResponse> listCreationHistoryMemories(String userId, String projectId, String platform, Integer limit);

    DisableMemoryResponse disableCreationHistoryMemory(String memoryId);
}
