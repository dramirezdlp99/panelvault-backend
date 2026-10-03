package com.panelvault.backend.library.domain;

/** Formato del archivo original. El navegador lo descomprime; el servidor solo lo registra. */
public enum ComicFormat {
    /** Archivo ZIP con imagenes. */
    CBZ,
    /** Archivo RAR con imagenes. */
    CBR,
    /** Documento PDF. */
    PDF,
    /** Carpeta o seleccion de imagenes sueltas. */
    IMAGES
}
