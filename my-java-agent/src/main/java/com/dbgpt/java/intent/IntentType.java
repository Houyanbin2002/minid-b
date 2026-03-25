package com.dbgpt.java.intent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 支持的意图类型
 */
public enum IntentType {
    
    @JsonProperty("RAG_SEARCH")
    RAG_SEARCH("从知识库或外部资料库进行检索问答"),
    
    @JsonProperty("TOOL_CALL")
    TOOL_CALL("执行具体工具，如API获取数据操作等"),
    
    @JsonProperty("CHAT")
    CHAT("一般性闲聊或者寒暄"),
    
    @JsonProperty("UNKNOWN")
    UNKNOWN("无法识别的意图");

    private final String description;

    IntentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
