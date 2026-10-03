package com.panelvault.backend.catalog.application;

import com.panelvault.backend.catalog.domain.CatalogRepository;
import com.panelvault.backend.catalog.domain.CatalogWork;
import com.panelvault.backend.catalog.domain.WorkDetails;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.error.NotFoundException;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de los curadores: crear, editar, publicar, despublicar y borrar obras.
 *
 * <p>Que solo un CURADOR (o un ADMIN, por la jerarquia de roles) llegue aqui lo garantiza la
 * configuracion de seguridad sobre las rutas {@code /api/v1/curation/**}.
 */
@Service
public class CurationService {

    private final CatalogRepository catalog;
    private final SlugAllocator slugs;
    private final Clock clock;

    public CurationService(CatalogRepository catalog, SlugAllocator slugs, Clock clock) {
        this.catalog = catalog;
        this.slugs = slugs;
        this.clock = clock;
    }

    @Transactional
    public CatalogWork create(UserId curator, WorkDetails details) {
        CatalogWork work = CatalogWork.draft(slugs.allocate(details.title()), details, curator, clock.instant());
        return catalog.save(work);
    }

    @Transactional
    public CatalogWork update(UUID id, WorkDetails details) {
        CatalogWork work = get(id);
        work.update(details, clock.instant());
        return catalog.save(work);
    }

    @Transactional
    public CatalogWork publish(UUID id) {
        CatalogWork work = get(id);
        work.publish(clock.instant());
        return catalog.save(work);
    }

    @Transactional
    public CatalogWork unpublish(UUID id) {
        CatalogWork work = get(id);
        work.unpublish(clock.instant());
        return catalog.save(work);
    }

    @Transactional
    public void delete(UUID id) {
        catalog.delete(get(id).id());
    }

    @Transactional(readOnly = true)
    public CatalogWork get(UUID id) {
        return catalog.findById(id)
                .orElseThrow(() -> new NotFoundException("catalog.work_not_found", "No existe esa obra en el catalogo"));
    }

    @Transactional(readOnly = true)
    public PageResult<CatalogWork> list(String search, PageQuery page) {
        return catalog.findAll(search, page);
    }
}
