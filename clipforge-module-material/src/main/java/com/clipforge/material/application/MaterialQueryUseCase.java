package com.clipforge.material.application;

import com.clipforge.contract.material.MaterialResponse;
import java.util.List;

/**
 * MaterialQueryUseCase（素材查询用例）。
 *
 * <p>该接口是素材模块对外暴露的读取边界，供接口层和创作编排层查询已经落库的素材元数据。
 */
public interface MaterialQueryUseCase {

    /**
     * listMaterials（查询素材列表）按用户、项目、会话、运行任务和素材类型读取素材集合。
     */
    List<MaterialResponse> listMaterials(String userId, String projectId, String conversationId, String runId, String materialType);

    /**
     * listMaterialsByIds（按编号查询素材列表）按用户选择顺序读取指定素材。
     */
    List<MaterialResponse> listMaterialsByIds(List<String> materialIds, String materialType);

    /**
     * queryMaterial（查询素材）按素材编号读取单个素材详情。
     */
    MaterialResponse queryMaterial(String materialId);
}
