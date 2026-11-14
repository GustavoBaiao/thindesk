package com.pfc.thindesk.controller;

import com.pfc.thindesk.entity.enums.StatusChamado;
import com.pfc.thindesk.service.DashboardService;
import com.pfc.thindesk.service.DashboardService.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/indicadores")
    public Indicadores indicadores(
            @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "status", required = false) StatusChamado status) {
        return dashboard.indicadores(inicio, fim, tipo, status);
    }

    @GetMapping("/atendidos")
    public List<AtendimentoPeriodo> atendidos(
            @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "agrupamento", defaultValue = "DIA") Agrupamento agrupamento) {
        return dashboard.atendidos(inicio, fim, tipo, agrupamento);
    }

    @GetMapping("/tipos-mais-atendidos")
    public List<TipoAtendido> tiposMaisAtendidos(
            @RequestParam(name = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(name = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(name = "tipo", required = false) String tipo) {
        return dashboard.tiposMaisAtendidos(inicio, fim, tipo);
    }

    @GetMapping("/tipos")
    public List<String> tipos() {
        return dashboard.tipos();
    }
}
