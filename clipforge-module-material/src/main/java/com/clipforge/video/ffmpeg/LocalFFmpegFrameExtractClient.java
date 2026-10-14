package com.clipforge.video.ffmpeg;

import com.clipforge.video.frame.VideoFrameExtractStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LocalFFmpegFrameExtractClient（本地 FFmpeg 帧抽取客户端）。
 *
 * <p>该实现通过 ProcessBuilder（进程构建器）调用本机 FFmpeg（音视频处理工具）抽取单张候选帧图片。
 */
@Component
@RequiredArgsConstructor
public class LocalFFmpegFrameExtractClient implements FFmpegFrameExtractClient {

    /** ffmpegProperties（音视频处理配置）提供 FFmpeg 路径和抽帧超时时间。 */
    private final FFmpegProperties ffmpegProperties;

    /**
     * extractFrame（抽取帧）用于按指定时间点生成一张候选帧图片。
     *
     * @param request 帧抽取请求
     * @return 帧抽取结果
     */
    @Override
    public FrameExtractResult extractFrame(FrameExtractRequest request) {
        // 校验抽帧请求。
        validateRequest(request);
        try {
            // 创建输出目录。
            Files.createDirectories(request.getOutputPath().getParent());
            // 构建 FFmpeg 抽帧命令。
            List<String> command = List.of(
                    // FFmpeg 可执行文件路径。
                    ffmpegProperties.getFfmpegPath(),
                    // 覆盖已有输出文件，便于重复测试。
                    "-y",
                    // seek（跳转）到指定时间点。
                    "-ss", String.valueOf(request.getTimestampSeconds()),
                    // 输入视频文件。
                    "-i", request.getVideoPath().toString(),
                    // 只抽取一帧。
                    "-frames:v", "1",
                    // 设置 JPEG 图片质量。
                    "-q:v", String.valueOf(request.getJpegQuality()),
                    // 输出图片路径。
                    request.getOutputPath().toString()
            );
            // 创建进程构建器。
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 合并错误输出，失败时可以拿到完整原因。
            processBuilder.redirectErrorStream(true);
            // 启动 FFmpeg 进程。
            Process process = processBuilder.start();
            // 异步读取输出，避免缓冲区阻塞。
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
            // 等待命令完成。
            boolean finished = process.waitFor(ffmpegProperties.getFrameExtractTimeoutSeconds(), TimeUnit.SECONDS);
            // 如果超时，则强制销毁。
            if (!finished) {
                // 强制结束进程。
                process.destroyForcibly();
                // 抛出超时异常。
                throw new FFmpegFrameExtractException(VideoFrameExtractStatus.TIMEOUT, "FFmpeg 抽帧超时，超过 " + ffmpegProperties.getFrameExtractTimeoutSeconds() + " 秒");
            }
            // 读取输出。
            String output = outputFuture.join();
            // 读取退出码。
            int exitCode = process.exitValue();
            // 非 0 退出码表示抽帧失败。
            if (exitCode != 0) {
                // 抛出失败异常。
                throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "FFmpeg 抽帧失败，退出码：" + exitCode + "，输出：" + output);
            }
            // 如果输出图片不存在，视为失败。
            if (!Files.exists(request.getOutputPath())) {
                // 抛出失败异常。
                throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "FFmpeg 抽帧后没有生成输出文件：" + request.getOutputPath());
            }
            // 返回抽帧结果。
            return FrameExtractResult.builder()
                    // 设置输出路径。
                    .outputPath(request.getOutputPath())
                    // 设置退出码。
                    .exitCode(exitCode)
                    // 构建结果。
                    .build();
        } catch (FFmpegFrameExtractException exception) {
            // 帧抽取业务异常直接抛出。
            throw exception;
        } catch (CompletionException exception) {
            // 异步读取输出失败时解包原始异常。
            if (exception.getCause() instanceof FFmpegFrameExtractException frameExtractException) {
                // 抛出原始帧抽取异常。
                throw frameExtractException;
            }
            // 抛出普通失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "读取 FFmpeg 输出失败：" + exception.getMessage(), exception);
        } catch (IOException exception) {
            // 命令不可用或文件写入失败。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FFMPEG_UNAVAILABLE, "FFmpeg 命令执行失败，请检查 ffmpeg-path 配置：" + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            // 恢复线程中断状态。
            Thread.currentThread().interrupt();
            // 抛出普通失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "FFmpeg 抽帧被中断", exception);
        }
    }

    /**
     * validateRequest（校验请求）用于检查抽帧请求的必需字段。
     *
     * @param request 帧抽取请求
     */
    private void validateRequest(FrameExtractRequest request) {
        // 请求不能为空。
        if (request == null) {
            // 抛出失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "抽帧请求不能为空");
        }
        // 视频路径不能为空且必须存在。
        if (request.getVideoPath() == null || !Files.exists(request.getVideoPath())) {
            // 抛出视频文件不存在异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.VIDEO_FILE_NOT_FOUND, "视频文件不存在：" + request.getVideoPath());
        }
        // 输出路径不能为空。
        if (request.getOutputPath() == null) {
            // 抛出失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "抽帧输出路径不能为空");
        }
        // 时间点不能为空且不能小于 0。
        if (request.getTimestampSeconds() == null || request.getTimestampSeconds() < 0) {
            // 抛出失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "抽帧时间点不合法：" + request.getTimestampSeconds());
        }
        // JPEG 质量不能为空且必须大于 0。
        if (request.getJpegQuality() == null || request.getJpegQuality() <= 0) {
            // 抛出失败异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "JPEG 质量参数不合法：" + request.getJpegQuality());
        }
    }

    /**
     * readOutput（读取输出）用于读取 FFmpeg（音视频处理工具）的标准输出。
     *
     * @param process FFmpeg 进程
     * @return 输出文本
     */
    private String readOutput(Process process) {
        try {
            // 使用 UTF-8（统一字符编码）读取完整输出。
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // 读取失败时抛出运行时异常。
            throw new FFmpegFrameExtractException(VideoFrameExtractStatus.FAILED, "读取 FFmpeg 输出失败：" + exception.getMessage(), exception);
        }
    }
}
