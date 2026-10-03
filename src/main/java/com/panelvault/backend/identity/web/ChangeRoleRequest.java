package com.panelvault.backend.identity.web;

import com.panelvault.backend.identity.domain.Role;
import jakarta.validation.constraints.NotNull;

/** Cuerpo JSON de {@code PATCH /api/v1/admin/users/{id}/role}. Un rol inexistente da 400. */
public record ChangeRoleRequest(@NotNull Role role) {}