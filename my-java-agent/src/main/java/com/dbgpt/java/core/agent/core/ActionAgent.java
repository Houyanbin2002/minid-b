package com.dbgpt.java.core.agent.core;

import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 等价于 DB-GPT 的 ActionAgent / ToolAgent 
 * 职责是使用自身挂载的 Tool (由 SpringAI Function Calling提供支持) 与观察环境
 */
public class ActionAgent extends ConversableAgent {

    private final ChatClient chatClient;
    private static final int MAX_RETRIES = 3;

    public ActionAgent(AgentProfile profile, AgentMemory memory, ChatClient.Builder builder) {
        super(profile, memory);
        this.chatClient = builder
                .defaultSystem(this.profile.getProfile() + "\n" + this.profile.getConstraints())
                .defaultFunctions(this.profile.getToolNames().toArray(new String[0])) // 挂载工具
                .build();
    }

    @Override
    protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
        return CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            String llmResponse = "";
            
            while (attempts < MAX_RETRIES) {
                try {
                    // 提取全量上下文 (消除“鱼的记忆”)
                    List<Message> springMessages = new ArrayList<>();
                    // 将自定义 AgentMemory 中的历史转换为 Spring AI 标准消息
                    for (AgentMessage historyMsg : memory.getMessages()) {
                        if ("assistant".equalsIgnoreCase(historyMsg.getRole())) {
                            springMessages.add(new AssistantMessage(historyMsg.getContent() != null ? historyMsg.getContent() : ""));
                        } else {
                            springMessages.add(new UserMessage(historyMsg.getContent() != null ? historyMsg.getContent() : ""));
                        }
                    }

                    // 调用模型，SpringAI 会基于函数签名拦截并自动请求工具
                    llmResponse = chatClient.prompt()
                            .messages(springMessages)
                            .call()
                            .content();
                            
                    // 成功则跳出重试循环
                    break; 

                } catch (Exception e) {
                    attempts++;
                    System.err.println("[" + this.getName() + "] Execution error on attempt " + attempts + ": " + e.getMessage());
                    
                    if (attempts >= MAX_RETRIES) {
                        llmResponse = "【系统提示】Agent 执行失败，已达到最大重试次数。最后错误：" + e.getMessage();
                        break;
                    }
                    
                    // 错误反刍，触发 LLM 自动纠错 (Self-Healing)
                    String errorMessage = "Previous action failed with error: " + e.getMessage() + ". Please correct your format or tool arguments and retry.";
                    AgentMessage errorFeedback = AgentMessage.builder()
                            .role("user")
                            .name("System")
                            .content(errorMessage)
                            .build();
                    memory.appendMessage(errorFeedback); // 将错误注回记忆，让模型下一次带着 Error 思考
                }
            }

            // 构建回复消息
            return AgentMessage.builder()
                    .role("assistant")
                    .name(this.getName())
                    .content(llmResponse)
                    .build();
        });
    }
}