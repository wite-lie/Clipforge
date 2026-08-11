package com.clipforge.memory.extract.llm;

import com.clipforge.common.BusinessException;
import com.clipforge.common.ErrorCode;
import com.clipforge.infra.persistence.enums.MemorySourceType;
import com.clipforge.memory.extract.ExtractedUserPreferenceMemory;
import com.clipforge.memory.extract.MemoryExtractionModelClient;
import com.clipforge.memory.extract.MemoryExtractionModelRequest;
import com.clipforge.memory.extract.MemoryExtractionModelResponse;
import com.clipforge.memory.extract.MemoryExtractionProperties;
import com.clipforge.model.chat.ChatMessage;
import com.clipforge.model.chat.ChatModelClient;
import com.clipforge.model.chat.ChatModelProperties;
import com.clipforge.model.chat.ChatModelRequest;
import com.clipforge.model.chat.ChatModelResponse;
import com.clipforge.model.usage.ModelCallRecord;
import com.clipforge.model.usage.ModelUsageRecorder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * LLMMemoryExtractionModelClient（大模型记忆抽取客户端）。
 */
@Component
@RequiredArgsConstructor
public class LLMMemoryExtractionModelClient implements MemoryExtractionModelClient {

    private static final String PURPOSE = "MEMORY_EXTRACTION";

    private final ChatModelClient chatModelClient;
    private final MemoryExtractionProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ModelUsageRecorder modelUsageRecorder;
    private final ChatModelProperties chatModelProperties;

    @Override
    public MemoryExtractionModelResponse extract(MemoryExtractionModelRequest request) {
        validateRequest(request);
        String systemPrompt = readResource(properties.getSystemPath());
        String userPrompt = renderUserPrompt(readResource(properties.getUserPath()), request);
        if (properties.isLogPrompt()) {
            System.out.println("===== Memory 抽取 System Prompt =====");
            System.out.println(systemPrompt);
            System.out.println("===== Memory 抽取 User Prompt =====");
            System.out.println(userPrompt);
        }
        long startedAt = System.currentTimeMillis();
        ChatModelResponse response = null;
        try {
            response = chatModelClient.chat(ChatModelRequest.builder()
                    .runId(request.getRunId())
                    .purpose(PURPOSE)
                    .temperature(properties.getTemperature())
                    .maxTokens(properties.getMaxTokens())
                    .messages(List.of(
                            ChatMessage.system(systemPrompt),
                            ChatMessage.user(userPrompt)
                    ))
                    .build());
            String rawContent = response == null ? null : response.getContent();
            List<ExtractedUserPreferenceMemory> memories = parseMemories(rawContent);
            return MemoryExtractionModelResponse.builder()
                    .memories(filterAndLimit(memories))
                    .modelResponse(response)
                    .rawContent(rawContent)
                    .build();
        } catch (RuntimeException exception) {
            throw exception;
        } finally {
            recordUsage(request, response, System.currentTimeMillis() - startedAt);
        }
    }

    private void validateRequest(MemoryExtractionModelRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "记忆抽取请求不能为空");
        }
    }

    private String renderUserPrompt(String template, MemoryExtractionModelRequest request) {
        String ruleMemories = renderRuleMemories(request.getRuleMemories());
        String finalResult = request.getFinalResult() == null ? "暂无最终结果" : safe(objectToJson(request.getFinalResult()));
        return template
                .replace("{{runId}}", safe(request.getRunId()))
                .replace("{{userId}}", safe(request.getUserId()))
                .replace("{{projectId}}", safe(request.getProjectId()))
                .replace("{{conversationId}}", safe(request.getConversationId()))
                .replace("{{platform}}", safe(request.getPlatform()))
                .replace("{{triggerType}}", safe(request.getTriggerType() == null ? null : request.getTriggerType().value()))
                .replace("{{triggerReason}}", safe(request.getTriggerReason()))
                .replace("{{userInput}}", safe(request.getUserInput()))
                .replace("{{stylePreference}}", safe(request.getStylePreference()))
                .replace("{{finalResult}}", finalResult)
                .replace("{{ruleMemories}}", ruleMemories);
    }

    private String renderRuleMemories(List<ExtractedUserPreferenceMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return "- 暂无规则抽取结果";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < memories.size(); index++) {
            ExtractedUserPreferenceMemory memory = memories.get(index);
            builder.append("- memory ").append(index + 1).append(System.lineSeparator())
                    .append("  memoryType: ").append(safe(memory.getMemoryType())).append(System.lineSeparator())
                    .append("  memoryKey: ").append(safe(memory.getMemoryKey())).append(System.lineSeparator())
                    .append("  memoryValue: ").append(safe(memory.getMemoryValue())).append(System.lineSeparator())
                    .append("  confidence: ").append(memory.getConfidence() == null ? "" : memory.getConfidence()).append(System.lineSeparator())
                    .append("  sourceType: ").append(safe(memory.getSourceType())).append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    private List<ExtractedUserPreferenceMemory> parseMemories(String rawContent) {
        if (!hasText(rawContent)) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "记忆抽取模型返回为空");
        }
        String json = extractJson(rawContent);
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode memoriesNode = root.path("memories");
            List<ExtractedUserPreferenceMemory> memories = new ArrayList<>();
            if (memoriesNode.isArray()) {
                for (JsonNode node : memoriesNode) {
                    ExtractedUserPreferenceMemory memory = objectMapper.treeToValue(node, ExtractedUserPreferenceMemory.class);
                    if (memory != null && hasText(memory.getMemoryType()) && hasText(memory.getMemoryKey()) && hasText(memory.getMemoryValue())) {
                        if (!hasText(memory.getSourceType())) {
                            memory.setSourceType(MemorySourceType.LLM_INFERRED.value());
                        }
                        memories.add(memory);
                    }
                }
            }
            return memories;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.MODEL_OUTPUT_PARSE_ERROR, "记忆抽取模型输出 JSON 解析失败", exception);
        }
    }

    private List<ExtractedUserPreferenceMemory> filterAndLimit(List<ExtractedUserPreferenceMemory> memories) {
        if (memories == null || memories.isEmpty()) {
            return List.of();
        }
        BigDecimal threshold = properties.getMinPersistConfidence();
        List<ExtractedUserPreferenceMemory> filtered = new ArrayList<>();
        for (ExtractedUserPreferenceMemory memory : memories) {
            if (memory == null || !hasText(memory.getMemoryType()) || !hasText(memory.getMemoryKey()) || !hasText(memory.getMemoryValue())) {
                continue;
            }
            if (threshold != null && memory.getConfidence() != null && memory.getConfidence().compareTo(threshold) < 0) {
                continue;
            }
            filtered.add(memory);
            if (properties.getMaxCandidateCount() > 0 && filtered.size() >= properties.getMaxCandidateCount()) {
                break;
            }
        }
        return filtered;
    }

    private String extractJson(String rawContent) {
        String trimmed = rawContent.trim();
        if (trimmed.startsWith("```")) {
            int firstNewLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewLine >= 0 && lastFence > firstNewLine) {
                return trimmed.substring(firstNewLine + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String readResource(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "记忆抽取提示词模板不存在：" + path);
            }
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_ERROR, "记忆抽取提示词模板读取失败：" + path, exception);
        }
    }

    private String objectToJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private void recordUsage(MemoryExtractionModelRequest request, ChatModelResponse response, long latencyMs) {
        modelUsageRecorder.record(ModelCallRecord.builder()
                .runId(request.getRunId())
                .projectId(request.getProjectId())
                .conversationId(request.getConversationId())
                .provider(response == null ? chatModelProperties.getProvider() : response.getProvider())
                .modelName(response == null ? chatModelProperties.getModelName() : response.getModelName())
                .purpose(PURPOSE)
                .promptTokens(response == null ? null : response.getPromptTokens())
                .completionTokens(response == null ? null : response.getCompletionTokens())
                .totalTokens(response == null ? null : response.getTotalTokens())
                .latencyMs(latencyMs)
                .success(response != null)
                .errorCode(response == null ? "MODEL_EMPTY" : null)
                .errorMessage(response == null ? "记忆抽取模型未返回内容" : null)
                .build());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
