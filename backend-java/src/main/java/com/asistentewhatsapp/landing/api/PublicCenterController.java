package com.asistentewhatsapp.landing.api;

import com.asistentewhatsapp.landing.application.PublicCenterService;
import com.asistentewhatsapp.landing.api.PublicCenterResponse.WhatsAppInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/public/centros", produces = MediaType.APPLICATION_JSON_VALUE)
public class PublicCenterController {

    private final PublicCenterService publicCenterService;

    public PublicCenterController(PublicCenterService publicCenterService) {
        this.publicCenterService = publicCenterService;
    }

    @GetMapping("/{slug}")
    public PublicCenterResponse getCenter(@PathVariable String slug) {
        return publicCenterService.getCenterBySlug(slug);
    }

    @PostMapping("/{slug}/contacto")
    public Map<String, String> saveContact(
            @PathVariable String slug,
            @Valid @RequestBody PublicContactRequest request) {
        String waUrl = publicCenterService.saveContact(slug, request.name(), request.phone());
        return Map.of("waUrl", waUrl);
    }

    @GetMapping("/{slug}/whatsapp")
    public ResponseEntity<Void> redirectWhatsApp(
            @PathVariable String slug,
            HttpServletRequest request) {
        WhatsAppInfo whatsapp = publicCenterService.getWhatsAppInfo(slug);

        if (whatsapp == null || whatsapp.waUrl() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        publicCenterService.registerClick(slug,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                request.getHeader("Referer"));

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(whatsapp.waUrl()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
