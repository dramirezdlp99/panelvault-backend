package com.panelvault.backend.shared.error;

/**
 * Los datos tienen el formato correcto pero violan una regla de negocio, por ejemplo marcar como
 * leida una pagina mayor al numero de paginas del tomo.
 */
public class BusinessRuleException extends DomainException {

    public BusinessRuleException(String code, String message) {
        super(ErrorCategory.BUSINESS_RULE, code, message);
    }
}