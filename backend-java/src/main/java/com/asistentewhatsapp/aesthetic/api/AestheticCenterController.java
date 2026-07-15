package com.asistentewhatsapp.aesthetic.api;

import com.asistentewhatsapp.aesthetic.application.AestheticCenterService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class AestheticCenterController {

    private final AestheticCenterService aestheticCenterService;

    public AestheticCenterController(AestheticCenterService aestheticCenterService) {
        this.aestheticCenterService = aestheticCenterService;
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/service-categories", "/api/v1/esthetic/service-categories"})
    public PagedResponse<AestheticCategoryResponse> listServiceCategories(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Boolean active) {
        return aestheticCenterService.listServiceCategories(authenticatedUser, page, size, active);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/product-categories", "/api/v1/esthetic/product-categories"})
    public PagedResponse<AestheticCategoryResponse> listProductCategories(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) Boolean active) {
        return aestheticCenterService.listProductCategories(authenticatedUser, page, size, active);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/services", "/api/v1/esthetic/services"})
    public PagedResponse<AestheticServiceResponse> listServices(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Boolean active) {
        return aestheticCenterService.listServices(authenticatedUser, page, size, search, categoryCode, active);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/services/{serviceId}", "/api/v1/esthetic/services/{serviceId}"})
    public AestheticServiceResponse getService(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID serviceId) {
        return aestheticCenterService.getService(authenticatedUser, serviceId);
    }



    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
    @PostMapping(
            value = {"/api/esthetic/services", "/api/v1/esthetic/services"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticServiceResponse createService(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertAestheticServiceRequest request) {
        return aestheticCenterService.createService(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
    @PutMapping(
            value = {"/api/esthetic/services/{serviceId}", "/api/v1/esthetic/services/{serviceId}"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticServiceResponse updateService(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID serviceId,
            @Valid @RequestBody UpsertAestheticServiceRequest request) {
        return aestheticCenterService.updateService(authenticatedUser, serviceId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/products", "/api/v1/esthetic/products"})
    public PagedResponse<AestheticProductResponse> listProducts(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean lowStockOnly) {
        return aestheticCenterService.listProducts(authenticatedUser, page, size, search, categoryCode, active, lowStockOnly);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
    @GetMapping({"/api/esthetic/products/{productId}", "/api/v1/esthetic/products/{productId}"})
    public AestheticProductResponse getProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID productId) {
        return aestheticCenterService.getProduct(authenticatedUser, productId);
    }



    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
    @PostMapping(
            value = {"/api/esthetic/products", "/api/v1/esthetic/products"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticProductResponse createProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertAestheticProductRequest request) {
        return aestheticCenterService.createProduct(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
    @PutMapping(
            value = {"/api/esthetic/products/{productId}", "/api/v1/esthetic/products/{productId}"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticProductResponse updateProduct(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID productId,
            @Valid @RequestBody UpsertAestheticProductRequest request) {
        return aestheticCenterService.updateProduct(authenticatedUser, productId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @GetMapping({"/api/esthetic/rules", "/api/v1/esthetic/rules"})
    public PagedResponse<AestheticBusinessRuleResponse> listRules(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Boolean active) {
        return aestheticCenterService.listRules(authenticatedUser, page, size, ruleType, active);
    }



    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @GetMapping({"/api/esthetic/rules/{ruleId}", "/api/v1/esthetic/rules/{ruleId}"})
    public AestheticBusinessRuleResponse getRule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID ruleId) {
        return aestheticCenterService.getRule(authenticatedUser, ruleId);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @PostMapping(
            value = {"/api/esthetic/rules", "/api/v1/esthetic/rules"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticBusinessRuleResponse createRule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertAestheticBusinessRuleRequest request) {
        return aestheticCenterService.createRule(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @PutMapping(
            value = {"/api/esthetic/rules/{ruleId}", "/api/v1/esthetic/rules/{ruleId}"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public AestheticBusinessRuleResponse updateRule(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpsertAestheticBusinessRuleRequest request) {
        return aestheticCenterService.updateRule(authenticatedUser, ruleId, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @PostMapping(
            value = {"/api/esthetic/intent/analyze", "/api/v1/esthetic/intent/analyze"},
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public IntentAnalysisResponse analyzeIntent(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody IntentAnalysisRequest request) {
        return aestheticCenterService.analyzeIntent(authenticatedUser, request);
    }

    @PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'AUTOMATION_MANAGE')")
    @GetMapping({"/api/esthetic/intent/logs", "/api/v1/esthetic/intent/logs"})
    public PagedResponse<AestheticIntentLogResponse> listIntentLogs(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return aestheticCenterService.listIntentLogs(authenticatedUser, page, size);
    }
}
