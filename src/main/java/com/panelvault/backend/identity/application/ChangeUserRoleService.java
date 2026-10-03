package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.Role;
import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.BusinessRuleException;
import com.panelvault.backend.shared.error.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: un ADMIN cambia el rol de otro usuario.
 *
 * <p>Que solo un ADMIN llegue aqui lo garantiza la configuracion de seguridad (las rutas
 * {@code /api/v1/admin/**}). Esta clase agrega la regla de negocio: un administrador no puede
 * cambiar su propio rol, asi nunca se queda el sistema sin administradores por accidente.
 *
 * <p>El nuevo rol aplica en el siguiente access token, es decir, en maximo 15 minutos.
 */
@Service
public class ChangeUserRoleService {

    private final UserRepository users;
    private final Clock clock;

    public ChangeUserRoleService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public User changeRole(UserId actorId, UserId targetId, Role newRole) {
        if (actorId.equals(targetId)) {
            throw new BusinessRuleException(
                    "user.cannot_change_own_role", "Un administrador no puede cambiar su propio rol");
        }
        User target = users.findById(targetId)
                .orElseThrow(() -> new NotFoundException("user.not_found", "No existe el usuario"));
        target.changeRole(newRole, clock.instant());
        return users.save(target);
    }
}