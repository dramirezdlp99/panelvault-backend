package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.application.RegisterUserCommand;
import com.panelvault.backend.identity.application.RegisterUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacion. Por ahora solo el registro; login, refresh y logout llegan en el
 * siguiente bloque junto con los JWT.
 *
 * <p>El controlador es delgado a proposito: traduce HTTP a un comando y el resultado a JSON. Los
 * errores no se manejan aqui sino en el manejador global.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserService registerUser;

    public AuthController(RegisterUserService registerUser) {
        this.registerUser = registerUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command =
                new RegisterUserCommand(request.email(), request.displayName(), request.password());
        return UserResponse.from(registerUser.register(command));
    }
}