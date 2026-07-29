package com.asistentewhatsapp.landing.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PublicCenterResponse(CompanyInfo company, List<ServiceItem> services, List<PromotionItem> promotions,
		List<LocationItem> locations, WhatsAppInfo whatsapp, PageConfig page) {

	public record CompanyInfo(String name, String businessName, String description, String email, String phone,
			String address) {
	}

	public record ServiceItem(UUID id, String name, String description, String categoryCode, String categoryName,
			Integer durationMinutes, BigDecimal priceBase) {
	}

	public record PromotionItem(UUID id, String name, String description, String discountType, BigDecimal discountValue,
			LocalDate startsOn, LocalDate endsOn) {
	}

	public record LocationItem(UUID id, String name, String address, String city, String commune, String phone,
			String timezone) {
	}

	public record WhatsAppInfo(String waUrl, String phoneNumber, String displayPhoneNumber, String prefilledMessage) {
	}

	public record PageConfig(String primaryColor, String secondaryColor, String welcomeTitle, String welcomeSubtitle,
			String aboutTitle, String aboutText, List<Benefit> benefits, List<Testimonial> testimonials,
			String headerLogoUrl, String heroImageUrl, boolean showServices, boolean showPromotions,
			boolean showTestimonials) {
	}

	public record Benefit(String icon, String title, String text) {
	}

	public record Testimonial(String name, String text, Integer rating) {
	}
}
