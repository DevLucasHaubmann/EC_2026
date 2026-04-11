package com.tukan.api.service;

import com.tukan.api.dto.ai.AiPerfilContext;
import com.tukan.api.dto.ai.AiRecommendationContext;
import com.tukan.api.dto.ai.AiTriagemContext;
import com.tukan.api.dto.ai.AiUsuarioContext;
import com.tukan.api.entity.Perfil;
import com.tukan.api.entity.Triagem;
import com.tukan.api.entity.User;
import com.tukan.api.exception.IncompleteProfileException;
import com.tukan.api.repository.PerfilRepository;
import com.tukan.api.repository.TriagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiContextService {

    private final UserService userService;
    private final PerfilRepository perfilRepository;
    private final TriagemRepository triagemRepository;

    @Transactional(readOnly = true)
    public AiRecommendationContext build(Integer usuarioId) {
        User user = userService.findById(usuarioId);

        Perfil perfil = perfilRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "perfil",
                        "Perfil nutricional não encontrado. Complete seu perfil antes de solicitar recomendações."));

        Triagem triagem = triagemRepository.findByUsuarioId(user.getId())
                .orElseThrow(() -> new IncompleteProfileException(
                        "triagem",
                        "Triagem não encontrada. Complete sua triagem antes de solicitar recomendações."));

        return new AiRecommendationContext(
                buildUsuarioContext(user),
                buildPerfilContext(perfil),
                buildTriagemContext(triagem)
        );
    }

    private AiUsuarioContext buildUsuarioContext(User user) {
        return new AiUsuarioContext(user.getId(), user.getNome());
    }

    private AiPerfilContext buildPerfilContext(Perfil perfil) {
        return new AiPerfilContext(
                perfil.getSexo().name(),
                perfil.calcularIdade(LocalDate.now()),
                perfil.getPesoKg(),
                perfil.getAlturaCm(),
                perfil.getNivelAtividade().name()
        );
    }

    private AiTriagemContext buildTriagemContext(Triagem triagem) {
        return new AiTriagemContext(
                triagem.getObjetivo().name(),
                normalizeList(triagem.getRestricoesAlimentares()),
                normalizeList(triagem.getAlergias()),
                normalizeList(triagem.getCondicoesSaude())
        );
    }

    private List<String> normalizeList(String valor) {
        if (valor == null || valor.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(valor.split("[,;]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}
