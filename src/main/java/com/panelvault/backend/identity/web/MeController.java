package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code GET /api/v1/me}: datos del usuario que hace la peticion.
 *
 * <p>No recibe ningun id por parametro: el id sale del token firmado. Asi es imposible pedir los
 * datos de otra persona cambiando un numero en la URL.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserProfileService profiles;

    public MeController(UserProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(profiles.get(CurrentUser.idOf(jwt)));
    }
}