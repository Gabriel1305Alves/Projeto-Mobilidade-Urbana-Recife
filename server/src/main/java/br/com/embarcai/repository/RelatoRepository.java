package br.com.embarcai.repository;

import br.com.embarcai.model.Linha;
import br.com.embarcai.model.Relato;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelatoRepository extends JpaRepository<Relato, Long> {
    List<Relato> findTop40ByLinhaAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
        Linha linha,
        OffsetDateTime limite
    );

    @Query("SELECT r FROM Relato r JOIN FETCH r.linha WHERE r.createdAt >= :limite ORDER BY r.createdAt DESC")
    List<Relato> findRecentesComLinha(@Param("limite") OffsetDateTime limite);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime limite);
    long countByConfirmacoesGreaterThanEqual(int minimo);

    @Query(
        value = "SELECT COUNT(DISTINCT COALESCE(autor, 'anon')) FROM relatos WHERE created_at >= :inicio",
        nativeQuery = true
    )
    long contarAutoresDesde(@Param("inicio") OffsetDateTime inicio);

    boolean existsByLinhaAndMensagemContainingIgnoreCase(Linha linha, String trecho);
}
