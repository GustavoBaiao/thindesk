package com.pfc.thindesk.service;

import com.pfc.thindesk.controller.DashboardController;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.enums.StatusChamado;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class DashboardServiceTest {
    private List<Chamado> dados;
    private Query consulta;
    private DashboardService service;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        dados = List.of();
        MongoOperations mongo = (MongoOperations) Proxy.newProxyInstance(
                MongoOperations.class.getClassLoader(), new Class<?>[]{MongoOperations.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("find")) {
                        consulta = (Query) args[0];
                        return dados;
                    }
                    if (metodo.getName().equals("findDistinct")) {
                        return List.of(" Rede ", "Hardware", "Rede", " ");
                    }
                    throw new UnsupportedOperationException(metodo.getName());
                });
        service = new DashboardService(mongo);
        mvc = MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();
    }

    @Test
    void indicadoresVaziosIncluemTodosOsStatus() {
        var resultado = service.indicadores(null, null, null, null);
        assertEquals(0, resultado.total());
        assertEquals(4, resultado.porStatus().size());
        assertTrue(resultado.porStatus().values().stream().allMatch(v -> v == 0));
    }

    @Test
    void contaStatusEUsaAberturaComFimExclusivoDoDiaSeguinte() {
        dados = List.of(chamado(StatusChamado.ABERTO, "Rede", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "Rede", "2026-11-02T09:00:00"));
        var resultado = service.indicadores(LocalDate.parse("2026-11-01"),
                LocalDate.parse("2026-11-14"), "R.de", StatusChamado.ABERTO);
        assertEquals(2, resultado.total());
        assertEquals(1L, resultado.porStatus().get(StatusChamado.CONCLUIDO));
        var filtro = consulta.getQueryObject();
        var periodo = (org.bson.Document) filtro.get("dataAbertura");
        assertEquals(LocalDateTime.parse("2026-11-01T00:00:00"), periodo.get("$gte"));
        assertEquals(LocalDateTime.parse("2026-11-15T00:00:00"), periodo.get("$lt"));
        assertEquals(StatusChamado.ABERTO, filtro.get("status"));
        assertTrue(filtro.get("tipo").toString().contains("\\QR.de\\E"));
    }

    @Test
    void agrupaDiasSemanasEMesesEmOrdemCronologica() {
        dados = List.of(chamado(StatusChamado.CONCLUIDO, "Rede", "2027-01-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "Rede", "2026-12-31T23:59:59"));
        var dias = service.atendidos(null, null, null, DashboardService.Agrupamento.DIA);
        assertEquals(LocalDate.parse("2026-12-31"), dias.getFirst().periodo());
        var semanas = service.atendidos(null, null, null, DashboardService.Agrupamento.SEMANA);
        assertEquals(1, semanas.size());
        assertEquals(LocalDate.parse("2026-12-28"), semanas.getFirst().periodo());
        assertEquals(2, semanas.getFirst().quantidade());
        assertEquals(2, service.atendidos(null, null, null, DashboardService.Agrupamento.MES).size());
    }

    @Test
    void concluidoFiltraPorConclusaoEExcluiDataNulaSemColidirComPeriodo() {
        service.atendidos(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-14"),
                null, DashboardService.Agrupamento.DIA);
        var filtro = consulta.getQueryObject();
        assertEquals(StatusChamado.CONCLUIDO, filtro.get("status"));
        assertTrue(filtro.containsKey("dataConclusao"));
        assertFalse(filtro.containsKey("dataAbertura"));
        assertTrue(filtro.containsKey("$and"));
    }

    @Test
    void rankingLimitaCincoDesempataPorNomeENormalizaTipo() {
        dados = List.of(chamado(StatusChamado.CONCLUIDO, " Rede ", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "Rede", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "B", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "A", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "C", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "D", "2026-11-01T09:00:00"),
                chamado(StatusChamado.CONCLUIDO, "E", "2026-11-01T09:00:00"));
        var ranking = service.tiposMaisAtendidos(null, null, null);
        assertEquals(5, ranking.size());
        assertEquals("Rede", ranking.getFirst().tipo());
        assertEquals(2, ranking.getFirst().quantidade());
        assertEquals("A", ranking.get(1).tipo());
    }

    @Test
    void listasVaziasETiposOrdenados() {
        assertTrue(service.atendidos(null, null, null, DashboardService.Agrupamento.DIA).isEmpty());
        assertTrue(service.tiposMaisAtendidos(null, null, null).isEmpty());
        assertEquals(List.of("Hardware", "Rede"), service.tipos());
    }

    @Test
    void periodoInvertidoRejeitado() {
        assertThrows(ResponseStatusException.class, () -> service.indicadores(
                LocalDate.parse("2026-11-14"), LocalDate.parse("2026-11-01"), null, null));
    }

    @Test
    void endpointsResolvemParametrosSemFlagParametersERetornamJson() throws Exception {
        for (String caminho : List.of("indicadores", "atendidos", "tipos-mais-atendidos", "tipos")) {
            var resposta = mvc.perform(MockMvcRequestBuilders.get("/api/dashboard/" + caminho))
                    .andReturn().getResponse();
            assertEquals(200, resposta.getStatus(), caminho);
            assertTrue(resposta.getContentType().contains("application/json"));
        }
        assertTrue(mvc.perform(MockMvcRequestBuilders.get("/api/dashboard/indicadores"))
                .andReturn().getResponse().getContentAsString().contains("\"total\":0"));
    }

    @Test
    void filtrosInvalidosRetornam400() throws Exception {
        for (String consulta : List.of("dataInicio=abc", "status=INVALIDO", "tipo=",
                "dataInicio=2026-11-14&dataFim=2026-11-01")) {
            assertEquals(400, mvc.perform(MockMvcRequestBuilders.get("/api/dashboard/indicadores?" + consulta))
                    .andReturn().getResponse().getStatus(), consulta);
        }
        assertEquals(400, mvc.perform(MockMvcRequestBuilders.get("/api/dashboard/atendidos?agrupamento=ANO"))
                .andReturn().getResponse().getStatus());
    }

    private Chamado chamado(StatusChamado status, String tipo, String conclusao) {
        Chamado chamado = new Chamado();
        chamado.setStatus(status);
        chamado.setTipo(tipo);
        chamado.setDataConclusao(LocalDateTime.parse(conclusao));
        return chamado;
    }
}
