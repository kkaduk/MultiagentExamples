package com.example.coordinator.conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;


public class ConversationMemoryChatMemory implements ChatMemory {

    Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {

        List<Message> userMessages = conversationHistory.getOrDefault(conversationId, new ArrayList<>());

        for (Message message : messages) {
            if (message.getMessageType() == MessageType.USER) {
                userMessages.add(message);
                conversationHistory.put(conversationId, userMessages);
            }
        }

       
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> all = this.conversationHistory.get(conversationId);
        return all != null ? all.stream().skip(Math.max(0, all.size() - lastN)).toList() : List.of();
    }

    @Override
    public void clear(String conversationId) {
        this.conversationHistory.remove(conversationId);
    }

}
