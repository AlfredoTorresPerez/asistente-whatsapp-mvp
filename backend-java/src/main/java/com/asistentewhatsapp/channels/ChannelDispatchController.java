package com.asistentewhatsapp.channels;

import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channels/messages")
public class ChannelDispatchController {

    private final ChannelDispatchService channelDispatchService;

    public ChannelDispatchController(ChannelDispatchService channelDispatchService) {
        this.channelDispatchService = channelDispatchService;
    }

    @PostMapping("/dispatch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ChannelDispatchResponse dispatch(@Valid @RequestBody ChannelDispatchRequest request) {
        return channelDispatchService.dispatch(request);
    }
}
