(() => {
    'use strict';
    const root = document.getElementById('dashboard');
    if (!root) return;
    const base = root.dataset.apiBase;
    const byId = id => document.getElementById(id);
    const form = byId('dashboard-filters');
    const grouping = byId('agrupamento');
    const number = new Intl.NumberFormat('pt-BR');
    const statuses = [
        ['ABERTO', 'Abertos', 'open'], ['EM_ANDAMENTO', 'Em andamento', 'progress-status'],
        ['CONCLUIDO', 'Concluídos', 'done'], ['CANCELADO', 'Cancelados', 'canceled']
    ];
    let busy = false;
    let typesLoaded = false;

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }

    function message(id, text, error = false) {
        const node = byId(id);
        node.textContent = text;
        node.hidden = !text;
        node.classList.toggle('error', error);
    }

    async function request(path, params = new URLSearchParams()) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 15000);
        try {
            const response = await fetch(`${base}/${path}?${params}`, {
                credentials: 'same-origin', headers: {Accept: 'application/json'}, signal: controller.signal
            });
            if (response.redirected || response.status === 401 || response.status === 403) {
                throw new Error('Sua sessão expirou. Faça login novamente e atualize o painel.');
            }
            if (!response.ok) {
                throw new Error(response.status === 400
                    ? 'Não foi possível consultar: confira o período e os filtros.'
                    : 'Não foi possível carregar os dados. Tente Atualizar novamente.');
            }
            if (!(response.headers.get('Content-Type') || '').includes('application/json')) {
                throw new Error('A resposta não contém dados do painel. Confira sua sessão de login.');
            }
            return await response.json();
        } catch (error) {
            if (error.name === 'AbortError') throw new Error('A consulta demorou mais que o esperado. Tente Atualizar novamente.');
            if (error instanceof TypeError) throw new Error('Falha de conexão com o servidor. Tente Atualizar novamente.');
            throw error;
        } finally {
            clearTimeout(timeout);
        }
    }

    function assertCount(value) {
        if (!Number.isSafeInteger(value) || value < 0) throw new Error('O servidor retornou contagens inválidas. Tente atualizar.');
    }

    function renderOverview(data) {
        assertCount(data.total);
        statuses.forEach(([key]) => assertCount(data.porStatus[key]));
        byId('metric-total').textContent = number.format(data.total);
        statuses.forEach(([key]) => {
            byId(`metric-${key}`).textContent = number.format(data.porStatus[key]);
        });
        message('overview-message', data.total === 0 ? 'Nenhum chamado encontrado com estes filtros.' : '');
        const meter = byId('status-meter');
        const legend = byId('status-legend');
        meter.replaceChildren();
        legend.replaceChildren();
        byId('situation-chart').hidden = data.total === 0;
        message('situation-message', data.total === 0 ? 'Nenhum chamado encontrado com estes filtros.' : '');
        statuses.forEach(([key, label, color]) => {
            const count = data.porStatus[key];
            const percentage = data.total ? count / data.total * 100 : 0;
            const segment = element('span', color);
            segment.style.width = `${percentage}%`;
            meter.append(segment);
            const row = element('li');
            row.append(element('span', `status-dot ${color}`), element('span', '', label),
                element('strong', '', number.format(count)),
                element('small', '', `${percentage.toLocaleString('pt-BR', {maximumFractionDigits: 1})}%`));
            legend.append(row);
        });
    }

    function formatPeriod(period, mode) {
        const [year, month, day] = period.split('-');
        if (mode === 'MES') return `${month}/${year}`;
        return `${day}/${month}/${year}`;
    }

    function renderAttendance(data, mode) {
        if (!Array.isArray(data)) throw new Error('Formato de atendimentos inválido.');
        data.forEach(item => {
            assertCount(item.quantidade);
            if (!/^\d{4}-\d{2}-\d{2}$/.test(item.periodo)) throw new Error('Período inválido na resposta.');
        });
        const chart = byId('attendance-chart');
        chart.replaceChildren();
        chart.hidden = data.length === 0;
        message('attendance-message', data.length === 0 ? 'Nenhum atendimento concluído neste recorte.' : '');
        const notes = {
            DIA: 'Cada coluna representa um dia com atendimentos.',
            SEMANA: 'As semanas começam na segunda-feira. A data indica o início da semana.',
            MES: 'Cada coluna representa um mês com atendimentos.'
        };
        byId('attendance-note').textContent = notes[mode];
        if (!data.length) return;
        const maximum = Math.max(...data.map(item => item.quantidade), 1);
        const plot = element('div', 'bar-plot');
        plot.setAttribute('role', 'list');
        plot.setAttribute('aria-label', 'Quantidade de chamados concluídos por período');
        const labels = element('div', 'bar-labels');
        labels.setAttribute('aria-hidden', 'true');
        const width = `${data.length * 90}px`;
        plot.style.minWidth = width;
        labels.style.minWidth = width;
        for (const item of data) {
            const label = formatPeriod(item.periodo, mode);
            const column = element('div', 'bar-column');
            column.setAttribute('role', 'listitem');
            column.setAttribute('aria-label', `${label}: ${number.format(item.quantidade)} chamados concluídos`);
            column.title = `${label}: ${number.format(item.quantidade)}`;
            const bar = element('div', 'period-bar');
            bar.style.height = `${item.quantidade / maximum * 80}%`;
            column.append(element('strong', '', number.format(item.quantidade)), bar);
            plot.append(column);
            labels.append(element('span', '', label));
        }
        chart.append(plot, labels);
    }

    function renderRanking(data) {
        if (!Array.isArray(data)) throw new Error('Formato de ranking inválido.');
        data.forEach(item => {
            assertCount(item.quantidade);
            if (typeof item.tipo !== 'string') throw new Error('Tipo inválido na resposta.');
        });
        const chart = byId('ranking-chart');
        chart.replaceChildren();
        chart.hidden = data.length === 0;
        message('ranking-message', data.length === 0 ? 'Nenhum atendimento concluído para montar o ranking.' : '');
        const maximum = Math.max(...data.map(item => item.quantidade), 1);
        data.forEach((item, index) => {
            const row = element('li', 'ranking-row');
            const track = element('div', 'ranking-track');
            track.setAttribute('aria-hidden', 'true');
            const fill = element('div', 'ranking-fill');
            fill.style.width = `${item.quantidade / maximum * 100}%`;
            track.append(fill);
            row.append(element('span', 'rank-number', String(index + 1).padStart(2, '0')),
                element('span', 'rank-name', item.tipo), track,
                element('strong', '', `${number.format(item.quantidade)} ${item.quantidade === 1 ? 'chamado' : 'chamados'}`));
            chart.append(row);
        });
    }

    async function loadTypes() {
        if (typesLoaded) return;
        try {
            const types = await request('tipos');
            if (!Array.isArray(types) || types.some(type => typeof type !== 'string')) throw new Error('Lista de tipos inválida.');
            const select = byId('tipo');
            const previous = select.value;
            select.replaceChildren(new Option('Todos os tipos', ''));
            types.forEach(type => select.add(new Option(type, type)));
            select.value = previous;
            typesLoaded = true;
            message('types-error', '');
        } catch (error) {
            message('types-error', `Filtro de tipos indisponível. ${error.message}`, true);
        }
    }

    async function update() {
        if (busy || !form.reportValidity()) return;
        const start = byId('dataInicio').value;
        const end = byId('dataFim').value;
        if (start && end && start > end) {
            message('filter-error', 'A data inicial deve ser anterior ou igual à data final.', true);
            return;
        }
        message('filter-error', '');
        const common = new URLSearchParams();
        if (start) common.set('dataInicio', start);
        if (end) common.set('dataFim', end);
        if (byId('tipo').value) common.set('tipo', byId('tipo').value);
        const overview = new URLSearchParams(common);
        if (byId('status').value) overview.set('status', byId('status').value);
        const attendance = new URLSearchParams(common);
        const mode = grouping.value;
        attendance.set('agrupamento', mode);
        busy = true;
        byId('filter-fields').disabled = true;
        grouping.disabled = true;
        byId('updated-at').textContent = 'Atualizando…';
        ['overview', 'attendance-panel', 'situation-panel', 'ranking-panel'].forEach(id => byId(id).setAttribute('aria-busy', 'true'));
        ['attendance-chart', 'situation-chart', 'ranking-chart'].forEach(id => {
            byId(id).hidden = true;
        });
        byId('metric-total').textContent = '—';
        statuses.forEach(([key]) => {
            byId(`metric-${key}`).textContent = '—';
        });
        message('overview-message', 'Carregando indicadores…');
        message('situation-message', 'Carregando situação…');
        message('attendance-message', 'Carregando atendimentos…');
        message('ranking-message', 'Carregando ranking…');
        const jobs = [
            request('indicadores', overview).then(renderOverview).catch(error => {
                message('overview-message', error.message, true);
                message('situation-message', 'Situação indisponível. Tente Atualizar novamente.', true);
                throw error;
            }),
            request('atendidos', attendance).then(data => renderAttendance(data, mode)).catch(error => {
                message('attendance-message', error.message, true);
                throw error;
            }),
            request('tipos-mais-atendidos', common).then(renderRanking).catch(error => {
                message('ranking-message', error.message, true);
                throw error;
            })
        ];
        try {
            const results = await Promise.allSettled([...jobs, loadTypes()]);
            const failed = results.slice(0, 3).some(result => result.status === 'rejected');
            byId('updated-at').textContent = failed ? 'Atualização incompleta · tente novamente'
                : `Atualizado às ${new Date().toLocaleTimeString('pt-BR', {hour: '2-digit', minute: '2-digit'})}`;
        } finally {
            busy = false;
            byId('filter-fields').disabled = false;
            grouping.disabled = false;
            ['overview', 'attendance-panel', 'situation-panel', 'ranking-panel'].forEach(id => byId(id).setAttribute('aria-busy', 'false'));
        }
    }

    form.addEventListener('submit', event => {
        event.preventDefault();
        update();
    });
    form.addEventListener('reset', () => {
        grouping.value = 'DIA';
        setTimeout(update, 0);
    });
    grouping.addEventListener('change', update);
    update();
})();
