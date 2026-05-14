package com.silva.projeto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silva.projeto.model.entity.Venda;

public interface VendaRepository extends JpaRepository<Venda, Long> {

}