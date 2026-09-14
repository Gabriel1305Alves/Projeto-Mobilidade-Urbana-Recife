package br.com.embarcai.repository;

import br.com.embarcai.model.Linha;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinhaRepository extends JpaRepository<Linha, Long> {
    Optional<Linha> findByCodigo(String codigo);
    List<Linha> findAllByOrderByCodigoAsc();
    boolean existsByCodigo(String codigo);
}
