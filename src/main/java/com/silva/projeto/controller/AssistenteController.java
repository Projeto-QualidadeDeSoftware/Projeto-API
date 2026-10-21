package com.silva.projeto.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.silva.projeto.model.dto.PerguntaRequest;
import com.silva.projeto.model.dto.RespostaAssistenteDTO;
import com.silva.projeto.service.AssistenteService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/assistente")
@Tag(name = "Assistente de IA")
public class AssistenteController {

    private final AssistenteService service;

    public AssistenteController(AssistenteService service) {
        this.service = service;
    }

    @PostMapping("/pergunta")
    public RespostaAssistenteDTO perguntar(@RequestBody @Valid PerguntaRequest dados) {
        return new RespostaAssistenteDTO(service.responder(dados.pergunta()));
    }
}
