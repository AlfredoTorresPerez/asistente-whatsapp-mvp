package com.asistentewhatsapp.catalog.infrastructure;

import com.asistentewhatsapp.catalog.api.CatalogCategoryResponse;
import com.asistentewhatsapp.catalog.api.CatalogProductResponse;
import com.asistentewhatsapp.catalog.api.UpsertCatalogCategoryRequest;
import com.asistentewhatsapp.catalog.api.UpsertCatalogProductRequest;
import com.asistentewhatsapp.shared.api.PagedResponse;
import com.asistentewhatsapp.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CatalogJdbcRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CatalogJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PagedResponse<CatalogProductResponse> findProducts(
            UUID businessId,
            int page,
            int size,
            String search,
            String categoryCode,
            Boolean active) {
        QueryParts queryParts = productQuery(businessId, search, categoryCode, active);
        Long total = jdbcTemplate.queryForObject("select count(*) " + queryParts.fromAndWhere(), queryParts.parameters(), Long.class);
        long totalItems = total == null ? 0 : total;
        MapSqlParameterSource parameters = queryParts.parameters()
                .addValue("limit", size)
                .addValue("offset", page * size);
        List<CatalogProductResponse> items = jdbcTemplate.query(productSelect() + queryParts.fromAndWhere() + """
                order by ps.created_at desc
                limit :limit
                offset :offset
                """, parameters, productRowMapper());
        return new PagedResponse<>(items, page, size, totalItems, totalPages(totalItems, size));
    }

    public CatalogProductResponse findProduct(UUID businessId, UUID productId) {
        List<CatalogProductResponse> items = jdbcTemplate.query(productSelect() + """
                from product_service ps
                join product_category pc on pc.id = ps.category_id and pc.business_id = ps.business_id
                where ps.business_id = :businessId
                  and ps.id = :productId
                  and ps.type = 'PRODUCT'
                """, new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("productId", productId), productRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro el producto solicitado.");
        }
        return items.getFirst();
    }

    public CatalogProductResponse insertProduct(UUID businessId, UpsertCatalogProductRequest request) {
        UUID productId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into product_service (
                    id, business_id, category_id, type, name, sku, description, price, duration_minutes,
                    active, stock_quantity, stock_minimum, supplier, expires_at
                )
                values (
                    :productId, :businessId,
                    (select id from product_category where business_id = :businessId and code = :categoryCode),
                    'PRODUCT', :name, :sku, :description, :price, null,
                    :active, :stock, :stockMinimum, :supplier, :expiresAt
                )
                """, productParameters(businessId, productId, request));
        return findProduct(businessId, productId);
    }

    public CatalogProductResponse updateProduct(UUID businessId, UUID productId, UpsertCatalogProductRequest request) {
        int updated = jdbcTemplate.update("""
                update product_service
                set category_id = (select id from product_category where business_id = :businessId and code = :categoryCode),
                    name = :name,
                    sku = :sku,
                    description = :description,
                    price = :price,
                    active = :active,
                    stock_quantity = :stock,
                    stock_minimum = :stockMinimum,
                    supplier = :supplier,
                    expires_at = :expiresAt,
                    updated_at = current_timestamp
                where business_id = :businessId
                  and id = :productId
                  and type = 'PRODUCT'
                """, productParameters(businessId, productId, request));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro el producto solicitado.");
        }
        return findProduct(businessId, productId);
    }

    public CatalogProductResponse updateProductStatus(UUID businessId, UUID productId, boolean active) {
        int updated = jdbcTemplate.update("""
                update product_service
                set active = :active,
                    updated_at = current_timestamp
                where business_id = :businessId
                  and id = :productId
                  and type = 'PRODUCT'
                """, new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("productId", productId)
                .addValue("active", active));
        if (updated == 0) {
            throw new ResourceNotFoundException("No se encontro el producto solicitado.");
        }
        return findProduct(businessId, productId);
    }

    public PagedResponse<CatalogCategoryResponse> findCategories(UUID businessId, int page, int size, Boolean active) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("limit", size)
                .addValue("offset", page * size);
        StringBuilder where = new StringBuilder(" where business_id = :businessId\n");
        if (active != null) {
            where.append(" and active = :active\n");
            parameters.addValue("active", active);
        }
        Long total = jdbcTemplate.queryForObject("select count(*) from product_category" + where, parameters, Long.class);
        long totalItems = total == null ? 0 : total;
        List<CatalogCategoryResponse> items = jdbcTemplate.query("""
                select id, code, name, description, active
                from product_category
                """ + where + """
                order by name asc
                limit :limit
                offset :offset
                """, parameters, categoryRowMapper());
        return new PagedResponse<>(items, page, size, totalItems, totalPages(totalItems, size));
    }

    public CatalogCategoryResponse insertCategory(UUID businessId, UpsertCatalogCategoryRequest request) {
        UUID categoryId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into product_category (id, business_id, code, name, description, active)
                values (:categoryId, :businessId, :code, :name, :description, :active)
                """, new MapSqlParameterSource()
                .addValue("categoryId", categoryId)
                .addValue("businessId", businessId)
                .addValue("code", normalizeCode(request.code()))
                .addValue("name", request.name().trim())
                .addValue("description", clean(request.description()))
                .addValue("active", request.active() == null || request.active()));
        return findCategory(businessId, categoryId);
    }

    public CatalogCategoryResponse findCategory(UUID businessId, UUID categoryId) {
        List<CatalogCategoryResponse> items = jdbcTemplate.query("""
                select id, code, name, description, active
                from product_category
                where business_id = :businessId
                  and id = :categoryId
                """, new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("categoryId", categoryId), categoryRowMapper());
        if (items.isEmpty()) {
            throw new ResourceNotFoundException("No se encontro la categoria solicitada.");
        }
        return items.getFirst();
    }

    public boolean categoryExists(UUID businessId, String categoryCode) {
        Boolean exists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1 from product_category where business_id = :businessId and code = :categoryCode and active = true
                )
                """, new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("categoryCode", normalizeCode(categoryCode)), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private QueryParts productQuery(UUID businessId, String search, String categoryCode, Boolean active) {
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("businessId", businessId);
        StringBuilder where = new StringBuilder("""
                from product_service ps
                join product_category pc on pc.id = ps.category_id and pc.business_id = ps.business_id
                where ps.business_id = :businessId
                  and ps.type = 'PRODUCT'
                """);
        if (search != null && !search.isBlank()) {
            where.append("""
                    and (
                        lower(ps.name) like :search
                        or lower(ps.sku) like :search
                        or lower(coalesce(ps.description, '')) like :search
                        or lower(pc.name) like :search
                    )
                    """);
            parameters.addValue("search", "%" + search.toLowerCase(Locale.ROOT).trim() + "%");
        }
        if (categoryCode != null && !categoryCode.isBlank()) {
            where.append(" and pc.code = :categoryCode\n");
            parameters.addValue("categoryCode", normalizeCode(categoryCode));
        }
        if (active != null) {
            where.append(" and ps.active = :active\n");
            parameters.addValue("active", active);
        }
        return new QueryParts(where.toString(), parameters);
    }

    private String productSelect() {
        return """
                select
                    ps.id,
                    pc.id as category_id,
                    pc.code as category_code,
                    pc.name as category_name,
                    ps.sku,
                    ps.name,
                    ps.description,
                    ps.price,
                    ps.stock_quantity,
                    ps.stock_minimum,
                    ps.supplier,
                    ps.expires_at,
                    ps.active,
                    ps.created_at,
                    ps.updated_at
                """;
    }

    private MapSqlParameterSource productParameters(UUID businessId, UUID productId, UpsertCatalogProductRequest request) {
        return new MapSqlParameterSource()
                .addValue("businessId", businessId)
                .addValue("productId", productId)
                .addValue("categoryCode", normalizeCode(request.categoryCode()))
                .addValue("sku", normalizeSku(request.sku(), request.name()))
                .addValue("name", request.name().trim())
                .addValue("description", clean(request.description()))
                .addValue("price", request.price())
                .addValue("stock", request.stock() == null ? 0 : request.stock())
                .addValue("stockMinimum", request.stockMinimum() == null ? 0 : request.stockMinimum())
                .addValue("supplier", clean(request.supplier()))
                .addValue("expiresAt", request.expiresAt())
                .addValue("active", request.active() == null || request.active());
    }

    private RowMapper<CatalogProductResponse> productRowMapper() {
        return (rs, rowNum) -> new CatalogProductResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("category_id", UUID.class),
                rs.getString("category_code"),
                rs.getString("category_name"),
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBigDecimal("price"),
                rs.getInt("stock_quantity"),
                rs.getInt("stock_minimum"),
                rs.getInt("stock_quantity") <= rs.getInt("stock_minimum"),
                rs.getString("supplier"),
                getLocalDate(rs, "expires_at"),
                rs.getBoolean("active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private RowMapper<CatalogCategoryResponse> categoryRowMapper() {
        return (rs, rowNum) -> new CatalogCategoryResponse(
                rs.getObject("id", UUID.class),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"));
    }

    private int totalPages(long totalItems, int size) {
        return totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
    }

    private LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private String normalizeSku(String sku, String name) {
        if (sku != null && !sku.isBlank()) {
            return sku.trim().toUpperCase(Locale.ROOT);
        }
        return name.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record QueryParts(String fromAndWhere, MapSqlParameterSource parameters) {
    }
}
