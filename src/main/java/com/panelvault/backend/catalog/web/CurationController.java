package com.panelvault.backend.catalog.web;

import com.panelvault.backend.catalog.application.CurationService;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracion del catalogo ({@code /api/v1/curation}). Exige rol CURADOR (o ADMIN); la regla
 * esta en {@code SecurityConfiguration}, en el backend.
 */
@RestController
@RequestMapping("/api/v1/curation/works")
public class CurationController {

    private final CurationService curation;

    public CurationController(CurationService curation) {
        this.curation = curation;
    }

    @GetMapping
    public PageResult<WorkResponse> list(
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return curation.list(search, PageQuery.of(page, size)).map(WorkResponse::from);
    }

    @PostMapping
    public ResponseEntity<WorkResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WorkRequest request) {
        CatalogWork work = curation.create(new UserId(UUID.fromString(jwt.getSubject())), request.toDetails());
        return ResponseEntity.created(URI.create("/api/v1/curation/works/" + work.id())).body(WorkResponse.from(work));
    }

    @GetMapping("/{id}")
    public WorkResponse get(@PathVariable UUID id) {
        return WorkResponse.from(curation.get(id));
    }

    @PutMapping("/{id}")
    public WorkResponse update(@PathVariable UUID id, @Valid @RequestBody WorkRequest request) {
        return WorkResponse.from(curation.update(id, request.toDetails()));
    }

    @PostMapping("/{id}/publish")
    public WorkResponse publish(@PathVariable UUID id) {
        return WorkResponse.from(curation.publish(id));
    }

    @PostMapping("/{id}/unpublish")
    public WorkResponse unpublish(@PathVariable UUID id) {
        return WorkResponse.from(curation.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        curation.delete(id);
    }
}
