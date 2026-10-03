package com.panelvault.backend.catalog.web;

import com.panelvault.backend.catalog.application.CatalogService;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.time.Duration;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogo publico de comics libres ({@code /api/v1/catalog}). No requiere sesion.
 *
 * <p>Lo consumen las paginas ISR del frontend ({@code /explorar}). Las respuestas llevan
 * {@code Cache-Control} publico: un CDN o el propio Next.js pueden guardarlas un rato y servir la
 * copia mientras la renuevan en segundo plano ({@code stale-while-revalidate}), la misma idea de ISR.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofSeconds(60))
            .cachePublic()
            .staleWhileRevalidate(Duration.ofMinutes(5));

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/works")
    public ResponseEntity<PageResult<WorkResponse>> list(
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        PageResult<WorkResponse> result = catalog.list(search, PageQuery.of(page, size)).map(WorkResponse::from);
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(result);
    }

    @GetMapping("/works/{slug}")
    public ResponseEntity<WorkResponse> get(@PathVariable String slug) {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(WorkResponse.from(catalog.get(slug)));
    }

    @GetMapping("/slugs")
    public ResponseEntity<List<String>> slugs() {
        return ResponseEntity.ok().cacheControl(PUBLIC_CACHE).body(catalog.publishedSlugs());
    }
}
