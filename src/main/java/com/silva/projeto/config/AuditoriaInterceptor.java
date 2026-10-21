package com.silva.projeto.config;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.silva.projeto.model.entity.RegistroAuditoria;
import com.silva.projeto.service.AuditoriaService;

/**
 * Passa por todas as requisições da API:
 * - grava uma linha no log de acesso (método, rota, status, tempo, usuário e IP);
 * - nas operações que alteram dados (POST, PUT, PATCH e DELETE) grava também um
 *   registro na tabela de auditoria.
 */
@Component
public class AuditoriaInterceptor implements HandlerInterceptor {

    /** Preenchido pelo login, que acontece antes de existir um usuário autenticado. */
    public static final String ATRIBUTO_USUARIO = "auditoria.usuario";
    public static final String ATRIBUTO_FUNCIONARIO = "auditoria.funcionarioId";

    private static final String ATRIBUTO_INICIO = "auditoria.inicio";
    private static final Set<String> METODOS_QUE_ALTERAM = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final int TAMANHO_MAXIMO_RECURSO = 255;

    private static final Logger acesso = LoggerFactory.getLogger("acesso");

    private final AuditoriaService auditoriaService;

    public AuditoriaInterceptor(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATRIBUTO_INICIO, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception erro) {

        String metodo = request.getMethod();
        String recurso = limitar(request.getRequestURI());
        int status = response.getStatus();
        long duracaoMs = duracaoEmMs(request);
        String ip = request.getRemoteAddr();
        Long funcionarioId = funcionarioDaRequisicao(request);

        acesso.info("{} {} {} {}ms funcionario={} ip={}",
                metodo, recurso, status, duracaoMs,
                funcionarioId == null ? "-" : funcionarioId, ip);

        if (!METODOS_QUE_ALTERAM.contains(metodo)) {
            return;
        }

        String usuario = funcionarioId != null
                ? auditoriaService.nomeDoFuncionario(funcionarioId)
                : (String) request.getAttribute(ATRIBUTO_USUARIO);

        auditoriaService.registrar(new RegistroAuditoria(
                LocalDateTime.now(),
                funcionarioId,
                usuario,
                acao(metodo, recurso, status),
                metodo,
                recurso,
                status,
                ip,
                duracaoMs));
    }

    static String acao(String metodo, String recurso, int status) {
        if (recurso.startsWith("/auth/login")) {
            return status < 400 ? "LOGIN" : "LOGIN_RECUSADO";
        }
        if (recurso.startsWith("/assistente")) {
            return "PERGUNTA_IA";
        }
        return switch (metodo) {
            case "POST" -> "INCLUSAO";
            case "DELETE" -> "EXCLUSAO";
            default -> "ALTERACAO";
        };
    }

    private static Long funcionarioDaRequisicao(HttpServletRequest request) {
        Object doLogin = request.getAttribute(ATRIBUTO_FUNCIONARIO);
        if (doLogin instanceof Long id) {
            return id;
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null
                || !autenticacao.isAuthenticated()
                || autenticacao instanceof AnonymousAuthenticationToken) {
            return null;
        }

        try {
            return Long.valueOf(autenticacao.getName());
        } catch (NumberFormatException erro) {
            return null;
        }
    }

    private static long duracaoEmMs(HttpServletRequest request) {
        Object inicio = request.getAttribute(ATRIBUTO_INICIO);
        if (inicio instanceof Long nanos) {
            return (System.nanoTime() - nanos) / 1_000_000;
        }
        return 0;
    }

    private static String limitar(String recurso) {
        return recurso.length() > TAMANHO_MAXIMO_RECURSO
                ? recurso.substring(0, TAMANHO_MAXIMO_RECURSO)
                : recurso;
    }
}
