package com.silva.projeto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silva.projeto.model.entity.Funcionario;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

}