package com.clipforge.knowledge.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * SimpleKeywordTokenizer（轻量关键词分词器）。
 *
 * <p>该实现用于 Phase 7.10（阶段 7.10）第一版 BM25（最佳匹配 25）关键词检索。它不追求工业级中文分词精度，而是用可解释、可替换的规则先跑通混合检索闭环。
 */
@Component
public class SimpleKeywordTokenizer implements KeywordTokenizer {

    /** tokenPattern（词项匹配模式）用于抽取连续中文、英文单词和数字。 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}]+|[A-Za-z]+|\\d+");

    /** stopWords（停用词）用于过滤对检索贡献很低的常见词。 */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "与", "及", "以及", "一个", "这个", "那个", "这些", "那些",
            "怎么", "如何", "什么", "一下", "进行", "关于", "是否", "可以", "需要", "用户"
    );

    /**
     * tokenize（分词）用于生成有序去重的查询词项。
     *
     * @param text 原始查询文本
     * @return 关键词列表
     */
    @Override
    public List<String> tokenize(String text) {
        // 如果输入为空，则返回空列表。
        if (text == null || text.isBlank()) {
            // 返回空关键词。
            return List.of();
        }
        // 使用 LinkedHashSet（有序集合）保证去重后仍保留词项出现顺序。
        Set<String> terms = new LinkedHashSet<>();
        // 创建正则匹配器。
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        // 遍历匹配到的连续片段。
        while (matcher.find()) {
            // 读取当前片段。
            String token = matcher.group();
            // 判断中文片段还是英文数字片段。
            if (isChineseToken(token)) {
                // 中文片段使用 2 到 4 字滑动窗口生成候选词。
                appendChineseTerms(terms, token);
            } else {
                // 英文统一转为小写，便于后续关键词匹配。
                appendTerm(terms, token.toLowerCase());
            }
        }
        // 返回有序关键词列表。
        return new ArrayList<>(terms);
    }

    /**
     * appendChineseTerms（追加中文词项）用于从连续中文片段中生成 2 到 4 字窗口词。
     *
     * @param terms 目标词项集合
     * @param token 连续中文片段
     */
    private void appendChineseTerms(Set<String> terms, String token) {
        // 如果中文片段长度为 1，则只在非停用词时保留。
        if (token.length() == 1) {
            // 追加单字词项。
            appendTerm(terms, token);
            // 单字片段处理结束。
            return;
        }
        // 优先保留完整中文片段，方便命中“抖音”“虎鲸”等短实体。
        appendTerm(terms, token);
        // 生成 2 到 4 字窗口。
        for (int window = 2; window <= 4; window++) {
            // 如果窗口大于片段长度，则跳过。
            if (window > token.length()) {
                // 继续下一个窗口。
                continue;
            }
            // 按当前位置生成滑动窗口词。
            for (int index = 0; index <= token.length() - window; index++) {
                // 追加窗口词。
                appendTerm(terms, token.substring(index, index + window));
            }
        }
    }

    /**
     * appendTerm（追加词项）用于过滤空白和停用词后写入集合。
     *
     * @param terms 目标词项集合
     * @param term 候选词项
     */
    private void appendTerm(Set<String> terms, String term) {
        // 如果候选词为空，则跳过。
        if (term == null || term.isBlank()) {
            // 结束处理。
            return;
        }
        // 如果候选词是停用词，则跳过。
        if (STOP_WORDS.contains(term)) {
            // 结束处理。
            return;
        }
        // 写入词项集合。
        terms.add(term);
    }

    /**
     * isChineseToken（是否中文片段）用于判断当前片段是否全部由中文字符组成。
     *
     * @param token 待判断片段
     * @return true 表示中文片段
     */
    private boolean isChineseToken(String token) {
        // 遍历片段中的每个字符。
        for (int index = 0; index < token.length(); index++) {
            // 如果存在非中文字符，则返回 false。
            if (Character.UnicodeScript.of(token.charAt(index)) != Character.UnicodeScript.HAN) {
                // 不是纯中文片段。
                return false;
            }
        }
        // 所有字符都是中文。
        return true;
    }
}
