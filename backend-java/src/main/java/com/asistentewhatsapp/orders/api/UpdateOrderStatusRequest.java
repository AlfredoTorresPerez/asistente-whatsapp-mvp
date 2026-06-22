package com.asistentewhatsapp.orders.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(@NotBlank String status) {
}
