package br.com.embarcai.repository;

import br.com.embarcai.model.SolicitacaoLinha;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitacaoLinhaRepository extends JpaRepository<SolicitacaoLinha, Long> {
    boolean existsByCodigoIgnoreCaseAndStatus(String codigo, String status);

    boolean existsByUsuarioIdAndCodigoIgnoreCaseAndStatus(Long usuarioId, String codigo, String status);

    @Query("SELECT s FROM SolicitacaoLinha s JOIN FETCH s.usuario WHERE s.status = :status ORDER BY s.createdAt ASC")
    List<SolicitacaoLinha> findByStatusComUsuario(@Param("status") String status);
}
