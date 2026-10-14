package com.clipforge.server.api;

import com.clipforge.contract.knowledge.VideoUnderstandingResponse;
import com.clipforge.application.VideoUnderstandingApplicationService;
import com.clipforge.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * VideoUnderstandingController（视频理解控制器）。
 *
 * <p>该控制器提供视频理解执行和结果查询接口。
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoUnderstandingController {

    /** videoUnderstandingApplicationService（视频理解应用服务）负责执行和查询视频理解结果。 */
    private final VideoUnderstandingApplicationService videoUnderstandingApplicationService;

    /**
     * understandVideo（理解视频）用于触发视频理解局部 Agent（智能体）。
     *
     * @param materialId 视频素材编号
     * @param runId 可选运行编号
     * @param instruction 可选理解指令
     * @return 视频理解响应
     */
    @PostMapping("/{materialId}/understand")
    public ApiResponse<VideoUnderstandingResponse> understandVideo(@PathVariable String materialId,
                                                                    @RequestParam(required = false) String runId,
                                                                    @RequestParam(required = false) String instruction) {
        // 调用应用服务执行视频理解。
        VideoUnderstandingResponse response = videoUnderstandingApplicationService.understand(materialId, runId, instruction);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * queryUnderstanding（查询视频理解）用于读取已经保存的视频理解结果。
     *
     * @param materialId 视频素材编号
     * @return 视频理解响应
     */
    @GetMapping("/{materialId}/understanding")
    public ApiResponse<VideoUnderstandingResponse> queryUnderstanding(@PathVariable String materialId) {
        // 调用应用服务查询视频理解结果。
        VideoUnderstandingResponse response = videoUnderstandingApplicationService.queryUnderstanding(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }
}
