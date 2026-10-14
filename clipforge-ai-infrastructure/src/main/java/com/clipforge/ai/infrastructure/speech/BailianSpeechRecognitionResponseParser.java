package com.clipforge.ai.infrastructure.speech;
import com.clipforge.ai.api.speech.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * BailianSpeechRecognitionResponseParser（百炼语音识别响应解析器）。
 *
 * <p>百炼 ASR（自动语音识别）响应结构可能随模型或接口形态变化，因此这里采用容错解析：
 * 优先读取明确的 text/transcript 字段，再尝试从 content 数组和字幕片段数组中提取文本。
 */
public class BailianSpeechRecognitionResponseParser {

    private final ObjectMapper objectMapper;

    public BailianSpeechRecognitionResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * parse（解析）用于把供应商原始 JSON（对象表示格式）转成系统内部 ASR 响应。
     */
    public SpeechRecognitionModelResponse parse(String rawContent, String modelName) {
        try {
            JsonNode root = objectMapper.readTree(rawContent);
            List<SpeechRecognitionSegment> segments = new ArrayList<>();
            collectSegments(root, segments);
            collectSentenceNode(root.findPath("sentence"), segments);
            String transcript = firstNonBlank(
                    textAt(root, "/output/text"),
                    textAt(root, "/output/transcript"),
                    textAt(root, "/sentence/text"),
                    textAt(root, "/sentence/transcript"),
                    textAt(root, "/sentence/sentence"),
                    textAt(root, "/output/sentence/text"),
                    textAt(root, "/output/choices/0/message/content"),
                    textAt(root, "/output/choices/0/message/content/0/text"),
                    textAt(root, "/output/choices/0/message/content/0/transcript"),
                    joinSegmentText(segments)
            );
            boolean speechDetected = transcript != null && !transcript.isBlank();
            return SpeechRecognitionModelResponse.builder()
                    .provider("bailian")
                    .modelName(modelName)
                    .transcriptText(transcript)
                    .speechDetected(speechDetected)
                    .segments(segments)
                    .rawContent(rawContent)
                    .build();
        } catch (Exception exception) {
            return SpeechRecognitionModelResponse.builder()
                    .provider("bailian")
                    .modelName(modelName)
                    .transcriptText(null)
                    .speechDetected(false)
                    .segments(List.of())
                    .rawContent(rawContent)
                    .build();
        }
    }

    private void collectSegments(JsonNode root, List<SpeechRecognitionSegment> result) {
        collectSegmentArray(root.findPath("segments"), result);
        collectSegmentArray(root.findPath("sentences"), result);
        collectSegmentArray(root.findPath("transcripts"), result);
    }

    private void collectSentenceNode(JsonNode node, List<SpeechRecognitionSegment> result) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) {
            return;
        }
        String text = firstNonBlank(textAt(node, "/text"), textAt(node, "/transcript"), textAt(node, "/sentence"));
        JsonNode wordsNode = node.findPath("words");
        if ((text == null || text.isBlank()) && wordsNode != null && wordsNode.isArray()) {
            text = joinWordText(wordsNode);
        }
        if (text == null || text.isBlank()) {
            return;
        }
        Double startSeconds = secondsAt(node, new String[]{"/start_seconds", "/start"}, new String[]{"/begin_time", "/beginTime"});
        Double endSeconds = secondsAt(node, new String[]{"/end_seconds", "/end"}, new String[]{"/end_time", "/endTime"});
        if ((startSeconds == null || endSeconds == null) && wordsNode != null && wordsNode.isArray() && wordsNode.size() > 0) {
            JsonNode firstWord = wordsNode.get(0);
            JsonNode lastWord = wordsNode.get(wordsNode.size() - 1);
            if (startSeconds == null) {
                startSeconds = secondsAt(firstWord, new String[]{"/start_seconds", "/start"}, new String[]{"/begin_time", "/beginTime"});
            }
            if (endSeconds == null) {
                endSeconds = secondsAt(lastWord, new String[]{"/end_seconds", "/end"}, new String[]{"/end_time", "/endTime"});
            }
        }
        result.add(SpeechRecognitionSegment.builder()
                .startSeconds(startSeconds)
                .endSeconds(endSeconds)
                .text(text)
                .confidence(numberAt(node, "/confidence", "/score"))
                .build());
    }

    private void collectSegmentArray(JsonNode arrayNode, List<SpeechRecognitionSegment> result) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return;
        }
        for (JsonNode node : arrayNode) {
            String text = firstNonBlank(textAt(node, "/text"), textAt(node, "/transcript"), textAt(node, "/sentence"));
            if (text == null || text.isBlank()) {
                continue;
            }
            result.add(SpeechRecognitionSegment.builder()
                    .startSeconds(secondsAt(node, new String[]{"/start_seconds", "/start"}, new String[]{"/begin_time", "/beginTime"}))
                    .endSeconds(secondsAt(node, new String[]{"/end_seconds", "/end"}, new String[]{"/end_time", "/endTime"}))
                    .text(text)
                    .confidence(numberAt(node, "/confidence", "/score"))
                    .build());
        }
    }

    private Double secondsAt(JsonNode node, String[] secondPointers, String[] millisecondPointers) {
        Double seconds = numberAt(node, secondPointers);
        if (seconds != null) {
            return seconds;
        }
        Double milliseconds = numberAt(node, millisecondPointers);
        if (milliseconds == null) {
            return null;
        }
        return milliseconds / 1000D;
    }

    private String textAt(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            for (JsonNode item : node) {
                String text = firstNonBlank(textAt(item, "/text"), textAt(item, "/transcript"));
                if (text != null && !text.isBlank()) {
                    parts.add(text);
                }
            }
            return parts.isEmpty() ? null : String.join("\n", parts);
        }
        return null;
    }

    private Double numberAt(JsonNode node, String... pointers) {
        for (String pointer : pointers) {
            JsonNode value = node.at(pointer);
            if (value != null && value.isNumber()) {
                return value.asDouble();
            }
            if (value != null && value.isTextual()) {
                try {
                    return Double.parseDouble(value.asText());
                } catch (NumberFormatException ignored) {
                    // 继续尝试下一个字段。
                }
            }
        }
        return null;
    }

    private String joinSegmentText(List<SpeechRecognitionSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        List<String> texts = segments.stream()
                .map(SpeechRecognitionSegment::getText)
                .filter(text -> text != null && !text.isBlank())
                .toList();
        return texts.isEmpty() ? null : String.join("", texts);
    }

    private String joinWordText(JsonNode wordsNode) {
        if (wordsNode == null || !wordsNode.isArray()) {
            return null;
        }
        List<String> texts = new ArrayList<>();
        for (JsonNode word : wordsNode) {
            String text = firstNonBlank(textAt(word, "/text"), textAt(word, "/transcript"), textAt(word, "/word"));
            if (text != null && !text.isBlank()) {
                texts.add(text);
            }
        }
        return texts.isEmpty() ? null : String.join("", texts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
