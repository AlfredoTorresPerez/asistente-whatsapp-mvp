package com.asistentewhatsapp.bookings.application;

import com.asistentewhatsapp.bookings.domain.BookingPaymentProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BookingPaymentProviderRegistry {

    private final Map<String, BookingPaymentProvider> providerMap;
    private final BookingPaymentProvider defaultProvider;

    public BookingPaymentProviderRegistry(List<BookingPaymentProvider> providers,
                                          BookingPaymentProperties properties) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(
                        BookingPaymentProvider::providerName,
                        Function.identity(),
                        (existing, replacement) -> existing));
        String defaultName = properties.getProvider() != null && !properties.getProvider().isBlank()
                ? properties.getProvider().trim().toUpperCase()
                : "SIMULATED";
        this.defaultProvider = providerMap.getOrDefault(defaultName, providerMap.get("SIMULATED"));
    }

    public BookingPaymentProvider getProvider(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return defaultProvider;
        }
        return providerMap.getOrDefault(providerName.trim().toUpperCase(), defaultProvider);
    }

    public BookingPaymentProvider getDefaultProvider() {
        return defaultProvider;
    }
}
