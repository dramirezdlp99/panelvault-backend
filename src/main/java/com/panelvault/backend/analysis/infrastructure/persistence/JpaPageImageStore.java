package com.panelvault.backend.analysis.infrastructure.persistence;

import com.panelvault.backend.analysis.domain.PageImageStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * Guarda las imagenes pendientes en PostgreSQL (columna {@code bytea}).
 *
 * <p>Es suficiente porque son temporales: se borran al terminar cada trabajo, asi que la tabla
 * nunca crece mucho. Si un dia hiciera falta, este adaptador se cambia por uno de almacenamiento
 * de objetos sin tocar el resto del sistema.
 */
@Repository
public class JpaPageImageStore implements PageImageStore {

    private final SpringDataPageImageRepository jpa;

    public JpaPageImageStore(SpringDataPageImageRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(UUID jobId, byte[] content) {
        jpa.saveAndFlush(new PageImageJpaEntity(jobId, content));
    }

    @Override
    public Optional<byte[]> load(UUID jobId) {
        return jpa.findById(jobId).map(PageImageJpaEntity::getContent);
    }

    @Override
    public void delete(UUID jobId) {
        jpa.deleteById(jobId);
        jpa.flush();
    }
}
