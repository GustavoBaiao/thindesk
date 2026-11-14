package com.pfc.thindesk.service;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.entity.enums.StatusChamado;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.Pattern;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {
    public enum Agrupamento {DIA, SEMANA, MES}

    public record Indicadores(long total, Map<StatusChamado, Long> porStatus) {
    }

    public record AtendimentoPeriodo(LocalDate periodo, long quantidade) {
    }

    public record TipoAtendido(String tipo, long quantidade) {
    }

    private final MongoOperations mongo;

    public DashboardService(MongoOperations mongo) {
        this.mongo = mongo;
    }

    public Indicadores indicadores(LocalDate inicio, LocalDate fim, String tipo, StatusChamado status) {
        Query query = filtro(inicio, fim, tipo, "dataAbertura");
        if (status != null) query.addCriteria(Criteria.where("status").is(status));
        query.fields().include("status");
        Map<StatusChamado, Long> contagens = new EnumMap<>(StatusChamado.class);
        for (StatusChamado valor : StatusChamado.values()) contagens.put(valor, 0L);
        List<Chamado> chamados = mongo.find(query, Chamado.class);
        for (Chamado chamado : chamados) {
            if (chamado.getStatus() != null) contagens.merge(chamado.getStatus(), 1L, Long::sum);
        }
        return new Indicadores(chamados.size(), contagens);
    }

    public List<AtendimentoPeriodo> atendidos(LocalDate inicio, LocalDate fim, String tipo,
                                              Agrupamento agrupamento) {
        Query query = concluidos(inicio, fim, tipo);
        query.fields().include("dataConclusao");
        Map<LocalDate, Long> contagens = new TreeMap<>();
        for (Chamado chamado : mongo.find(query, Chamado.class)) {
            LocalDate data = chamado.getDataConclusao().toLocalDate();
            LocalDate periodo = switch (agrupamento) {
                case DIA -> data;
                case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                case MES -> data.withDayOfMonth(1);
            };
            contagens.merge(periodo, 1L, Long::sum);
        }
        return contagens.entrySet().stream()
                .map(e -> new AtendimentoPeriodo(e.getKey(), e.getValue())).toList();
    }

    public List<TipoAtendido> tiposMaisAtendidos(LocalDate inicio, LocalDate fim, String tipo) {
        Query query = concluidos(inicio, fim, tipo);
        query.fields().include("tipo");
        Map<String, Long> contagens = new HashMap<>();
        for (Chamado chamado : mongo.find(query, Chamado.class)) {
            String nome = chamado.getTipo();
            nome = nome == null || nome.isBlank() ? "Sem tipo" : nome.trim();
            contagens.merge(nome, 1L, Long::sum);
        }
        return contagens.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5).map(e -> new TipoAtendido(e.getKey(), e.getValue())).toList();
    }

    public List<String> tipos() {
        return mongo.findDistinct(new Query(), "tipo", Chamado.class, String.class).stream()
                .filter(Objects::nonNull).map(String::trim).filter(t -> !t.isEmpty())
                .distinct().sorted().toList();
    }

    private Query concluidos(LocalDate inicio, LocalDate fim, String tipo) {
        Query query = filtro(inicio, fim, tipo, "dataConclusao");
        query.addCriteria(Criteria.where("status").is(StatusChamado.CONCLUIDO));
        // Documentos antigos sem conclusão não podem entrar nos gráficos nem no ranking.
        query.addCriteria(new Criteria().andOperator(Criteria.where("dataConclusao").ne(null)));
        return query;
    }

    private Query filtro(LocalDate inicio, LocalDate fim, String tipo, String campoData) {
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dataInicio deve ser anterior ou igual a dataFim.");
        }
        if (fim != null && fim.equals(LocalDate.MAX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataFim fora do intervalo suportado.");
        }
        Query query = new Query();
        if (inicio != null || fim != null) {
            Criteria periodo = Criteria.where(campoData);
            if (inicio != null) periodo.gte(inicio.atStartOfDay());
            if (fim != null) periodo.lt(fim.plusDays(1).atStartOfDay());
            query.addCriteria(periodo);
        }
        if (tipo != null) {
            if (tipo.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipo não pode ser vazio.");
            query.addCriteria(Criteria.where("tipo").regex("^\\s*" + Pattern.quote(tipo.trim()) + "\\s*$"));
        }
        return query;
    }
}
