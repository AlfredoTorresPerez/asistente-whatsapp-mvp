package com.asistentewhatsapp.channels;

import com.asistentewhatsapp.channels.application.ChannelDispatchRequest;
import com.asistentewhatsapp.channels.application.ChannelDispatchResponse;
import com.asistentewhatsapp.channels.application.ChannelDispatchService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CHANNEL_MANAGE')")
	@PostMapping("/dispatch")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public ChannelDispatchResponse dispatch(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody ChannelDispatchRequest request) {
		ChannelDispatchRequest secureRequest = new ChannelDispatchRequest(authenticatedUser.businessId(),
				request.channelType(), request.recipientPhone(), request.body());
		return channelDispatchService.dispatch(secureRequest);
	}
}
