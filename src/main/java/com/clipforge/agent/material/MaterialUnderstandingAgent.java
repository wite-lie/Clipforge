package com.clipforge.agent.material;

import com.clipforge.api.dto.MaterialResponse;
import com.clipforge.material.MaterialUnderstandingResult;
import java.util.List;

/**
 * MaterialUnderstandingAgent（素材理解智能体）。
 *
 * <p>该接口定义素材理解局部 Agent（智能体）的统一入口，负责把素材列表转换为创作链路可使用的素材理解结果。
 */
public interface MaterialUnderstandingAgent {

    /**
     * understand（理解素材）用于执行素材过滤、图片理解和素材摘要合并。
     *
     * @param runId 运行编号
     * @param materials 素材列表
     * @param instruction 理解指令
     * @return 素材理解结果
     */
    MaterialUnderstandingResult understand(String runId, List<MaterialResponse> materials, String instruction);
}
