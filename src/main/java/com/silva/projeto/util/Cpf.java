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

    /** Normaliza, exige 11 dígitos e confere os dígitos verificadores. Usado nos cadastros. */
    public static String validar(String cpf) {
        String digitos = normalizar(cpf);
        if (digitos == null || digitos.length() != 11) {
            throw new DadoInvalidoException("CPF deve ter 11 dígitos");
        }
        if (!digitosVerificadoresConferem(digitos)) {
            throw new DadoInvalidoException("CPF inválido");
        }
        return digitos;
    }

    /** CPFs com todos os dígitos iguais (111.111.111-11) passam na conta, mas não existem. */
    private static boolean digitosVerificadoresConferem(String digitos) {
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    private static int digitoVerificador(String digitos, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) {
            soma += (digitos.charAt(i) - '0') * (posicao + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
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
