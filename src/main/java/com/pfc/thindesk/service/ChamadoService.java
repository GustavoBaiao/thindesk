package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.enums.StatusChamado;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import com.pfc.thindesk.repository.ChamadoRepository;

@Service
public class ChamadoService {

    @Autowired
    private ChamadoRepository chamadoRepository;

    public Chamado criarChamado(Chamado chamado) {
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDataAbertura(LocalDateTime.now());
        chamado.setDataConclusao(null);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll();
    }

    public Chamado atualizarChamado(String id, Chamado chamadoAtualizado) {
        validarStatus(chamadoAtualizado.getStatus());
        return chamadoRepository.findById(id)
                .map(chamado -> {
                    chamado.setDescricao(chamadoAtualizado.getDescricao());
                    atualizarDataConclusao(chamado, chamadoAtualizado.getStatus());
                    chamado.setStatus(chamadoAtualizado.getStatus());
                    chamado.setTipo(chamadoAtualizado.getTipo());
                    chamado.setTecnico(chamadoAtualizado.getTecnico());
                    chamado.setUsuario(chamadoAtualizado.getUsuario());
                    return chamadoRepository.save(chamado);
                })
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado com id: " + id));
    }

    private void validarStatus(StatusChamado status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O status do chamado deve ser informado.");
        }
    }

    private void atualizarDataConclusao(Chamado chamado, StatusChamado novoStatus) {
        if (novoStatus != StatusChamado.CONCLUIDO) {
            chamado.setDataConclusao(null);
        } else if (chamado.getStatus() != StatusChamado.CONCLUIDO || chamado.getDataConclusao() == null) {
            chamado.setDataConclusao(LocalDateTime.now());
        }
    }

}
