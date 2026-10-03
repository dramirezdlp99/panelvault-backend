package com.panelvault.backend.identity.application;

import com.panelvault.backend.identity.domain.User;
import com.panelvault.backend.identity.domain.UserId;
import com.panelvault.backend.identity.domain.UserRepository;
import com.panelvault.backend.shared.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: consultar los datos de un usuario (por ejemplo, el que inicio sesion). */
@Service
public class UserProfileService {

    private final UserRepository users;

    public UserProfileService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User get(UserId id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("user.not_found", "No existe el usuario"));
    }
}