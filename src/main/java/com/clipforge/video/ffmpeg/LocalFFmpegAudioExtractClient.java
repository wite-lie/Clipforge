package com.clipforge.video.ffmpeg;

import com.clipforge.video.audio.AudioExtractStatus;
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
 * LocalFFmpegAudioExtractClient（本地 FFmpeg 音频提取客户端）。
 *
 * <p>该实现通过 ProcessBuilder（进程构建器）调用本机 FFmpeg（音视频处理工具），把视频音轨提取为 wav（波形音频文件）。
 */
@Component
@RequiredArgsConstructor
public class LocalFFmpegAudioExtractClient implements FFmpegAudioExtractClient {

    /** ffmpegProperties（音视频处理配置）提供 FFmpeg 路径和音频提取超时时间。 */
    private final FFmpegProperties ffmpegProperties;

    /**
     * extractAudio（提取音频）用于从视频中提取单声道音频文件。
     *
     * @param request 音频提取请求
     * @return 音频提取结果
     */
    @Override
    public AudioExtractResult extractAudio(AudioExtractRequest request) {
        // 校验音频提取请求。
        validateRequest(request);
        try {
            // 创建输出目录。
            Files.createDirectories(request.getOutputPath().getParent());
            // 构建 FFmpeg 音频提取命令。
            List<String> command = List.of(
                    // FFmpeg 可执行文件路径。
                    ffmpegProperties.getFfmpegPath(),
                    // 覆盖已有输出文件，方便重复测试。
                    "-y",
                    // 输入视频文件。
                    "-i", request.getVideoPath().toString(),
                    // vn（video none）表示不输出视频流。
                    "-vn",
                    // 设置声道数，默认单声道。
                    "-ac", String.valueOf(request.getChannels()),
                    // 设置采样率，默认 16000。
                    "-ar", String.valueOf(request.getSampleRate()),
                    // 输出音频文件。
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
            boolean finished = process.waitFor(ffmpegProperties.getAudioExtractTimeoutSeconds(), TimeUnit.SECONDS);
            // 如果超时，则强制销毁。
            if (!finished) {
                // 强制结束进程。
                process.destroyForcibly();
                // 抛出超时异常。
                throw new FFmpegAudioExtractException(AudioExtractStatus.TIMEOUT, "FFmpeg 音频提取超时，超过 " + ffmpegProperties.getAudioExtractTimeoutSeconds() + " 秒");
            }
            // 读取输出。
            String output = outputFuture.join();
            // 读取退出码。
            int exitCode = process.exitValue();
            // 非 0 退出码表示音频提取失败。
            if (exitCode != 0) {
                // 抛出失败异常。
                throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "FFmpeg 音频提取失败，退出码：" + exitCode + "，输出：" + output);
            }
            // 如果输出音频不存在，视为失败。
            if (!Files.exists(request.getOutputPath())) {
                // 抛出失败异常。
                throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "FFmpeg 音频提取后没有生成输出文件：" + request.getOutputPath());
            }
            // 返回音频提取结果。
            return AudioExtractResult.builder()
                    // 设置输出路径。
                    .outputPath(request.getOutputPath())
                    // 设置退出码。
                    .exitCode(exitCode)
                    // 构建结果。
                    .build();
        } catch (FFmpegAudioExtractException exception) {
            // 音频提取业务异常直接抛出。
            throw exception;
        } catch (CompletionException exception) {
            // 异步读取输出失败时解包原始异常。
            if (exception.getCause() instanceof FFmpegAudioExtractException audioExtractException) {
                // 抛出原始音频提取异常。
                throw audioExtractException;
            }
            // 抛出普通失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "读取 FFmpeg 输出失败：" + exception.getMessage(), exception);
        } catch (IOException exception) {
            // 命令不可用或文件写入失败。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FFMPEG_UNAVAILABLE, "FFmpeg 命令执行失败，请检查 ffmpeg-path 配置：" + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            // 恢复线程中断状态。
            Thread.currentThread().interrupt();
            // 抛出普通失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "FFmpeg 音频提取被中断", exception);
        }
    }

    /**
     * validateRequest（校验请求）用于检查音频提取请求的必需字段。
     *
     * @param request 音频提取请求
     */
    private void validateRequest(AudioExtractRequest request) {
        // 请求不能为空。
        if (request == null) {
            // 抛出失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "音频提取请求不能为空");
        }
        // 视频路径不能为空且必须存在。
        if (request.getVideoPath() == null || !Files.exists(request.getVideoPath())) {
            // 抛出视频文件不存在异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.VIDEO_FILE_NOT_FOUND, "视频文件不存在：" + request.getVideoPath());
        }
        // 输出路径不能为空。
        if (request.getOutputPath() == null) {
            // 抛出失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "音频提取输出路径不能为空");
        }
        // 采样率必须大于 0。
        if (request.getSampleRate() == null || request.getSampleRate() <= 0) {
            // 抛出失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "音频采样率不合法：" + request.getSampleRate());
        }
        // 声道数必须大于 0。
        if (request.getChannels() == null || request.getChannels() <= 0) {
            // 抛出失败异常。
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "音频声道数不合法：" + request.getChannels());
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
            throw new FFmpegAudioExtractException(AudioExtractStatus.FAILED, "读取 FFmpeg 输出失败：" + exception.getMessage(), exception);
        }
    }
}
