# PanelVault · Backend

API REST de **PanelVault**, gestor de cómics digitales *offline-first* con análisis visual inteligente.
Gestiona identidad y seguridad, la biblioteca y el progreso de lectura de cada usuario, el catálogo
público de cómics libres y la **cola de análisis** que conecta con el motor de IA que detecta las
viñetas y su orden de lectura.

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1 (Spring MVC, Spring Security, Spring Data JPA) |
| Base de datos | PostgreSQL 17 con migraciones Flyway |
| Seguridad | JWT HS256, refresh tokens rotativos, 2FA TOTP, RBAC, firma HMAC |
| Pruebas | JUnit 5, AssertJ, MockMvc, PostgreSQL real |

Repositorios del proyecto (independientes): `panelvault-frontend` (Next.js), `panelvault-backend`
(este) y `panelvault-ai-engine` (Python, OpenCV).

---

## Arquitectura

El backend sigue una **arquitectura hexagonal (puertos y adaptadores) por módulo**:

```
com.panelvault.backend
├── shared      Errores uniformes, paginación, criptografía (Base32, TOTP), reloj
├── edge        Capa de borde: firma HMAC del gateway (Next.js) y CORS
├── identity    Registro, login, JWT, refresh rotativo, 2FA, límite de intentos, roles
├── library     Biblioteca de cómics de cada usuario (solo metadatos)
├── reading     Progreso de lectura y marcadores sincronizados entre dispositivos
├── analysis    Cola de análisis de viñetas y caché de resultados
└── catalog     Catálogo público de cómics libres y su curaduría
```

Cada módulo separa sus capas:

| Capa | Contiene | Depende de |
|---|---|---|
| `domain` | Entidades, objetos de valor, reglas y **puertos** (interfaces) | Nada de Spring ni JPA |
| `application` | Casos de uso que orquestan el dominio | `domain` |
| `infrastructure` | **Adaptadores**: JPA, cliente HTTP del motor, JWT, cifrado | `domain`, `application` |
| `web` | Controladores REST y DTO de entrada y salida | `application` |

Patrones de diseño usados: Repository, Adapter, Factory Method, Value Object, Specification
(política de contraseñas), Command, Strategy (puertos intercambiables), Decorator (petición con
cuerpo cacheado), tipos sellados para resultados, Test Fixture y Fake en las pruebas.

### Encaje en la arquitectura de 7 capas de PanelVault

| Capa | Dónde |
|---|---|
| Cliente | `panelvault-frontend` (Next.js, Service Worker, IndexedDB) |
| Edge/Gateway | Servidor de Next.js (BFF) que firma con HMAC + paquete `edge` de este backend |
| Aplicación | Este backend |
| Asíncrona | Cola `analysis_jobs` en PostgreSQL y su worker programado |
| IA | `panelvault-ai-engine`, llamado por HTTP firmado con HMAC |
| Datos | PostgreSQL (Neon en producción) |
| DevOps | Docker, Render, Vercel |

---

## Decisiones técnicas destacadas

**Seguridad**
- Contraseñas con **BCrypt** (factor 12) y política de longitud, letras y dígitos. Se rechazan las
  contraseñas de más de 72 bytes, porque BCrypt truncaría el resto en silencio.
- **Access token JWT de 15 minutos** y **refresh token opaco de 7 días**. Del refresh token solo se
  guarda su SHA-256.
- **Rotación con detección de reúso**: si un refresh token ya usado reaparece, se revoca toda su
  familia de sesión. La revocación se conserva aunque la petición falle (`noRollbackFor`).
- **Login sin enumeración de cuentas**: el mismo error y el mismo tiempo de respuesta (hash de
  relleno) existan o no las cuentas.
- **2FA TOTP** (RFC 6238 implementado a mano y verificado con los vectores oficiales):
  - Secreto cifrado con **AES-256-GCM**, ligado al usuario.
  - 10 códigos de recuperación guardados como huella HMAC.
  - Protección contra repetición de códigos.
  - Ticket del segundo paso firmado con una clave distinta a la de los access tokens.
- **Límite de intentos** con ventana deslizante: 5 fallos en 15 minutos → `429` con `Retry-After`.
- **RBAC** con jerarquía `ADMIN > CURADOR > LECTOR`, validado siempre en el backend.
- **Gateway HMAC**: en producción, el backend solo acepta peticiones firmadas por el servidor de
  Next.js.
- **Errores uniformes**: el mismo JSON en toda la API, sin trazas internas (los 500 llevan una
  referencia para el log).

**Cola de análisis**
- `SELECT … FOR UPDATE SKIP LOCKED`: varios workers trabajan en paralelo sin tomar el mismo trabajo.
- **Leases con token de cerca**: si un worker muere, el trabajo se retoma. Si un worker lento
  responde tarde, su resultado se descarta.
- **Reintentos con espera exponencial y jitter**, distinguiendo fallos temporales de definitivos.
- **Caché por SHA-256 de la página**: la misma página se analiza una sola vez para todos.
- Las imágenes se borran al terminar: el servidor solo guarda el mapa de viñetas.
- Antes de cada lote se "despierta" al motor de IA (Render Free duerme los servicios sin uso).

**Sincronización offline-first**
- Los ids de cómics y marcadores los genera el cliente, y las escrituras (`PUT`) son idempotentes:
  reintentar al recuperar la conexión nunca duplica.
- El progreso de lectura resuelve conflictos entre dispositivos con **last-writer-wins**, con
  desempate determinista y límite para relojes adelantados.

---

## API

Todas las rutas cuelgan de `/api/v1`. Salvo las públicas, requieren `Authorization: Bearer <token>`.

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| POST | `/auth/register` | Público | Crear cuenta |
| POST | `/auth/login` | Público | Login; puede pedir el segundo paso (2FA) |
| POST | `/auth/2fa/verify` | Público (ticket) | Segundo paso del login |
| POST | `/auth/refresh` | Público (refresh token) | Rotar tokens |
| POST | `/auth/logout` | Público (refresh token) | Cerrar la sesión |
| GET | `/me` | Lector | Perfil del usuario |
| GET · POST | `/me/2fa`, `/me/2fa/setup`, `/me/2fa/confirm`, `/me/2fa/disable` | Lector | Gestionar la 2FA |
| PATCH | `/admin/users/{id}/role` | Admin | Cambiar el rol de un usuario |
| PUT · GET · DELETE | `/library/comics/{id}` | Lector | Guardar (idempotente), ver y borrar un cómic |
| GET | `/library/comics?q=&page=&size=` | Lector | Listar y buscar la biblioteca |
| GET | `/library/stats` | Lector | Resumen de la biblioteca |
| PUT · GET | `/reading/comics/{id}/progress` | Lector | Sincronizar y leer el progreso |
| GET | `/reading/recent` | Lector | Continuar leyendo |
| PUT · GET | `/reading/comics/{id}/bookmarks[/{bookmarkId}]` | Lector | Marcadores |
| DELETE | `/reading/bookmarks/{bookmarkId}` | Lector | Borrar un marcador |
| POST | `/analysis/pages?preset=western\|manga` | Lector | Subir una página (`200` en caché, `202` en cola) |
| GET | `/analysis/jobs/{id}` | Lector | Estado del análisis |
| GET | `/analysis/results/{sha256}?preset=` | Lector | Mapa de viñetas por huella |
| GET | `/catalog/works`, `/catalog/works/{slug}`, `/catalog/slugs` | Público | Catálogo de cómics libres |
| GET · POST · PUT · DELETE | `/curation/works[/{id}][/publish\|/unpublish]` | Curador | Administrar el catálogo |
| GET | `/actuator/health` | Público | Salud del servicio |

Formato de error (igual en toda la API):

```json
{
  "status": 409,
  "code": "user.email_taken",
  "message": "Ya existe una cuenta con ese correo",
  "path": "/api/v1/auth/register",
  "timestamp": "2026-10-03T15:00:00Z"
}
```

---

## Ejecutar en local

### Requisitos
- JDK 21
- PostgreSQL 17 con las bases `panelvault_dev` y `panelvault_test` (dueño: rol `panelvault`)
- Opcional: el motor de IA corriendo en `http://localhost:8001` para analizar páginas

### Variables de entorno

Los secretos **nunca** se escriben en archivos del repositorio. Ver `.env.example`.

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PANELVAULT_DB_PASSWORD` | Sí | Contraseña del rol `panelvault` |
| `PANELVAULT_JWT_SECRET` | Sí | Clave HMAC de los JWT, Base64, mínimo 32 bytes |
| `PANELVAULT_TOTP_ENCRYPTION_KEY` | Sí | Clave AES-256 de la 2FA, Base64, exactamente 32 bytes |
| `PANELVAULT_ENGINE_SECRET` | Sí | Secreto HMAC compartido con el motor de IA (mínimo 32 caracteres) |
| `PANELVAULT_ENGINE_URL` | No | URL del motor (por defecto `http://localhost:8001`) |
| `PANELVAULT_GATEWAY_ENABLED` | No | `true` en producción |
| `PANELVAULT_GATEWAY_SECRET` | Si el gateway está activo | Secreto HMAC compartido con el frontend |
| `PANELVAULT_CORS_ORIGINS` | No | Orígenes permitidos (por defecto `http://localhost:3000`) |
| `PANELVAULT_DB_URL`, `PANELVAULT_DB_USER`, `PORT` | No | Por defecto: base local, rol `panelvault`, puerto `9096` |

### Comandos

```bash
./mvnw test            # todas las pruebas (usan la base panelvault_test)
./mvnw spring-boot:run # arranca en http://localhost:9096
```

En Windows: `.\mvnw test` y `.\mvnw spring-boot:run`.

### Docker

```bash
docker build -t panelvault-backend .
docker run -p 9096:9096 --env-file .env panelvault-backend
```

---

## Pruebas

Las pruebas cubren cada capa:
- **Dominio y casos de uso** con repositorios en memoria (rápidas, sin base de datos).
- **Adaptadores JPA** contra PostgreSQL real, incluidas la consulta `SKIP LOCKED` y las restricciones.
- **De punta a punta** con MockMvc, Spring Security real y JWT firmados de verdad.
- **Compatibilidad con el motor de IA**: las firmas HMAC se comparan con las que calcula el motor en
  Python, y el cliente HTTP se prueba contra un servidor HTTP real.
