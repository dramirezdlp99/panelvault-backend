package com.panelvault.backend.catalog.application;

import com.panelvault.backend.catalog.domain.CatalogRepository;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.WorkSlug;
import com.panelvault.backend.shared.error.InvalidInputException;
import com.panelvault.backend.shared.error.NotFoundException;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas publicas del catalogo (sin sesion). Solo muestran obras publicadas. */
@Service
public class CatalogService {

    private final CatalogRepository catalog;

    public CatalogService(CatalogRepository catalog) {
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public PageResult<CatalogWork> list(String search, PageQuery page) {
        return catalog.findPublished(search, page);
    }

    /** Una obra en borrador responde 404, igual que una inexistente. */
    @Transactional(readOnly = true)
    public CatalogWork get(String slug) {
        WorkSlug parsed;
        try {
            parsed = new WorkSlug(slug);
        } catch (InvalidInputException e) {
            throw notFound();
        }
        return catalog.findBySlug(parsed).filter(CatalogWork::published).orElseThrow(CatalogService::notFound);
    }

    @Transactional(readOnly = true)
    public List<String> publishedSlugs() {
        return catalog.publishedSlugs();
    }

    private static NotFoundException notFound() {
        return new NotFoundException("catalog.work_not_found", "No existe esa obra en el catalogo");
    }
}
