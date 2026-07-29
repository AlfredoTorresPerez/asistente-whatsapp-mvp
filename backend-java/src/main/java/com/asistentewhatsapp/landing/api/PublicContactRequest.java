package com.asistentewhatsapp.landing.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PublicContactRequest(@NotBlank @JsonProperty("name") String name,
		@NotBlank @JsonProperty("phone") String phone) {
}
