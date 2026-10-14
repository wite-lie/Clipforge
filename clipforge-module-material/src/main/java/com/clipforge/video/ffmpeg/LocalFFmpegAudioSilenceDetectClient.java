package com.clipforge.video.ffmpeg;

import com.clipforge.video.audio.AudioSilenceDetectClient;
import com.clipforge.video.audio.AudioSilenceDetectRequest;
import com.clipforge.video.audio.AudioSilenceDetectResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LocalFFmpegAudioSilenceDetectClient（本地 FFmpeg 音频静音检测客户端）。
 */
@Component
@RequiredArgsConstructor
public class LocalFFmpegAudioSilenceDetectClient implements AudioSilenceDetectClient {

    private static final Pattern SILENCE_START_PATTERN = Pattern.compile("silence_start: ([0-9.]+)");
    private static final Pattern SILENCE_END_PATTERN = Pattern.compile("silence_end: ([0-9.]+)");

    private final FFmpegProperties ffmpegProperties;

    @Override
    public AudioSilenceDetectResult detect(AudioSilenceDetectRequest request) {
        if (request == null || request.getAudioPath() == null || !Files.exists(request.getAudioPath())) {
            return AudioSilenceDetectResult.builder()
                    .silenceDetected(false)
                    .silenceRatio(0)
                    .silenceSeconds(0)
                    .message("音频文件不存在，跳过静音检测")
                    .build();
        }
        try {
            String output = execute(request);
            double silenceSeconds = parseSilenceSeconds(output, request.getDurationSeconds());
            double duration = request.getDurationSeconds() == null || request.getDurationSeconds() <= 0
                    ? 0
                    : request.getDurationSeconds();
            double ratio = duration <= 0 ? 0 : Math.min(1.0, silenceSeconds / duration);
            boolean silenceDetected = ratio >= ffmpegProperties.getSilenceRatioThreshold();
            return AudioSilenceDetectResult.builder()
                    .silenceDetected(silenceDetected)
                    .silenceRatio(ratio)
                    .silenceSeconds(silenceSeconds)
                    .message(silenceDetected
                            ? "音频近似静音，静音占比：" + ratio
                            : "音频存在非静音内容，静音占比：" + ratio)
                    .build();
        } catch (Exception exception) {
            return AudioSilenceDetectResult.builder()
                    .silenceDetected(false)
                    .silenceRatio(0)
                    .silenceSeconds(0)
                    .message("静音检测失败，继续尝试 ASR：" + exception.getMessage())
                    .build();
        }
    }

    private String execute(AudioSilenceDetectRequest request) throws IOException, InterruptedException {
        List<String> command = List.of(
                ffmpegProperties.getFfmpegPath(),
                "-i", request.getAudioPath().toString(),
                "-af", "silencedetect=noise=" + ffmpegProperties.getSilenceNoiseDb() + "dB:d="
                        + ffmpegProperties.getSilenceMinDurationSeconds(),
                "-f", "null",
                "-"
        );
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
        boolean finished = process.waitFor(ffmpegProperties.getSilenceDetectTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("FFmpeg 静音检测超时");
        }
        return outputFuture.join();
    }

    double parseSilenceSeconds(String output, Double durationSeconds) {
        double silenceSeconds = 0;
        Double currentStart = null;
        for (String line : output.split("\\R")) {
            Matcher startMatcher = SILENCE_START_PATTERN.matcher(line);
            if (startMatcher.find()) {
                currentStart = Double.parseDouble(startMatcher.group(1));
                continue;
            }
            Matcher endMatcher = SILENCE_END_PATTERN.matcher(line);
            if (endMatcher.find() && currentStart != null) {
                double end = Double.parseDouble(endMatcher.group(1));
                silenceSeconds += Math.max(0, end - currentStart);
                currentStart = null;
            }
        }
        if (currentStart != null && durationSeconds != null && durationSeconds > currentStart) {
            silenceSeconds += durationSeconds - currentStart;
        }
        return silenceSeconds;
    }

    private String readOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("读取 FFmpeg 静音检测输出失败", exception);
        }
    }
}
