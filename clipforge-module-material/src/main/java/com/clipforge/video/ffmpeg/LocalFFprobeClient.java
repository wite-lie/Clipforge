package com.clipforge.video.ffmpeg;

import com.clipforge.video.metadata.VideoMetadataProbeStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LocalFFprobeClient（本地 FFprobe 客户端）。
 *
 * <p>该实现通过 ProcessBuilder（进程构建器）调用本机 FFprobe（视频探测工具）。
 */
@Component
@RequiredArgsConstructor
public class LocalFFprobeClient implements FFprobeClient {

    /** ffmpegProperties（音视频处理配置）提供 FFprobe 路径和超时时间。 */
    private final FFmpegProperties ffmpegProperties;

    /**
     * probe（探测）用于读取视频文件的媒体信息 JSON（对象表示格式）。
     *
     * @param videoPath 视频文件路径
     * @return FFprobe 探测结果
     */
    @Override
    public FFprobeResult probe(Path videoPath) {
        // 校验视频路径不能为空。
        if (videoPath == null) {
            // 抛出文件不存在异常。
            throw new FFprobeException(VideoMetadataProbeStatus.FILE_NOT_FOUND, "视频文件路径不能为空");
        }
        // 校验视频文件必须存在。
        if (!Files.exists(videoPath)) {
            // 抛出文件不存在异常。
            throw new FFprobeException(VideoMetadataProbeStatus.FILE_NOT_FOUND, "视频文件不存在：" + videoPath);
        }
        // 构建 FFprobe 命令。
        List<String> command = List.of(
                // FFprobe 可执行文件路径。
                ffmpegProperties.getFfprobePath(),
                // quiet（安静模式）避免日志污染 JSON 输出。
                "-v", "quiet",
                // 输出 JSON（对象表示格式）。
                "-print_format", "json",
                // 显示容器格式信息。
                "-show_format",
                // 显示媒体流信息。
                "-show_streams",
                // 视频文件路径。
                videoPath.toString()
        );
        try {
            // 创建进程构建器。
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            // 合并错误输出，失败时可以拿到更完整的错误信息。
            processBuilder.redirectErrorStream(true);
            // 启动 FFprobe 进程。
            Process process = processBuilder.start();
            // 异步读取输出，避免进程输出缓冲区塞满后阻塞退出。
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
            // 等待命令在超时时间内完成。
            boolean finished = process.waitFor(ffmpegProperties.getProbeTimeoutSeconds(), TimeUnit.SECONDS);
            // 如果超时，则强制销毁进程。
            if (!finished) {
                // 强制结束进程。
                process.destroyForcibly();
                // 抛出超时异常。
                throw new FFprobeException(VideoMetadataProbeStatus.TIMEOUT, "FFprobe 读取视频元数据超时，超过 " + ffmpegProperties.getProbeTimeoutSeconds() + " 秒");
            }
            // 读取输出内容。
            String output = outputFuture.join();
            // 读取退出码。
            int exitCode = process.exitValue();
            // 非 0 退出码表示执行失败。
            if (exitCode != 0) {
                // 抛出执行失败异常。
                throw new FFprobeException(VideoMetadataProbeStatus.UNSUPPORTED_FORMAT, "FFprobe 读取失败，退出码：" + exitCode + "，输出：" + output);
            }
            // 输出为空表示无法获得有效元数据。
            if (output.isBlank()) {
                // 抛出普通失败异常。
                throw new FFprobeException(VideoMetadataProbeStatus.FAILED, "FFprobe 输出为空");
            }
            // 返回探测结果。
            return FFprobeResult.builder()
                    // 设置原始 JSON。
                    .rawJson(output)
                    // 设置退出码。
                    .exitCode(exitCode)
                    // 构建结果。
                    .build();
        } catch (FFprobeException exception) {
            // FFprobe 业务异常直接抛出。
            throw exception;
        } catch (CompletionException exception) {
            // 异步读取输出失败时解包原始异常。
            if (exception.getCause() instanceof FFprobeException ffprobeException) {
                // 抛出原始 FFprobe 异常。
                throw ffprobeException;
            }
            // 抛出普通失败异常。
            throw new FFprobeException(VideoMetadataProbeStatus.FAILED, "读取 FFprobe 输出失败：" + exception.getMessage(), exception);
        } catch (IOException exception) {
            // 命令不可用或进程启动失败。
            throw new FFprobeException(VideoMetadataProbeStatus.FAILED, "FFprobe 命令执行失败，请检查 ffprobe-path 配置：" + exception.getMessage(), exception);
        } catch (InterruptedException exception) {
            // 恢复线程中断状态。
            Thread.currentThread().interrupt();
            // 抛出中断异常。
            throw new FFprobeException(VideoMetadataProbeStatus.FAILED, "FFprobe 读取被中断", exception);
        }
    }

    /**
     * readOutput（读取输出）用于读取 FFprobe（视频探测工具）的标准输出。
     *
     * @param process FFprobe 进程
     * @return 输出文本
     */
    private String readOutput(Process process) {
        try {
            // 使用 UTF-8（统一字符编码）读取完整输出。
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            // 读取失败时抛出运行时异常，由外层 join（等待结果）包装。
            throw new FFprobeException(VideoMetadataProbeStatus.FAILED, "读取 FFprobe 输出失败：" + exception.getMessage(), exception);
        }
    }
}
