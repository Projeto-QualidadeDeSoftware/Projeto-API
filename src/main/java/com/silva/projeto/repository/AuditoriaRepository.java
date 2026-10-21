package com.silva.projeto.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.silva.projeto.model.entity.RegistroAuditoria;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    Page<RegistroAuditoria> findAllByOrderByIdDesc(Pageable pagina);
}
