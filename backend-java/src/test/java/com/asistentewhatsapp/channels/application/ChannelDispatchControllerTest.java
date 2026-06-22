package com.asistentewhatsapp.channels.application;

import com.asistentewhatsapp.channels.ChannelDispatchController;
import com.asistentewhatsapp.channels.domain.MessageChannelType;
import com.asistentewhatsapp.security.JwtAccessDeniedHandler;
import com.asistentewhatsapp.security.JwtAuthenticationEntryPoint;
import com.asistentewhatsapp.security.SecurityConfig;
import com.asistentewhatsapp.security.application.JwtService;
import com.asistentewhatsapp.shared.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChannelDispatchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ChannelDispatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @MockitoBean
    private ChannelDispatchService channelDispatchService;

    @Test
    @WithMockUser(roles = "OWNER")
    void shouldDispatchMessageWhenRequestIsValid() throws Exception {
        when(channelDispatchService.dispatch(any(ChannelDispatchRequest.class)))
                .thenReturn(new ChannelDispatchResponse(
                        MessageChannelType.WHATSAPP,
                        "msg-123",
                        "QUEUED",
                        Instant.parse("2026-05-23T20:15:30Z")));

        ChannelDispatchRequest request = new ChannelDispatchRequest(
                UUID.fromString("4b3e1466-4c89-4e46-8c57-bc992a3b3b1a"),
                MessageChannelType.WHATSAPP,
                "+56911112222",
                "Hola, este es un mensaje de prueba.");

        mockMvc.perform(post("/api/v1/channels/messages/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.channelType").value("WHATSAPP"))
                .andExpect(jsonPath("$.externalMessageId").value("msg-123"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    @WithMockUser(roles = "OWNER")
    void shouldReturnValidationErrorsWhenRequestIsInvalid() throws Exception {
        String payload = """
                {
                  "businessId": null,
                  "channelType": "WHATSAPP",
                  "recipientPhone": "",
                  "body": ""
                }
                """;

        mockMvc.perform(post("/api/v1/channels/messages/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.businessId").value("businessId es obligatorio"))
                .andExpect(jsonPath("$.fieldErrors.recipientPhone").value("recipientPhone es obligatorio"))
                .andExpect(jsonPath("$.fieldErrors.body").value("body es obligatorio"));
    }
}
