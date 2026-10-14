package com.clipforge.ai.infrastructure.output;

import org.springframework.stereotype.Component;

/**
 * JsonCompletenessChecker（JSON 完整性检查器）。
 *
 * <p>该组件只做轻量外形检查，不负责把 JSON（对象表示格式）反序列化为业务对象。
 */
@Component
public class JsonCompletenessChecker {

    /**
     * check（检查）用于判断文本中的 JSON 外形是否闭合。
     *
     * @param rawContent 原始模型输出
     * @return JSON 完整性检查结果
     */
    public JsonCompletenessResult check(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return result(false, ModelOutputIssueType.EMPTY_OUTPUT, "模型输出为空", 0, 0);
        }
        String content = stripMarkdownFence(rawContent.strip());
        int startIndex = findJsonStart(content);
        if (startIndex < 0) {
            return result(false, ModelOutputIssueType.INVALID_JSON, "没有找到 JSON 起始符号", 0, 0);
        }
        int objectBalance = 0;
        int arrayBalance = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int index = startIndex; index < content.length(); index++) {
            char item = content.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (item == '\\' && inString) {
                escaping = true;
                continue;
            }
            if (item == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (item == '{') {
                objectBalance++;
            } else if (item == '}') {
                objectBalance--;
            } else if (item == '[') {
                arrayBalance++;
            } else if (item == ']') {
                arrayBalance--;
            }
            if (objectBalance < 0 || arrayBalance < 0) {
                return result(false, ModelOutputIssueType.INVALID_JSON, "JSON 闭合符号多于起始符号", objectBalance, arrayBalance);
            }
        }
        if (inString) {
            return result(false, ModelOutputIssueType.UNCLOSED_JSON_STRING, "JSON 字符串未闭合", objectBalance, arrayBalance);
        }
        if (objectBalance > 0) {
            return result(false, ModelOutputIssueType.UNCLOSED_JSON_OBJECT, "JSON 对象未闭合", objectBalance, arrayBalance);
        }
        if (arrayBalance > 0) {
            return result(false, ModelOutputIssueType.UNCLOSED_JSON_ARRAY, "JSON 数组未闭合", objectBalance, arrayBalance);
        }
        return result(true, ModelOutputIssueType.NONE, "JSON 外形完整", objectBalance, arrayBalance);
    }

    /**
     * stripMarkdownFence（去除代码围栏）用于兼容模型返回 ```json 包裹的内容。
     */
    private String stripMarkdownFence(String content) {
        String result = content.replaceFirst("^```json\\s*", "");
        result = result.replaceFirst("^```\\s*", "");
        return result.replaceFirst("\\s*```$", "");
    }

    /**
     * findJsonStart（查找 JSON 起点）用于定位对象或数组的第一个起始符号。
     */
    private int findJsonStart(String content) {
        int objectStart = content.indexOf('{');
        int arrayStart = content.indexOf('[');
        if (objectStart < 0) {
            return arrayStart;
        }
        if (arrayStart < 0) {
            return objectStart;
        }
        return Math.min(objectStart, arrayStart);
    }

    /**
     * result（结果）用于统一构建检查结果。
     */
    private JsonCompletenessResult result(boolean complete, ModelOutputIssueType issueType, String reason, int objectBalance, int arrayBalance) {
        return JsonCompletenessResult.builder()
                .complete(complete)
                .issueType(issueType)
                .reason(reason)
                .objectBalance(objectBalance)
                .arrayBalance(arrayBalance)
                .build();
    }
}
