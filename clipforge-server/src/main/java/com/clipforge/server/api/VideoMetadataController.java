package com.clipforge.server.api;

import com.clipforge.contract.material.VideoMetadataResponse;
import com.clipforge.application.VideoMetadataApplicationService;
import com.clipforge.common.ApiResponse;
import com.clipforge.material.application.VideoMetadataQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VideoMetadataController（视频元数据控制器）。
 *
 * <p>该控制器提供视频元数据读取和查询接口。
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoMetadataController {

    /** videoMetadataApplicationService（视频元数据应用服务）负责编排视频元数据读取和查询。 */
    private final VideoMetadataApplicationService videoMetadataApplicationService;

    /** videoMetadataQueryUseCase（视频元数据查询用例）负责读取已保存元数据。 */
    private final VideoMetadataQueryUseCase videoMetadataQueryUseCase;

    /**
     * readMetadata（读取元数据）用于触发 FFprobe（视频探测工具）读取并保存视频元数据。
     *
     * @param materialId 素材编号
     * @return 视频元数据响应
     */
    @PostMapping("/{materialId}/metadata/read")
    public ApiResponse<VideoMetadataResponse> readMetadata(@PathVariable String materialId) {
        // 调用应用服务读取视频元数据。
        VideoMetadataResponse response = videoMetadataApplicationService.readMetadata(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }

    /**
     * queryMetadata（查询元数据）用于读取已经保存的视频元数据。
     *
     * @param materialId 素材编号
     * @return 视频元数据响应
     */
    @GetMapping("/{materialId}/metadata")
    public ApiResponse<VideoMetadataResponse> queryMetadata(@PathVariable String materialId) {
        // 调用应用服务查询视频元数据。
        VideoMetadataResponse response = videoMetadataQueryUseCase.queryMetadata(materialId);
        // 返回统一成功响应。
        return ApiResponse.ok(response);
    }
}
