package com.panelvault.backend.library.web;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.library.application.LibraryService;
import com.panelvault.backend.library.application.UpsertResult;
import com.panelvault.backend.library.domain.ComicId;
import com.panelvault.backend.library.domain.LibraryStats;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Biblioteca del usuario autenticado ({@code /api/v1/library}).
 *
 * <p>Se usa PUT con el id generado por el cliente (y no POST) porque PUT es idempotente: repetir la
 * misma peticion deja el mismo resultado. Es lo que necesita una sincronizacion offline que
 * reintenta cuando vuelve la conexion.
 */
@RestController
@RequestMapping("/api/v1/library")
public class LibraryController {

    private final LibraryService library;

    public LibraryController(LibraryService library) {
        this.library = library;
    }

    @PutMapping("/comics/{comicId}")
    public ResponseEntity<ComicResponse> save(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String comicId, @Valid @RequestBody ComicRequest request) {
        UpsertResult result = library.save(userOf(jwt), ComicId.parse(comicId), request.toDetails());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ComicResponse.from(result.comic()));
    }

    @GetMapping("/comics")
    public PageResult<ComicResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        return library.list(userOf(jwt), search, PageQuery.of(page, size)).map(ComicResponse::from);
    }

    @GetMapping("/comics/{comicId}")
    public ComicResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String comicId) {
        return ComicResponse.from(library.get(userOf(jwt), ComicId.parse(comicId)));
    }

    @DeleteMapping("/comics/{comicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String comicId) {
        library.delete(userOf(jwt), ComicId.parse(comicId));
    }

    @GetMapping("/stats")
    public LibraryStats stats(@AuthenticationPrincipal Jwt jwt) {
        return library.stats(userOf(jwt));
    }

    private static UserId userOf(Jwt jwt) {
        return new UserId(UUID.fromString(jwt.getSubject()));
    }
}
