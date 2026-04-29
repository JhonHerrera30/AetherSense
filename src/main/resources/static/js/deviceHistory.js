const charts = new Map();

const palettes = {
    'ltrad': ['#4dabf7', '#69db7c', '#ffd43b', '#ff8787', '#da77f2'],
    'fire': ['#ff6b35', '#ff922b', '#ffd43b', '#ff8787', '#f783ac'],
    'volcano': ['#ff6348', '#ffa502', '#eccc68', '#a29bfe', '#74b9ff'],
    'default': ['#4dabf7', '#69db7c', '#ffd43b', '#ff8787', '#da77f2']
};

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
    'earthquake_flag': 'Earthquake Detected',
    'shutoff': 'Shutoff',
    'collapse': 'Collapse',
    'state': 'Sensor State',
    'axis_state': 'Axis State',
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
        const keyLow = key.toLowerCase();
        const title = SIGNAL_DISPLAY_NAME[keyLow] || pts[0].displayName || key;
        const unit = SIGNAL_UNIT[keyLow] || pts[0].unit || '';
        const chartType = getChartType(key);
        const labels = pts.map(p => formatBucket(p.bucket, period));

        if (pts.length < 2 && chartType !== 'boolean' && chartType !== 'status') {
            const msg = document.createElement('p');
            msg.style.cssText = 'margin:0;font-size:0.8rem;color:rgba(255,255,255,0.4);padding:0.5rem 0;';
            msg.textContent = `Only ${pts.length} data point — add more data to see the trend.`;
            wrapper.appendChild(msg);
            grid.appendChild(wrapper);
            return;
        }

        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'display:flex;flex-direction:column;gap:0.4rem;';

        const lbl = document.createElement('p');
        lbl.style.cssText = 'margin:0;font-size:0.85rem;font-weight:500;color:rgba(255,255,255,0.65);letter-spacing:0.03em;text-transform:uppercase;';
        lbl.textContent = unit ? `${title} (${unit})` : title;
        wrapper.appendChild(lbl);

        const canvas = document.createElement('canvas');
        const canvasH = (chartType === 'boolean' || chartType === 'status') ? '100px' : '200px';
        canvas.style.cssText = `width:100%!important;height:${canvasH}!important;`;
        wrapper.appendChild(canvas);
        grid.appendChild(wrapper);

        const ctx = canvas.getContext('2d');
        let chart;

        if (chartType === 'boolean') {
            const values = pts.map(p => p.avg != null ? Math.round(p.avg) : 0);
            const barColors = values.map(v =>
                v === 1 ? 'rgba(239,83,80,0.85)' : 'rgba(102,187,106,0.7)'
            );
            chart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels,
                    datasets: [{
                        label: title,
                        data: values.map(() => 1),
                        backgroundColor: barColors,
                        borderColor: barColors,
                        borderWidth: 1,
                        borderRadius: 4,
                        borderSkipped: false,
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: (ctx2) => {
                                    return values[ctx2.dataIndex] === 1 ? '⚠ Issue' : '✓ OK';
                                }
                            }
                        }
                    },
                    scales: {
                        x: { ticks: { color: '#dde1f2', maxTicksLimit: 12 }, grid: { display: false } },
                        y: { display: false, min: 0, max: 1.2 }
                    }
                }
            });

        } else if (chartType === 'status') {
            // Bar chart con valore numerico, colore da palette discreta
            const stateColors = ['#74b9ff', '#a29bfe', '#fd79a8', '#fdcb6e', '#00b894', '#ff7675', '#6c5ce7', '#e17055'];
            const barCols = pts.map(p => {
                const v = p.avg != null ? Math.round(p.avg) : 0;
                return stateColors[v % stateColors.length] + 'cc';
            });
            chart = new Chart(ctx, {
                type: 'bar',
                data: {
                    labels,
                    datasets: [{
                        label: title,
                        data: pts.map(p => p.avg != null ? Math.round(p.avg) : 0),
                        backgroundColor: barCols,
                        borderWidth: 0,
                        borderRadius: 3,
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                label: ctx2 => `State: ${ctx2.parsed.y}`
                            }
                        }
                    },
                    scales: {
                        x: { ticks: { color: '#dde1f2', maxTicksLimit: 12 }, grid: { display: false } },
                        y: { ticks: { color: '#dde1f2', stepSize: 1 }, grid: { color: 'rgba(255,255,255,0.06)' }, min: 0 }
                    }
                }
            });

        } else if (chartType === 'area+line') {
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [
                        { label: 'Max', data: pts.map(p => p.max), borderColor: color + '66', backgroundColor: color + '18', borderWidth: 1, borderDash: [4, 3], pointRadius: 0, fill: '+1', tension: 0.35, spanGaps: true },
                        { label: 'Avg', data: pts.map(p => p.avg), borderColor: color, backgroundColor: color + '40', borderWidth: 2.5, pointRadius: 2, pointHoverRadius: 5, fill: 'origin', tension: 0.35, spanGaps: true },
                        { label: 'Min', data: pts.map(p => p.min), borderColor: color + '66', backgroundColor: color + '18', borderWidth: 1, borderDash: [4, 3], pointRadius: 0, fill: '-1', tension: 0.35, spanGaps: true }
                    ]
                },
                options: lineChartOptions(unit)
            });

        } else {
            // gauge+line — linea Avg prominente, banda Min-Max sottile
            chart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels,
                    datasets: [
                        { label: 'Max', data: pts.map(p => p.max), borderColor: color + '40', backgroundColor: color + '0d', borderWidth: 1, borderDash: [3, 3], pointRadius: 0, fill: '+1', tension: 0.35, spanGaps: true },
                        { label: 'Avg', data: pts.map(p => p.avg), borderColor: color, backgroundColor: 'transparent', borderWidth: 2.5, pointRadius: 2.5, pointHoverRadius: 5, tension: 0.35, spanGaps: true },
                        { label: 'Min', data: pts.map(p => p.min), borderColor: color + '40', backgroundColor: color + '0d', borderWidth: 1, borderDash: [3, 3], pointRadius: 0, fill: '-1', tension: 0.35, spanGaps: true }
                    ]
                },
                options: lineChartOptions(unit)
            });
        }

        charts.set(key, chart);
    });
}

function lineChartOptions(unit) {
    return {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
            legend: { labels: { color: '#ffffff', boxWidth: 10, font: { size: 11 } } },
            tooltip: {
                callbacks: {
                    label: ctx => {
                        const v = ctx.parsed.y;
                        return `${ctx.dataset.label}: ${v != null ? v.toFixed(2) : '--'}${unit ? ' ' + unit : ''}`;
                    }
                }
            }
        },
        scales: {
            x: { ticks: { color: '#9fa8c7', maxTicksLimit: 12, font: { size: 11 } }, grid: { color: 'rgba(255,255,255,0.06)' } },
            y: { ticks: { color: '#9fa8c7', font: { size: 11 } }, grid: { color: 'rgba(255,255,255,0.06)' } }
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