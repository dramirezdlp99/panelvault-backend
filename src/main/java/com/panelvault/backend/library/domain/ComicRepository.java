package com.panelvault.backend.library.domain;

import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.shared.paging.PageQuery;
import com.panelvault.backend.shared.paging.PageResult;
import java.util.Optional;

/** Puerto de salida de la biblioteca. */
public interface ComicRepository {

    Optional<Comic> findById(ComicId id);

    Optional<Comic> findByOwnerAndFingerprint(UserId owner, FileFingerprint fingerprint);

    /**
     * Comics de un usuario, del modificado mas recientemente al mas antiguo.
     *
     * @param search texto a buscar en titulo o serie; vacio o {@code null} para no filtrar
     */
    PageResult<Comic> findByOwner(UserId owner, String search, PageQuery page);

    Comic save(Comic comic);

    void delete(ComicId id);

    long countByOwner(UserId owner);

    long totalPagesByOwner(UserId owner);
}
