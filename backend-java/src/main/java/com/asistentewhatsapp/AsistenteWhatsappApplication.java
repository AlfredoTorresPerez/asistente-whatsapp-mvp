package com.asistentewhatsapp;

import com.asistentewhatsapp.bookings.application.BookingConfirmationProperties;
import com.asistentewhatsapp.bookings.application.BookingPaymentProperties;
import com.asistentewhatsapp.bookings.application.BookingSyncProperties;
import com.asistentewhatsapp.channels.application.WhatsAppChannelProperties;
import com.asistentewhatsapp.security.AppSecurityProperties;
import com.asistentewhatsapp.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AppSecurityProperties.class, JwtProperties.class, WhatsAppChannelProperties.class, BookingConfirmationProperties.class, BookingPaymentProperties.class, BookingSyncProperties.class})
public class AsistenteWhatsappApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsistenteWhatsappApplication.class, args);
    }
}
