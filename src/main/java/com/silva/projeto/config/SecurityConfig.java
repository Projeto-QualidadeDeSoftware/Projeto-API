package com.silva.projeto.config;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Regras de acesso da API: tudo exige um token JWT válido, exceto o login,
 * a verificação de saúde usada pelo deploy, as métricas lidas pelo Prometheus
 * (bloqueadas para fora do servidor pelo Caddy) e a documentação da API.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${app.cors.origens-permitidas}") String origensPermitidas) throws Exception {

        AuthenticationEntryPoint naoAutenticado = naoAutenticado();

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(origensAutorizadas(origensPermitidas)))
                .sessionManagement(sessao -> sessao
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(naoAutenticado))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(naoAutenticado));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(@Value("${app.jwt.secret}") String segredo) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave(segredo)));
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String segredo) {
        return NimbusJwtDecoder
                .withSecretKey(chave(segredo))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Só os endereços configurados (front web) podem chamar a API a partir do navegador. */
    private static CorsConfigurationSource origensAutorizadas(String origens) {

        List<String> lista = Arrays.stream(origens.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();

        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(lista);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", cors);
        return fonte;
    }

    private static SecretKey chave(String segredo) {

        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (variável JWT_SECRET) precisa ter pelo menos 32 caracteres");
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /** Resposta 401 no mesmo formato JSON dos demais erros da API. */
    private static AuthenticationEntryPoint naoAutenticado() {

        return (request, response, erro) -> {
            response.setStatus(401);
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":401,"
                    + "\"mensagem\":\"Não autenticado: faça login e envie o token no cabeçalho Authorization\","
                    + "\"dataHora\":\"" + LocalDateTime.now() + "\"}");
        };
    }
}
