package com.silva.projeto.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditoriaInterceptorTest {

    @Test
    void classificaAAcaoPeloMetodoEPelaRota() {
        assertThat(AuditoriaInterceptor.acao("POST", "/produto", 200)).isEqualTo("INCLUSAO");
        assertThat(AuditoriaInterceptor.acao("PUT", "/produto/1", 200)).isEqualTo("ALTERACAO");
        assertThat(AuditoriaInterceptor.acao("DELETE", "/produto/1", 200)).isEqualTo("EXCLUSAO");
        assertThat(AuditoriaInterceptor.acao("POST", "/auth/login", 200)).isEqualTo("LOGIN");
        assertThat(AuditoriaInterceptor.acao("POST", "/auth/login", 401)).isEqualTo("LOGIN_RECUSADO");
        assertThat(AuditoriaInterceptor.acao("POST", "/assistente/pergunta", 200)).isEqualTo("PERGUNTA_IA");
    }
}
