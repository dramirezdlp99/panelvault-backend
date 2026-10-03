package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.LoginService;
import com.panelvault.backend.identity.application.RegisterUserCommand;
import com.panelvault.backend.identity.application.RegisterUserService;
import com.panelvault.backend.identity.application.TokenRefreshService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion: registro, login, refresh y logout.
 *
 * <p>El controlador es delgado a proposito: traduce HTTP a casos de uso y el resultado a JSON. Los
 * errores no se manejan aqui sino en el manejador global.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserService registerUser;
    private final LoginService login;
    private final TokenRefreshService tokenRefresh;

    public AuthController(RegisterUserService registerUser, LoginService login, TokenRefreshService tokenRefresh) {
        this.registerUser = registerUser;
        this.login = login;
        this.tokenRefresh = tokenRefresh;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command =
                new RegisterUserCommand(request.email(), request.displayName(), request.password());
        return UserResponse.from(registerUser.register(command));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(login.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return TokenResponse.from(tokenRefresh.refresh(request.refreshToken()));
    }

    /** Siempre 204, exista o no la sesion: no se revela nada sobre el token recibido. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        tokenRefresh.logout(request.refreshToken());
    }
}