package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.ChangeUserRoleService;
import com.panelvault.backend.identity.domain.UserId;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administracion de usuarios. Todo {@code /api/v1/admin/**} exige rol ADMIN; la regla esta en
 * {@code SecurityConfiguration}, en el backend, y no depende de lo que muestre el frontend.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final ChangeUserRoleService changeRole;

    public AdminUserController(ChangeUserRoleService changeRole) {
        this.changeRole = changeRole;
    }

    @PatchMapping("/{userId}/role")
    public UserResponse changeRole(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        return UserResponse.from(changeRole.changeRole(CurrentUser.idOf(jwt), new UserId(userId), request.role()));
    }
}