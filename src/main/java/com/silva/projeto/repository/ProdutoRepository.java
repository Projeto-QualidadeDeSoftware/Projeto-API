package com.silva.projeto.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.silva.projeto.model.entity.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

}