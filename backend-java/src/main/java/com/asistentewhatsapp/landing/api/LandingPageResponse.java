package com.asistentewhatsapp.landing.api;

import java.util.List;

public record LandingPageResponse(
        LandingCompanyResponse company,
        List<LandingServiceItemResponse> services,
        List<LandingLocationItemResponse> locations) {

    public record LandingCompanyResponse(
            String companyName,
            String businessName,
            String timezone,
            String currency,
            String contactEmail,
            String supportPhone,
            String address) {
    }
}
