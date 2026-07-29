package com.asistentewhatsapp.catalog.api;

import com.asistentewhatsapp.catalog.application.CatalogService;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
	@GetMapping({"/api/catalog/products", "/api/v1/catalog/products"})
	public PagedResponse<CatalogProductResponse> listProducts(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String search,
			@RequestParam(required = false) String categoryCode, @RequestParam(required = false) Boolean active) {
		return catalogService.listProducts(authenticatedUser, page, size, search, categoryCode, active);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
	@PostMapping(value = {"/api/catalog/products",
			"/api/v1/catalog/products"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public CatalogProductResponse createProduct(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertCatalogProductRequest request) {
		return catalogService.createProduct(authenticatedUser, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
	@GetMapping({"/api/catalog/products/{id}", "/api/v1/catalog/products/{id}"})
	public CatalogProductResponse getProduct(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id) {
		return catalogService.getProduct(authenticatedUser, id);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
	@PutMapping(value = {"/api/catalog/products/{id}",
			"/api/v1/catalog/products/{id}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public CatalogProductResponse updateProduct(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody UpsertCatalogProductRequest request) {
		return catalogService.updateProduct(authenticatedUser, id, request);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
	@PatchMapping(value = {"/api/catalog/products/{id}/status",
			"/api/v1/catalog/products/{id}/status"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public CatalogProductResponse updateProductStatus(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@PathVariable UUID id, @Valid @RequestBody UpdateCatalogProductStatusRequest request) {
		return catalogService.updateProductStatus(authenticatedUser, id, request.active());
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_VIEW')")
	@GetMapping({"/api/catalog/categories", "/api/v1/catalog/categories"})
	public PagedResponse<CatalogCategoryResponse> listCategories(
			@AuthenticationPrincipal AuthenticatedUser authenticatedUser, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "100") int size, @RequestParam(required = false) Boolean active) {
		return catalogService.listCategories(authenticatedUser, page, size, active);
	}

	@PreAuthorize("hasPermission(#authenticatedUser.businessId(), 'CATALOG_MANAGE')")
	@PostMapping(value = {"/api/catalog/categories",
			"/api/v1/catalog/categories"}, consumes = MediaType.APPLICATION_JSON_VALUE)
	public CatalogCategoryResponse createCategory(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
			@Valid @RequestBody UpsertCatalogCategoryRequest request) {
		return catalogService.createCategory(authenticatedUser, request);
	}
}
