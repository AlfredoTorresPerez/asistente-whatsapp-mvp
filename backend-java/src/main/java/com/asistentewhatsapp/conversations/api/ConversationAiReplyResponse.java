package com.asistentewhatsapp.conversations.api;

public record ConversationAiReplyResponse(
        String suggestedBody,
        double confidence,
        String source) {
}
