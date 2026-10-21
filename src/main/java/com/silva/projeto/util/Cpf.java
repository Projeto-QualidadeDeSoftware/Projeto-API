package com.silva.projeto.util;

import com.silva.projeto.exception.DadoInvalidoException;

/**
 * O CPF é sempre guardado e comparado só com números, para que "111.111.111-11"
 * e "11111111111" sejam tratados como o mesmo CPF, venha ele do desktop, do mobile ou da web.
 */
public final class Cpf {

    private Cpf() {
    }

    /** Remove pontos, traço e espaços. Usado no login, que não deve revelar se o CPF é válido. */
    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    /** Normaliza e exige 11 dígitos. Usado nos cadastros. */
    public static String validar(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != 11) {
            throw new DadoInvalidoException("CPF deve ter 11 dígitos");
        }
        return digitos;
    }

    /** Esconde os dígitos do meio (123.***.***-01), para registrar em log e auditoria sem expor o CPF. */
    public static String mascarar(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != 11) {
            return "***";
        }
        return digitos.substring(0, 3) + ".***.***-" + digitos.substring(9);
    }
}
