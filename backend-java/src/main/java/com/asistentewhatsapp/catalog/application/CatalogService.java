package com.asistentewhatsapp.catalog.application;

import com.asistentewhatsapp.catalog.api.CatalogCategoryResponse;
import com.asistentewhatsapp.catalog.api.CatalogProductResponse;
import com.asistentewhatsapp.catalog.api.UpsertCatalogCategoryRequest;
import com.asistentewhatsapp.catalog.api.UpsertCatalogProductRequest;
import com.asistentewhatsapp.catalog.infrastructure.CatalogJdbcRepository;
import com.asistentewhatsapp.security.domain.AuthenticatedUser;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CatalogService {

	private final CatalogJdbcRepository catalogJdbcRepository;

	public CatalogService(CatalogJdbcRepository catalogJdbcRepository) {
		this.catalogJdbcRepository = catalogJdbcRepository;
	}

	@Transactional(readOnly = true)
	public PagedResponse<CatalogProductResponse> listProducts(AuthenticatedUser authenticatedUser, int page, int size,
			String search, String categoryCode, Boolean active) {
		int resolvedPage = Math.max(page, 0);
		int resolvedSize = Math.min(Math.max(size, 1), 100);
		return catalogJdbcRepository.findProducts(authenticatedUser.businessId(), resolvedPage, resolvedSize, search,
				categoryCode, active);
	}

	@Transactional(readOnly = true)
	public CatalogProductResponse getProduct(AuthenticatedUser authenticatedUser, UUID productId) {
		return catalogJdbcRepository.findProduct(authenticatedUser.businessId(), productId);
	}

	@Transactional
	public CatalogProductResponse createProduct(AuthenticatedUser authenticatedUser,
			UpsertCatalogProductRequest request) {
		validateProduct(authenticatedUser, request);
		return catalogJdbcRepository.insertProduct(authenticatedUser.businessId(), request);
	}

	@Transactional
	public CatalogProductResponse updateProduct(AuthenticatedUser authenticatedUser, UUID productId,
			UpsertCatalogProductRequest request) {
		validateProduct(authenticatedUser, request);
		return catalogJdbcRepository.updateProduct(authenticatedUser.businessId(), productId, request);
	}

	@Transactional
	public CatalogProductResponse updateProductStatus(AuthenticatedUser authenticatedUser, UUID productId,
			boolean active) {
		return catalogJdbcRepository.updateProductStatus(authenticatedUser.businessId(), productId, active);
	}

	@Transactional(readOnly = true)
	public PagedResponse<CatalogCategoryResponse> listCategories(AuthenticatedUser authenticatedUser, int page,
			int size, Boolean active) {
		int resolvedPage = Math.max(page, 0);
		int resolvedSize = Math.min(Math.max(size, 1), 100);
		return catalogJdbcRepository.findCategories(authenticatedUser.businessId(), resolvedPage, resolvedSize, active);
	}

	@Transactional
	public CatalogCategoryResponse createCategory(AuthenticatedUser authenticatedUser,
			UpsertCatalogCategoryRequest request) {
		return catalogJdbcRepository.insertCategory(authenticatedUser.businessId(), request);
	}

	private void validateProduct(AuthenticatedUser authenticatedUser, UpsertCatalogProductRequest request) {
		if (!catalogJdbcRepository.categoryExists(authenticatedUser.businessId(), request.categoryCode())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "CATALOG_CATEGORY_NOT_FOUND",
					"La categoria del producto no existe o no esta activa.");
		}
	}
}
