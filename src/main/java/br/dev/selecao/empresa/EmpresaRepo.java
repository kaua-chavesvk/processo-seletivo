package br.dev.selecao.empresa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepo extends JpaRepository<Empresa, UUID> {}
