package com.asistentewhatsapp.conversations.api;

import java.util.UUID;

public record ConversationCustomerResponse(
        UUID id,
        String firstName,
        String lastName,
        String displayName,
        String phone,
        String email) {
}
