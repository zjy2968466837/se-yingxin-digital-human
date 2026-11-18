package com.sicnu.yingxin.qa.service.impl;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 问答文本归一化与相似度匹配工具。
 *
 * <p>匹配算法：字符 2-gram Jaccard 相似度。
 * 对「归一化输入」与「归一化(标准问法 + 每条同义词)」分别计算，取最大值作为该条目的命中分数。
 * 纯 Java 实现、无外部依赖，演示规模（全量内存打分）下足够稳定可复现。
 */
@Component
public class QaTextMatcher {

    /**
     * 文本归一化：转小写、去除所有空白与中英文标点（含全角）。
     *
     * <p>正则 {@code [\s\p{P}\p{S}]} 覆盖：空白、ASCII 标点、中文标点（，。！？「」等）、
     * 全角符号与 emoji 等符号类，只保留字母数字与汉字等语义字符。
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s\\p{P}\\p{S}]+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 问题指纹：归一化文本的 SHA-256 十六进制（64 位小写），用于热点缓存 key 与提问日志。
     */
    public String fingerprint(String normalizedText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(normalizedText.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 必带 SHA-256，理论上不会走到这里
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 字符 2-gram Jaccard 相似度。
     *
     * <p>长度为 1 的文本退化为单字符集合，保证「单字问法」也能正确比较；
     * 两个空串视为完全相同（相似度 1.0），调用方一般在归一化后已过滤空串。
     */
    public double similarity(String normalizedA, String normalizedB) {
        Set<String> gramsA = bigrams(normalizedA);
        Set<String> gramsB = bigrams(normalizedB);
        if (gramsA.isEmpty() && gramsB.isEmpty()) {
            return normalizedA.equals(normalizedB) ? 1.0 : 0.0;
        }
        if (gramsA.isEmpty() || gramsB.isEmpty()) {
            return 0.0;
        }
        int intersection = 0;
        for (String gram : gramsA) {
            if (gramsB.contains(gram)) {
                intersection++;
            }
        }
        int union = gramsA.size() + gramsB.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    /** 字符 2-gram 集合；长度不足 2 时退化为单字符 */
    private Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (text == null || text.isEmpty()) {
            return grams;
        }
        if (text.length() == 1) {
            grams.add(text);
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }
}
