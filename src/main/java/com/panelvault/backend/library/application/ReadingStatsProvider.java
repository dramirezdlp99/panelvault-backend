package com.panelvault.backend.library.application;

import com.panelvault.backend.identity.domain.UserId;

/**
 * Puerto que la biblioteca necesita para su resumen: cuantos comics empezo y termino el usuario.
 *
 * <p>Lo implementa el modulo de lectura (inversion de dependencias): la biblioteca no conoce las
 * tablas de lectura, y el modulo de lectura, que si depende de la biblioteca, le provee el dato.
 */
public interface ReadingStatsProvider {

    long comicsStarted(UserId user);

    long comicsFinished(UserId user);
}
