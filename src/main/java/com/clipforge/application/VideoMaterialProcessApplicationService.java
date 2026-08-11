package com.clipforge.application;

import com.clipforge.api.dto.VideoAudioResponse;
import com.clipforge.api.dto.VideoFrameResponse;
import com.clipforge.api.dto.VideoMetadataResponse;
import com.clipforge.api.dto.VideoUnderstandingResponse;
import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.entity.MaterialProcessTaskEntity;
import com.clipforge.infra.persistence.entity.MaterialUnderstandingEntity;
import com.clipforge.infra.persistence.repository.MaterialProcessTaskRepository;
import com.clipforge.material.sampling.MaterialUnderstandingSampler;
import com.clipforge.task.TaskBizType;
import com.clipforge.task.TaskNextAction;
import com.clipforge.task.TaskPhase;
import com.clipforge.task.TaskStatusValue;
import com.clipforge.task.cache.MaterialProcessTaskStatus;
import com.clipforge.task.cache.MaterialProcessTaskStatusCache;
import com.clipforge.video.audio.AudioExtractStatus;
import com.clipforge.video.audio.SpeechRecognitionStatus;
import com.clipforge.video.frame.VideoFrameExtractStatus;
import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import com.clipforge.video.understanding.VideoFrameUnderstandingParallelProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * VideoMaterialProcessApplicationService（视频素材处理应用服务）。
 *
 * <p>该服务负责编排视频上传后的异步处理流水线：视频元数据读取、关键帧抽取、帧素材注册、帧图片理解、音频提取、
 * ASR（自动语音识别）转写和视频理解汇总，并把处理进度同步写入数据库和 Redis（缓存）。
 */
@Service
@Profile("db")
@RequiredArgsConstructor
public class VideoMaterialProcessApplicationService {

    /** processTaskRepository（处理任务仓储）用于读取和更新 cf_material_process_task（素材处理任务表）。 */
    private final MaterialProcessTaskRepository processTaskRepository;

    /** videoMetadataApplicationService（视频元数据应用服务）用于读取视频基础信息。 */
    private final VideoMetadataApplicationService videoMetadataApplicationService;

    /** videoFrameExtractionApplicationService（视频抽帧应用服务）用于抽取候选帧。 */
    private final VideoFrameExtractionApplicationService videoFrameExtractionApplicationService;

    /** videoFrameMaterialRegistrationApplicationService（视频帧素材注册应用服务）用于把候选帧注册成图片素材。 */
    private final VideoFrameMaterialRegistrationApplicationService videoFrameMaterialRegistrationApplicationService;

    /** imageUnderstandingProcessApplicationService（图片理解处理应用服务）用于理解视频候选帧图片。 */
    private final ImageUnderstandingProcessApplicationService imageUnderstandingProcessApplicationService;

    /** videoAudioExtractionApplicationService（视频音频提取应用服务）用于从视频中提取音频。 */
    private final VideoAudioExtractionApplicationService videoAudioExtractionApplicationService;

    /** videoAudioTranscriptionApplicationService（视频音频转写应用服务）用于执行 ASR（自动语音识别）。 */
    private final VideoAudioTranscriptionApplicationService videoAudioTranscriptionApplicationService;

    /** videoUnderstandingApplicationService（视频理解应用服务）用于汇总视频理解结果。 */
    private final VideoUnderstandingApplicationService videoUnderstandingApplicationService;

    /** frameUnderstandingParallelProperties（帧理解并行配置）用于控制单个视频内部多帧 VLM（视觉语言模型）理解并发。 */
    private final VideoFrameUnderstandingParallelProperties frameUnderstandingParallelProperties;

    /** materialUnderstandingSampler（素材理解抽样器）用于控制真正进入 VLM（视觉语言模型）的视频帧数量。 */
    private final MaterialUnderstandingSampler materialUnderstandingSampler;

    /** statusCache（状态缓存）用于刷新 Redis（缓存）中的任务状态。 */
    private final MaterialProcessTaskStatusCache statusCache;

    /** transactionTemplate（事务模板）用于把每次任务状态更新控制在短事务内。 */
    private final TransactionTemplate transactionTemplate;

    /**
     * process（处理）用于执行单个视频素材的完整异步处理流水线。
     *
     * @param processTaskId 处理任务编号
     * @param instruction 视频理解指令
     * @return 视频理解响应
     */
    public VideoUnderstandingResponse process(String processTaskId, String instruction) {
        // 读取处理任务，确保消息中的任务编号真实存在。
        MaterialProcessTaskEntity task = processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "素材处理任务不存在，processTaskId=" + processTaskId));
        try {
            // 更新任务进入视频元数据阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_METADATA, 10, "正在读取视频元数据", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 读取视频元数据。
            VideoMetadataResponse metadata = videoMetadataApplicationService.readMetadata(task.getMaterialId());
            // 校验元数据读取结果，损坏视频或不存在文件通常会在这里被识别。
            assertMetadataSucceeded(metadata);

            // 更新任务进入视频抽帧阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_FRAME_EXTRACT, 30, "正在抽取视频候选帧", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 抽取视频候选帧。
            List<VideoFrameResponse> frames = videoFrameExtractionApplicationService.extractFrames(task.getMaterialId());
            // 校验至少存在一张成功候选帧。
            assertHasSucceededFrame(frames);

            // 更新任务仍处于抽帧阶段，但说明变为帧素材注册。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_FRAME_EXTRACT, 45, "正在注册视频帧图片素材", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 把候选帧注册为图片素材，便于复用图片理解结果表。
            List<VideoFrameResponse> registeredFrames = videoFrameMaterialRegistrationApplicationService.registerFrameMaterials(task.getMaterialId());
            // 校验注册结果中存在可理解的帧素材编号。
            assertHasFrameMaterial(registeredFrames);

            // 更新任务进入帧图片理解阶段，复用已有 IMAGE_UNDERSTANDING（图片理解）阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.IMAGE_UNDERSTANDING, 60, "正在理解视频关键帧图片", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 逐张理解帧图片并保存到 cf_material_understanding（素材理解结果表）。
            understandFrameMaterials(task, registeredFrames, instruction);

            // 更新任务进入音频提取阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_AUDIO_EXTRACT, 72, "正在提取视频音频", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 提取音频，无音轨属于可接受分支，文件损坏或 FFmpeg（音视频处理工具）失败属于失败分支。
            VideoAudioResponse audio = videoAudioExtractionApplicationService.extractAudio(task.getMaterialId());
            // 校验音频提取分支。
            assertAudioExtractUsable(audio);

            // 更新任务进入 ASR（自动语音识别）阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_ASR, 82, "正在进行视频语音转写", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 执行 ASR（自动语音识别），无音轨、无人声、静音视频允许跳过。
            VideoAudioResponse transcribedAudio = videoAudioTranscriptionApplicationService.transcribe(task.getMaterialId());
            // 校验 ASR（自动语音识别）结果是否可用于后续汇总。
            assertSpeechRecognitionUsable(transcribedAudio);

            // 更新任务进入视频理解汇总阶段。
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_UNDERSTANDING, 92, "正在汇总视频理解结果", null, null, TaskNextAction.WAIT_PROCESSING, null);
            // 汇总视频理解结果并落库。
            VideoUnderstandingResponse understanding = videoUnderstandingApplicationService.understand(task.getMaterialId(), task.getRunId(), instruction);

            // 标记视频素材处理完成。
            updateTask(task, TaskStatusValue.SUCCEEDED, TaskPhase.READY_FOR_CREATION, 100, "视频处理完成，可用于创作", null, null, TaskNextAction.ALLOW_CREATION, LocalDateTime.now());
            // 返回视频理解结果。
            return understanding;
        } catch (RuntimeException exception) {
            // 失败时记录可重试状态，便于前端展示重试入口。
            markFailed(task, exception);
            // 继续抛出异常，让消费者或调用方能感知失败。
            throw exception;
        }
    }

    /**
     * understandFrameMaterials（理解帧图片素材）用于把视频候选帧图片逐张送入图片理解处理服务。
     *
     * @param task 视频处理任务
     * @param frames 已注册帧响应列表
     * @param instruction 理解指令
     */
    /**
     * processFrameBranchTask（处理抽帧图像分支子任务）用于执行视频抽帧、帧素材注册和帧图片理解。
     *
     * @param processTaskId 子任务编号
     * @param instruction 视频理解指令
     */
    public void processFrameBranchTask(String processTaskId, String instruction) {
        MaterialProcessTaskEntity task = processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频抽帧图像分支子任务不存在，processTaskId=" + processTaskId));
        try {
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_FRAME_BRANCH, 10, "正在处理视频抽帧图像分支", null, null, TaskNextAction.WAIT_PROCESSING, null);
            List<VideoFrameResponse> frames = videoFrameExtractionApplicationService.extractFrames(task.getMaterialId());
            assertHasSucceededFrame(frames);
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_FRAME_BRANCH, 35, "正在注册视频帧图片素材", null, null, TaskNextAction.WAIT_PROCESSING, null);
            List<VideoFrameResponse> registeredFrames = videoFrameMaterialRegistrationApplicationService.registerFrameMaterials(task.getMaterialId());
            assertHasFrameMaterial(registeredFrames);
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_FRAME_BRANCH, 65, "正在理解视频关键帧图片", null, null, TaskNextAction.WAIT_PROCESSING, null);
            understandFrameMaterials(task, registeredFrames, instruction);
            updateTask(task, TaskStatusValue.SUCCEEDED, TaskPhase.VIDEO_FRAME_BRANCH, 100, "视频抽帧图像分支处理完成", null, null, TaskNextAction.WAIT_PROCESSING, LocalDateTime.now());
        } catch (RuntimeException exception) {
            markFailed(task, exception);
            throw exception;
        }
    }

    /**
     * processAudioBranchTask（处理音频分支子任务）用于执行音频提取和 ASR（自动语音识别）。
     *
     * @param processTaskId 子任务编号
     * @param instruction 视频理解指令
     */
    public void processAudioBranchTask(String processTaskId, String instruction) {
        MaterialProcessTaskEntity task = processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频音频分支子任务不存在，processTaskId=" + processTaskId));
        try {
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_AUDIO_BRANCH, 20, "正在提取视频音频", null, null, TaskNextAction.WAIT_PROCESSING, null);
            VideoAudioResponse audio = videoAudioExtractionApplicationService.extractAudio(task.getMaterialId());
            assertAudioExtractUsable(audio);
            if (AudioExtractStatus.NO_AUDIO_STREAM.value().equals(audio.getAudioExtractStatus())) {
                updateTask(task, TaskStatusValue.SUCCEEDED, TaskPhase.VIDEO_AUDIO_BRANCH, 100, "视频无音轨，已跳过语音转写", null, null, TaskNextAction.WAIT_PROCESSING, LocalDateTime.now());
                return;
            }
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_AUDIO_BRANCH, 65, "正在进行视频语音转写", null, null, TaskNextAction.WAIT_PROCESSING, null);
            VideoAudioResponse transcribedAudio = videoAudioTranscriptionApplicationService.transcribe(task.getMaterialId());
            assertSpeechRecognitionUsable(transcribedAudio);
            updateTask(task, TaskStatusValue.SUCCEEDED, TaskPhase.VIDEO_AUDIO_BRANCH, 100, "视频音频分支处理完成", null, null, TaskNextAction.WAIT_PROCESSING, LocalDateTime.now());
        } catch (RuntimeException exception) {
            markFailed(task, exception);
            throw exception;
        }
    }

    /**
     * processSummaryTask（处理汇总子任务）用于读取图像分支和音频分支结果并生成视频理解摘要。
     *
     * @param processTaskId 子任务编号
     * @param instruction 视频理解指令
     * @return 视频理解响应
     */
    public VideoUnderstandingResponse processSummaryTask(String processTaskId, String instruction) {
        MaterialProcessTaskEntity task = processTaskRepository.findByProcessTaskId(processTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "视频汇总子任务不存在，processTaskId=" + processTaskId));
        try {
            updateTask(task, TaskStatusValue.PROCESSING, TaskPhase.VIDEO_SUMMARY, 50, "正在汇总视频理解结果", null, null, TaskNextAction.WAIT_PROCESSING, null);
            VideoUnderstandingResponse understanding = videoUnderstandingApplicationService.understand(task.getMaterialId(), task.getRunId(), instruction);
            updateTask(task, TaskStatusValue.SUCCEEDED, TaskPhase.VIDEO_SUMMARY, 100, "视频理解汇总完成", null, null, TaskNextAction.WAIT_PROCESSING, LocalDateTime.now());
            return understanding;
        } catch (RuntimeException exception) {
            markFailed(task, exception);
            throw exception;
        }
    }

    private void understandFrameMaterials(MaterialProcessTaskEntity task, List<VideoFrameResponse> frames, String instruction) {
        // 先筛选出已经成功注册为 IMAGE（图片）素材的候选帧。
        List<VideoFrameResponse> frameMaterials = frames == null ? List.of() : frames.stream()
                // 只保留存在帧素材编号的记录。
                .filter(frame -> frame.getFrameMaterialId() != null && !frame.getFrameMaterialId().isBlank())
                // 收集为列表，便于后续计算并发数。
                .toList();
        // 没有可理解的帧素材时直接抛错，避免视频理解缺少视觉上下文。
        if (frameMaterials.isEmpty()) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_READY, "视频没有可用于理解的帧图片素材，materialId=" + task.getMaterialId());
        }
        // 在真正调用 VLM（视觉语言模型）前做抽样，降低多帧视频理解的耗时和 Token（词元）成本。
        List<VideoFrameResponse> selectedFrameMaterials = materialUnderstandingSampler.selectVideoFrames(frameMaterials);
        // 抽样后没有可用帧时终止流程，避免生成缺失视觉证据的视频理解结果。
        if (selectedFrameMaterials.isEmpty()) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_READY, "视频抽样后没有可用于理解的帧图片素材，materialId=" + task.getMaterialId());
        }
        // 配置关闭或抽样后只有一帧时使用串行逻辑，减少线程池开销。
        if (!frameUnderstandingParallelProperties.isEnabled() || selectedFrameMaterials.size() == 1) {
            understandFrameMaterialsSerial(task, selectedFrameMaterials, instruction);
            return;
        }
        // 多帧场景进入受控并行理解。
        understandFrameMaterialsParallel(task, selectedFrameMaterials, instruction);
    }

    /**
     * understandFrameMaterialsSerial（串行理解帧图片素材）用于配置关闭或单帧场景。
     *
     * @param task 视频处理任务
     * @param frames 帧素材列表
     * @param instruction 理解指令
     */
    private void understandFrameMaterialsSerial(MaterialProcessTaskEntity task, List<VideoFrameResponse> frames, String instruction) {
        // 遍历帧素材。
        for (VideoFrameResponse frame : frames) {
            // 理解单帧并校验结果。
            understandOneFrameMaterial(task, frame, instruction);
        }
    }

    /**
     * understandFrameMaterialsParallel（并行理解帧图片素材）用于提高单个视频内部多帧 VLM（视觉语言模型）理解速度。
     *
     * @param task 视频处理任务
     * @param frames 帧素材列表
     * @param instruction 理解指令
     */
    private void understandFrameMaterialsParallel(MaterialProcessTaskEntity task, List<VideoFrameResponse> frames, String instruction) {
        // 计算本次视频内部实际并发数，不能超过帧数量和配置上限。
        int threadCount = Math.min(frames.size(), Math.max(1, frameUnderstandingParallelProperties.getMaxConcurrent()));
        // 创建本次视频专用线程池，避免把慢 VLM（视觉语言模型）调用放入公共线程池造成干扰。
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            // 构造每一帧的理解任务。
            List<Callable<MaterialUnderstandingEntity>> callables = new ArrayList<>();
            // 遍历帧素材并生成 Callable（可调用任务）。
            for (VideoFrameResponse frame : frames) {
                callables.add(() -> understandOneFrameMaterial(task, frame, instruction));
            }
            // 在总超时时间内等待全部帧理解完成。
            List<Future<MaterialUnderstandingEntity>> futures = executorService.invokeAll(
                    callables,
                    Math.max(1L, frameUnderstandingParallelProperties.getTimeoutSeconds()),
                    TimeUnit.SECONDS);
            // 检查每个帧任务是否完成或失败。
            for (int index = 0; index < futures.size(); index++) {
                Future<MaterialUnderstandingEntity> future = futures.get(index);
                VideoFrameResponse frame = frames.get(index);
                if (future.isCancelled()) {
                    throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "视频帧图片理解超时，frameMaterialId=" + frame.getFrameMaterialId());
                }
                // 读取结果，触发 ExecutionException（执行异常）暴露真实失败原因。
                future.get(1, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException exception) {
            // 恢复线程中断标记，避免吞掉上层取消信号。
            Thread.currentThread().interrupt();
            // 包装为业务异常。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频帧并行理解被中断，materialId=" + task.getMaterialId(), exception);
        } catch (ExecutionException exception) {
            // 线程内部抛出的业务异常优先原样抛出。
            if (exception.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            // 其他异常包装为模型调用错误。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "视频帧并行理解失败：" + exception.getCause().getMessage(), exception.getCause());
        } catch (TimeoutException exception) {
            // 理论上 invokeAll 已处理总超时，这里兜底处理 get 的极短等待。
            throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "视频帧并行理解结果读取超时，materialId=" + task.getMaterialId(), exception);
        } finally {
            // 关闭本次视频专用线程池，避免线程泄漏。
            executorService.shutdownNow();
        }
    }

    /**
     * understandOneFrameMaterial（理解单个帧图片素材）用于复用图片理解处理服务。
     *
     * @param task 视频处理任务
     * @param frame 帧响应
     * @param instruction 理解指令
     * @return 素材理解结果
     */
    private MaterialUnderstandingEntity understandOneFrameMaterial(MaterialProcessTaskEntity task, VideoFrameResponse frame, String instruction) {
        // 调用图片理解处理服务，内部会优先复用已保存结果，未命中才调用 VLM（视觉语言模型）。
        MaterialUnderstandingEntity understanding = imageUnderstandingProcessApplicationService.processMaterial(frame.getFrameMaterialId(), task.getRunId(), instruction);
        // 如果理解结果为空或内容摘要为空，说明该帧无法贡献有效视觉信息。
        if (understanding == null || understanding.getContentSummary() == null || understanding.getContentSummary().isBlank()) {
            // 抛出异常，避免视频理解拿到空视觉上下文。
            throw new BusinessException(ErrorCode.MATERIAL_NOT_READY, "视频帧图片理解结果为空，frameMaterialId=" + frame.getFrameMaterialId());
        }
        // 返回单帧理解结果。
        return understanding;
    }

    /**
     * assertMetadataSucceeded（断言元数据成功）用于识别视频文件不存在、损坏或不可解析等异常。
     *
     * @param metadata 视频元数据响应
     */
    private void assertMetadataSucceeded(VideoMetadataResponse metadata) {
        // 元数据为空说明读取链路异常。
        if (metadata == null) {
            // 抛出内部错误。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频元数据读取结果为空");
        }
        // 元数据读取失败时终止后续流程。
        if (!VideoMetadataProbeStatus.SUCCEEDED.value().equals(metadata.getProbeStatus())) {
            // 抛出错误请求异常，错误消息中包含 FFprobe（视频探测工具）给出的原因。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频元数据读取失败，probeStatus=" + metadata.getProbeStatus() + "，原因：" + metadata.getProbeErrorMessage());
        }
    }

    /**
     * assertHasSucceededFrame（断言存在成功候选帧）用于识别视频损坏、无可抽取画面或 FFmpeg（音视频处理工具）失败。
     *
     * @param frames 候选帧响应列表
     */
    private void assertHasSucceededFrame(List<VideoFrameResponse> frames) {
        // 没有候选帧说明视频无法贡献视觉上下文。
        if (frames == null || frames.isEmpty()) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频没有抽取到候选帧");
        }
        // 至少需要一张抽取成功的候选帧。
        boolean hasSucceeded = frames.stream().anyMatch(frame -> VideoFrameExtractStatus.SUCCEEDED.value().equals(frame.getExtractStatus()));
        // 如果没有成功帧，则终止处理。
        if (!hasSucceeded) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频候选帧全部抽取失败，可能是视频文件损坏或 FFmpeg 配置异常");
        }
    }

    /**
     * assertHasFrameMaterial（断言存在帧素材）用于确保候选帧已经注册成图片素材。
     *
     * @param frames 候选帧响应列表
     */
    private void assertHasFrameMaterial(List<VideoFrameResponse> frames) {
        // 至少需要一个帧图片素材编号。
        boolean hasFrameMaterial = frames != null && frames.stream().anyMatch(frame -> frame.getFrameMaterialId() != null && !frame.getFrameMaterialId().isBlank());
        // 如果没有帧图片素材编号，则无法执行图片理解。
        if (!hasFrameMaterial) {
            // 抛出错误请求异常。
            throw new BusinessException(ErrorCode.BAD_REQUEST, "视频候选帧没有成功注册为图片素材");
        }
    }

    /**
     * assertAudioExtractUsable（断言音频提取可用）用于区分可接受跳过和真正失败。
     *
     * @param audio 视频音频响应
     */
    private void assertAudioExtractUsable(VideoAudioResponse audio) {
        // 音频响应为空说明提取链路异常。
        if (audio == null) {
            // 抛出内部错误。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频音频提取结果为空");
        }
        // 没有音轨是正常分支，可以继续做视频理解汇总。
        if (AudioExtractStatus.NO_AUDIO_STREAM.value().equals(audio.getAudioExtractStatus())) {
            // 直接返回。
            return;
        }
        // 提取成功是正常分支。
        if (AudioExtractStatus.SUCCEEDED.value().equals(audio.getAudioExtractStatus())) {
            // 直接返回。
            return;
        }
        // 其他状态视为不可继续的处理失败。
        throw new BusinessException(ErrorCode.BAD_REQUEST, "视频音频提取失败，audioExtractStatus=" + audio.getAudioExtractStatus() + "，原因：" + audio.getErrorMessage());
    }

    /**
     * assertSpeechRecognitionUsable（断言语音识别可用）用于区分无音轨、无人声和真正失败。
     *
     * @param audio 视频音频响应
     */
    private void assertSpeechRecognitionUsable(VideoAudioResponse audio) {
        // 音频响应为空说明 ASR（自动语音识别）链路异常。
        if (audio == null) {
            // 抛出内部错误。
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "视频语音转写结果为空");
        }
        // ASR（自动语音识别）成功、跳过、无人声都允许继续，因为视频仍然可以依靠视觉信息创作。
        if (SpeechRecognitionStatus.SUCCEEDED.value().equals(audio.getSpeechRecognitionStatus())
                || SpeechRecognitionStatus.SKIPPED.value().equals(audio.getSpeechRecognitionStatus())
                || SpeechRecognitionStatus.NO_SPEECH_DETECTED.value().equals(audio.getSpeechRecognitionStatus())) {
            // 直接返回。
            return;
        }
        // 其他状态视为语音识别失败。
        throw new BusinessException(ErrorCode.MODEL_CALL_ERROR, "视频语音转写失败，speechRecognitionStatus=" + audio.getSpeechRecognitionStatus() + "，原因：" + audio.getErrorMessage());
    }

    /**
     * markFailed（标记失败）用于把处理任务更新为可重试失败状态。
     *
     * @param task 处理任务实体
     * @param exception 异常
     */
    private void markFailed(MaterialProcessTaskEntity task, RuntimeException exception) {
        // 解析业务错误码。
        String errorCode = exception instanceof BusinessException businessException ? businessException.getErrorCode().getCode() : ErrorCode.INTERNAL_ERROR.getCode();
        // 更新任务为失败。
        updateTask(task, TaskStatusValue.FAILED, TaskPhase.FAILED, 100, "视频处理失败", errorCode, exception.getMessage(), TaskNextAction.RETRY_PROCESSING, LocalDateTime.now());
    }

    /**
     * updateTask（更新任务）用于同步数据库和 Redis（缓存）中的处理状态。
     *
     * @param task 处理任务实体
     * @param status 状态
     * @param phase 阶段
     * @param progressPercent 进度百分比
     * @param message 状态说明
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param nextAction 下一步动作
     * @param endedAt 结束时间
     */
    private void updateTask(MaterialProcessTaskEntity task, TaskStatusValue status, TaskPhase phase, Integer progressPercent, String message,
                            String errorCode, String errorMessage, TaskNextAction nextAction, LocalDateTime endedAt) {
        // 获取当前时间。
        LocalDateTime now = LocalDateTime.now();
        // 第一次进入处理中时记录开始时间。
        if (task.getStartedAt() == null && status == TaskStatusValue.PROCESSING) {
            // 设置开始时间。
            task.setStartedAt(now);
        }
        // 写入状态字段。
        task.setStatus(status.value());
        // 写入阶段字段。
        task.setPhase(phase.value());
        // 写入进度。
        task.setProgressPercent(progressPercent);
        // 写入人类可读消息。
        task.setMessage(message);
        // 写入错误码。
        task.setErrorCode(errorCode);
        // 写入错误消息。
        task.setErrorMessage(errorMessage);
        // 写入下一步动作。
        task.setNextAction(nextAction.value());
        // 失败时允许重试。
        task.setCanRetry(status == TaskStatusValue.FAILED ? 1 : 0);
        // 写入状态更新时间。
        task.setUpdatedAt(now);
        // 写入结束时间。
        task.setEndedAt(endedAt);
        // 写入数据库更新时间。
        task.setUpdateTime(now);
        // 使用短事务更新任务表。
        transactionTemplate.executeWithoutResult(transactionStatus -> processTaskRepository.update(task));
        // 刷新 Redis（缓存）中的状态。
        statusCache.save(toCacheStatus(task));
    }

    /**
     * toCacheStatus（转缓存状态）用于把处理任务实体转换为 Redis（缓存）状态对象。
     *
     * @param task 处理任务实体
     * @return 缓存状态
     */
    private MaterialProcessTaskStatus toCacheStatus(MaterialProcessTaskEntity task) {
        // 使用 Builder（建造者模式）组装缓存状态对象。
        return MaterialProcessTaskStatus.builder()
                // 设置任务编号。
                .taskId(task.getProcessTaskId())
                // 设置任务类型。
                .taskType(task.getTaskType())
                // 设置业务编号。
                .bizId(task.getMaterialId())
                // 设置业务类型。
                .bizType(TaskBizType.MATERIAL.value())
                // 设置任务状态。
                .status(task.getStatus())
                // 设置处理阶段。
                .phase(task.getPhase())
                // 设置进度百分比。
                .progressPercent(task.getProgressPercent())
                // 设置状态说明。
                .message(task.getMessage())
                // 设置错误码。
                .errorCode(task.getErrorCode())
                // 设置错误消息。
                .errorMessage(task.getErrorMessage())
                // 设置重试次数。
                .retryCount(task.getRetryCount())
                // 设置是否可重试。
                .canRetry(task.getCanRetry() != null && task.getCanRetry() == 1)
                // 设置下一步动作。
                .nextAction(task.getNextAction())
                // 设置开始时间。
                .startedAt(task.getStartedAt())
                // 设置更新时间。
                .updatedAt(task.getUpdatedAt())
                // 设置结束时间。
                .endedAt(task.getEndedAt())
                // 构建缓存状态。
                .build();
    }
}
