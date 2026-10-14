package com.clipforge.server.api;

import com.clipforge.contract.material.VideoFrameResponse;
import com.clipforge.application.VideoFrameExtractionApplicationService;
import com.clipforge.application.VideoFrameMaterialRegistrationApplicationService;
import com.clipforge.common.ApiResponse;
import com.clipforge.material.application.VideoFrameQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VideoFrameController（视频候选帧控制器）。
 *
 * <p>该控制器提供视频候选帧抽取和查询接口。
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoFrameController {

    /** videoFrameExtractionApplicationService（视频帧抽取应用服务）负责编排候选帧抽取和查询。 */
    private final VideoFrameExtractionApplicationService videoFrameExtractionApplicationService;

    /** videoFrameMaterialRegistrationApplicationService（视频帧素材注册应用服务）负责把候选帧注册成图片素材。 */
    private final VideoFrameMaterialRegistrationApplicationService videoFrameMaterialRegistrationApplicationService;

    /** videoFrameQueryUseCase（视频帧查询用例）负责读取已保存候选帧。 */
    private final VideoFrameQueryUseCase videoFrameQueryUseCase;

    /**
     * extractFrames（抽取帧）用于触发 FFmpeg（音视频处理工具）抽取候选帧。
     *
     * @param materialId 素材编号
     * @return 候选帧响应列表
     */
    @PostMapping("/{materialId}/frames/extract")
    public ApiResponse<List<VideoFrameResponse>> extractFrames(@PathVariable String materialId) {
        // 调用应用服务抽取候选帧。
        videoFrameExtractionApplicationService.extractFrames(materialId);
        // 抽帧完成后自动把成功候选帧注册为 IMAGE（图片）素材，便于后续图片理解链路复用。
        List<VideoFrameResponse> response = videoFrameMaterialRegistrationApplicationService.registerFrameMaterials(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * registerFrameMaterials（注册帧图片素材）用于只把已有候选帧注册成图片素材，不重新执行抽帧。
     *
     * @param materialId 素材编号
     * @return 候选帧响应列表
     */
    @PostMapping("/{materialId}/frames/register-materials")
    public ApiResponse<List<VideoFrameResponse>> registerFrameMaterials(@PathVariable String materialId) {
        // 调用应用服务注册帧图片素材。
        List<VideoFrameResponse> response = videoFrameMaterialRegistrationApplicationService.registerFrameMaterials(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * listFrames（查询帧列表）用于读取已经保存的视频候选帧。
     *
     * @param materialId 素材编号
     * @return 候选帧响应列表
     */
    @GetMapping("/{materialId}/frames")
    public ApiResponse<List<VideoFrameResponse>> listFrames(@PathVariable String materialId) {
        // 调用应用服务查询候选帧。
        List<VideoFrameResponse> response = videoFrameQueryUseCase.listFrames(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }
}
