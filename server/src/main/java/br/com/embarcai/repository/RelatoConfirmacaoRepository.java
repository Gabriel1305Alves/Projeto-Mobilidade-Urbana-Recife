package br.com.embarcai.repository;

import br.com.embarcai.model.RelatoConfirmacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelatoConfirmacaoRepository extends JpaRepository<RelatoConfirmacao, Long> {
    boolean existsByRelatoIdAndUsuarioId(Long relatoId, Long usuarioId);
    long countByRelatoId(Long relatoId);
}
