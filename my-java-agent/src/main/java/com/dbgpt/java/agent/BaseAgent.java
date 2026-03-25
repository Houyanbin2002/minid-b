package com.dbgpt.java.agent;

import org.springframework.ai.chat.client.ChatClient;
import java.util.concurrent.CompletableFuture;

/**
 * 核心 Agent 基类 (平替 DB-GPT 的 BaseAgent)
 */
public abstract class BaseAgent {

    protected final ChatClient chatClient;
    protected final String agentName;
    protected final String roleDescription;
    
    public BaseAgent(ChatClient.Builder builder, String agentName, String roleDescription) {
        this.agentName = agentName;
        this.roleDescription = roleDescription;
        this.chatClient = builder
                .defaultSystem(buildSystemPrompt())
                .build();
    }

    /**
     * 核心执行方法
     */
    public abstract CompletableFuture<String> execute(String input);

    /**
     * 构建带有角色描述的 System Prompt
     */
    protected String buildSystemPrompt() {
        return "You are a professional assistant named '" + agentName + "'.\n" +
               "Your Role: " + roleDescription + "\n\n" +
               "Please fulfill your role strictly and output the designated data or thoughts.";
    }
}
