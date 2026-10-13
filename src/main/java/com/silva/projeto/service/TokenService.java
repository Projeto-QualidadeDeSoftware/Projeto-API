package com.silva.projeto.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.silva.projeto.model.entity.Funcionario;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration validade;

    public TokenService(
            JwtEncoder encoder,
            @Value("${app.jwt.expiracao-minutos}") long expiracaoMinutos) {

        this.encoder = encoder;
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    public Instant calcularExpiracao(Instant agora) {
        return agora.plus(validade);
    }

    /** O "subject" do token é o id do funcionário (não expõe CPF no token). */
    public String gerar(Funcionario funcionario, Instant agora) {

        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet dados = JwtClaimsSet.builder()
                .subject(String.valueOf(funcionario.getId()))
                .issuedAt(agora)
                .expiresAt(calcularExpiracao(agora))
                .build();

        return encoder
                .encode(JwtEncoderParameters.from(cabecalho, dados))
                .getTokenValue();
    }
}
