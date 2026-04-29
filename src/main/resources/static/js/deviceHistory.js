const charts = new Map();

const palettes = {
    'ltrad': ['#007bff', '#4dabf7', '#a5d8ff'],
    'fire': ['#e85d04', '#ff922b', '#ffba08'],
    'volcano': ['#9d0208', '#f48c06', '#ffba08'],
    'default': ['#2d6cdf', '#4c9f70', '#dd6b20']
};

// Dizionario JS speculare al SignalDictionary.java
const SIGNAL_CHART_TYPE = {
    'temperature_celsius': 'gauge+line',
    'humidity_percent': 'gauge+line',
    'co2concentration_ppm': 'gauge+line',
    'pressure_hpa': 'gauge+line',
    'gasresistance_ohm': 'gauge+line',
    'voc_index': 'gauge+line',
    'nox_index': 'gauge+line',
    'si_m_s': 'gauge+line',
    'pga_m_s2': 'gauge+line',
    'pm1_0_ugm3': 'area+line',
    'pm2_5_ugm3': 'area+line',
    'pm4_0_ugm3': 'area+line',
    'pm10_0_ugm3': 'area+line',
    'earthquake_flag': 'boolean',
    'shutoff': 'boolean',
    'collapse': 'boolean',
    'state': 'status',
    'axis_state': 'status',
};
const SIGNAL_DISPLAY_NAME = {
    'temperature_celsius': 'Temperature',
    'humidity_percent': 'Relative Humidity',
    'co2concentration_ppm': 'CO₂ Concentration',
    'pressure_hpa': 'Atmospheric Pressure',
    'gasresistance_ohm': 'Air Quality (IAQ)',
    'voc_index': 'VOC Index',
    'nox_index': 'NOx Index',
    'si_m_s': 'Seismic Intensity (SI)',
    'pga_m_s2': 'Peak Ground Acceleration',
    'pm1_0_ugm3': 'PM1.0 Particulate',
    'pm2_5_ugm3': 'PM2.5 Particulate',
    'pm4_0_ugm3': 'PM4.0 Particulate',
    'pm10_0_ugm3': 'PM10 Particulate',
};

const SIGNAL_UNIT = {
    'temperature_celsius': '°C',
    'humidity_percent': '%RH',
    'co2concentration_ppm': 'ppm',
    'pressure_hpa': 'hPa',
    'gasresistance_ohm': 'Ω',
    'voc_index': 'VOC',
    'nox_index': 'NOx',
    'si_m_s': 'm/s',
    'pga_m_s2': 'm/s²',
    'pm1_0_ugm3': 'µg/m³',
    'pm2_5_ugm3': 'µg/m³',
    'pm4_0_ugm3': 'µg/m³',
    'pm10_0_ugm3': 'µg/m³',
};

function getChartType(signalKey) {
    return SIGNAL_CHART_TYPE[(signalKey || '').toLowerCase()] || 'gauge+line';
}

document.addEventListener('DOMContentLoaded', () => {
    setupDropdown();
    loadAggregated();
    document.getElementById('agg-period').addEventListener('change', loadAggregated);
    document.getElementById('agg-days').addEventListener('change', loadAggregated);
});

function setupDropdown() {
    const toggle = document.getElementById('userDropdown');
    const menu = document.getElementById('userDropdownMenu');
    if (!toggle || !menu) return;
    toggle.addEventListener('click', e => {
        e.stopPropagation();
        const expanded = toggle.getAttribute('aria-expanded') === 'true';
        toggle.setAttribute('aria-expanded', (!expanded).toString());
        menu.classList.toggle('show');
    });
    document.addEventListener('click', e => {
        if (!menu.contains(e.target) && e.target !== toggle) {
            menu.classList.remove('show');
            toggle.setAttribute('aria-expanded', 'false');
        }
    });
}

async function loadAggregated() {
    const period = document.getElementById('agg-period').value;
    const days = document.getElementById('agg-days').value;
    const empty = document.getElementById('history-empty');
    const grid = document.getElementById('history-grid');

    const res = await fetch(
        `/api/admin/devices/${encodeURIComponent(DEVICE_KEY)}/telemetry/aggregated?period=${period}&days=${days}`
    );

    if (!res.ok) { empty.hidden = false; grid.innerHTML = ''; return; }

    const points = await res.json();

    const byKey = new Map();
    points.forEach(p => {
        if (!byKey.has(p.signalKey)) byKey.set(p.signalKey, []);
        byKey.get(p.signalKey).push(p);
    });

    charts.forEach(c => c.destroy());
    charts.clear();
    grid.innerHTML = '';

    if (!byKey.size) { empty.hidden = false; return; }
    empty.hidden = true;

    const palette = palettes[PROJECT_KEY] || palettes['default'];
    let colorIdx = 0;

    byKey.forEach((pts, key) => {
        const color = palette[colorIdx % palette.length];
        colorIdx++;
        const info = pts[0];
        const title = SIGNAL_DISPLAY_NAME[key.toLowerCase()] || info.displayName || key;
        const unit = SIGNAL_UNIT[key.toLowerCase()] || info.unit || '';
        const chartType = getChartType(key);
        const labels = pts.map(p => formatBucket(p.bucket, period));

        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'display:flex;flex-direction:column;gap:0.5rem;';

        const lbl = document.createElement('p');
        lbl.style.cssText = 'margin:0;font-size:0.9rem;font-weight:500;color:rgba(255,255,255,0.7);';
        lbl.textContent = unit ? `${title} (${unit})` : title;
        wrapper.appendChild(lbl);

        const canvas = document.createElement('canvas');
        const canvasH = (chartType === 'boolean' || chartType === 'status') ? '80px' : '220px';
        canvas.style.cssText = `width:100%!important;height:${canvasH}!important;`;
        wrapper.appendChild(canvas);
        grid.appendChild(wrapper);

        const ctx = canvas.getContext('2d');
        let chart;

        if (chartType === 'boolean') {
            // Step chart 0/1 con area rossa quando 1
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: title,
                        data: pts.map(p => p.avg != null ? Math.round(p.avg) : null),
                        borderColor: '#ef5350',
                        backgroundColor: 'rgba(239,83,80,0.25)',
                        borderWidth: 2,
                        stepped: true,
                        fill: 'origin',
                        pointRadius: 3,
                        spanGaps: true
                    }]
                },
                options: chartOptions(unit, { yMin: -0.1, yMax: 1.1, yTicks: [0, 1] })
            });

        } else if (chartType === 'status') {
            // Step chart con valori discreti
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: title,
                        data: pts.map(p => p.avg != null ? Math.round(p.avg) : null),
                        borderColor: color,
                        backgroundColor: color + '33',
                        borderWidth: 2,
                        stepped: true,
                        fill: 'origin',
                        pointRadius: 3,
                        spanGaps: true
                    }]
                },
                options: chartOptions(unit, {})
            });

        } else if (chartType === 'area+line') {
            // Area piena con banda min-max
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [
                        {
                            label: 'Max',
                            data: pts.map(p => p.max),
                            borderColor: color + '55',
                            backgroundColor: color + '22',
                            borderWidth: 1,
                            borderDash: [4, 3],
                            pointRadius: 0,
                            fill: '+1',
                            tension: 0.3,
                            spanGaps: true
                        },
                        {
                            label: 'Avg',
                            data: pts.map(p => p.avg),
                            borderColor: color,
                            backgroundColor: color + '44',
                            borderWidth: 2,
                            pointRadius: 2,
                            fill: 'origin',
                            tension: 0.3,
                            spanGaps: true
                        },
                        {
                            label: 'Min',
                            data: pts.map(p => p.min),
                            borderColor: color + '55',
                            backgroundColor: color + '22',
                            borderWidth: 1,
                            borderDash: [4, 3],
                            pointRadius: 0,
                            fill: '-1',
                            tension: 0.3,
                            spanGaps: true
                        }
                    ]
                },
                options: chartOptions(unit, {})
            });

        } else {
            // gauge+line — linea avg con banda min-max sottile
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [
                        {
                            label: 'Max',
                            data: pts.map(p => p.max),
                            borderColor: color + '44',
                            backgroundColor: color + '11',
                            borderWidth: 1,
                            borderDash: [3, 3],
                            pointRadius: 0,
                            fill: '+1',
                            tension: 0.3,
                            spanGaps: true
                        },
                        {
                            label: 'Avg',
                            data: pts.map(p => p.avg),
                            borderColor: color,
                            backgroundColor: 'transparent',
                            borderWidth: 2.5,
                            pointRadius: 2,
                            pointHoverRadius: 5,
                            tension: 0.3,
                            spanGaps: true
                        },
                        {
                            label: 'Min',
                            data: pts.map(p => p.min),
                            borderColor: color + '44',
                            backgroundColor: color + '11',
                            borderWidth: 1,
                            borderDash: [3, 3],
                            pointRadius: 0,
                            fill: '-1',
                            tension: 0.3,
                            spanGaps: true
                        }
                    ]
                },
                options: chartOptions(unit, {})
            });
        }

        charts.set(key, chart);
    });
}

function chartOptions(unit, extra) {
    const yOpts = {
        ticks: { color: '#dde1f2' },
        grid: { color: 'rgba(255,255,255,0.08)' }
    };
    if (extra.yMin !== undefined) yOpts.min = extra.yMin;
    if (extra.yMax !== undefined) yOpts.max = extra.yMax;
    if (extra.yTicks) {
        yOpts.ticks = {
            ...yOpts.ticks,
            callback: v => extra.yTicks.includes(v) ? v : '',
            stepSize: 1
        };
    }
    return {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
            legend: { labels: { color: '#ffffff', boxWidth: 12, font: { size: 11 } } },
            tooltip: {
                callbacks: {
                    label: ctx => {
                        const v = ctx.parsed.y;
                        return `${ctx.dataset.label}: ${v != null ? v.toFixed(2) : '--'} ${unit}`;
                    }
                }
            }
        },
        scales: {
            x: { ticks: { color: '#dde1f2', maxTicksLimit: 12 }, grid: { color: 'rgba(255,255,255,0.08)' } },
            y: yOpts
        }
    };
}

function formatBucket(iso, period) {
    const d = new Date(iso);
    if (period === 'hour')
        return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    if (period === 'week')
        return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}
