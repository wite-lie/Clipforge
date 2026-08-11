package com.clipforge.api;

import com.clipforge.api.dto.VideoAudioResponse;
import com.clipforge.api.dto.VideoSubtitleSegmentResponse;
import com.clipforge.application.VideoAudioExtractionApplicationService;
import com.clipforge.application.VideoAudioTranscriptionApplicationService;
import com.clipforge.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VideoAudioController（视频音频控制器）。
 *
 * <p>提供视频音频提取、音频记录查询、ASR（自动语音识别）转写和字幕片段查询接口。
 */
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoAudioController {

    private final VideoAudioExtractionApplicationService videoAudioExtractionApplicationService;
    private final VideoAudioTranscriptionApplicationService videoAudioTranscriptionApplicationService;

    /**
     * extractAudio（提取音频）用于触发 FFmpeg（音视频处理工具）从视频中提取 wav（波形音频文件）。
     */
    @PostMapping("/{materialId}/audio/extract")
    public ApiResponse<VideoAudioResponse> extractAudio(@PathVariable String materialId) {
        return ApiResponse.ok(videoAudioExtractionApplicationService.extractAudio(materialId));
    }

    /**
     * queryAudio（查询音频）用于读取已经保存的视频音频记录。
     */
    @GetMapping("/{materialId}/audio")
    public ApiResponse<VideoAudioResponse> queryAudio(@PathVariable String materialId) {
        return ApiResponse.ok(videoAudioExtractionApplicationService.queryAudio(materialId));
    }

    /**
     * transcribeAudio（转写音频）用于触发 ASR（自动语音识别）生成转写文本和字幕片段。
     */
    @PostMapping("/{materialId}/audio/transcribe")
    public ApiResponse<VideoAudioResponse> transcribeAudio(@PathVariable String materialId) {
        return ApiResponse.ok(videoAudioTranscriptionApplicationService.transcribe(materialId));
    }

    /**
     * listSubtitleSegments（查询字幕片段）用于读取带时间戳的 ASR（自动语音识别）片段。
     */
    @GetMapping("/{materialId}/audio/subtitles")
    public ApiResponse<List<VideoSubtitleSegmentResponse>> listSubtitleSegments(@PathVariable String materialId) {
        return ApiResponse.ok(videoAudioTranscriptionApplicationService.listSubtitleSegments(materialId));
    }
}
