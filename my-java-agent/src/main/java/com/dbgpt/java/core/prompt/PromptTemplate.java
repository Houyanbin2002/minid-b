package com.dbgpt.java.core.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对应 DB-GPT Python 源码的 PromptTemplate
 * 核心的提示词模板管理基类
 */
public class PromptTemplate {

    private final String template;
    private final String name;

    public PromptTemplate(String name, String template) {
        this.name = name;
        this.template = template;
    }

    /**
     * 格式化注入变量 (平替 Python 中的 template.format(**kwargs))
     */
    public String format(Map<String, String> variables) {
        String result = this.template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "\\{" + entry.getKey() + "\\}";
            result = result.replaceAll(placeholder, Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    public String getName() {
        return name;
    }
}
