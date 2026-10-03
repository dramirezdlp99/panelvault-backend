package com.panelvault.backend.analysis.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto para guardar la imagen de una pagina mientras espera su analisis.
 *
 * <p>Es temporal a proposito: al terminar el trabajo (bien o mal) la imagen se borra. PanelVault
 * guarda los comics en el navegador del usuario (offline-first); el servidor solo conserva el mapa
 * de vinetas, que es pequeno y no contiene la obra.
 */
public interface PageImageStore {

    void save(UUID jobId, byte[] content);

    Optional<byte[]> load(UUID jobId);

    void delete(UUID jobId);
}
