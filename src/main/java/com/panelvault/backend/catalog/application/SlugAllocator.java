package com.panelvault.backend.catalog.application;

import com.panelvault.backend.catalog.domain.CatalogRepository;
import com.panelvault.backend.catalog.domain.WorkSlug;
import com.panelvault.backend.shared.error.ConflictException;
import org.springframework.stereotype.Component;

/**
 * Asigna un slug unico a una obra nueva. Si "krazy-kat" ya existe prueba "krazy-kat-2",
 * "krazy-kat-3"... La restriccion UNIQUE de la base sigue siendo la ultima palabra si dos curadores
 * crean a la vez obras con el mismo titulo.
 */
@Component
public class SlugAllocator {

    static final int MAX_ATTEMPTS = 50;

    private final CatalogRepository catalog;

    public SlugAllocator(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    public WorkSlug allocate(String title) {
        WorkSlug base = WorkSlug.fromTitle(title);
        if (!catalog.existsBySlug(base)) {
            return base;
        }
        for (int n = 2; n <= MAX_ATTEMPTS; n++) {
            WorkSlug candidate = base.withSuffix(n);
            if (!catalog.existsBySlug(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException("catalog.slug_exhausted", "Ya hay demasiadas obras con ese titulo");
    }
}
