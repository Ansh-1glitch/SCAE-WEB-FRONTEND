// ── P03: Load My Complaints from Backend ────────────────────────────
async function loadMyComplaints() {
  try {
    const res = await api('/complaints');
    myComplaints = res.data.map(c => ({
      id:     c.id,
      cat:    c.category  || c.cat  || 'Other',
      desc:   c.description || c.desc || '',
      urg:    c.urgency   || c.urg  || 5,
      zone:   c.zone      || '',
      status: c.status    || 'Pending',
      date:   c.date_filed || c.date || '',
      name:   c.name      || 'Citizen'
    }));
    renderMC();
    updateStats();
  } catch (e) {
    // Backend unreachable — show empty state, NO local fallback data
    myComplaints = [];
    renderMC();
    updateStats();
    console.warn('Could not load complaints from backend:', e.message);
  }
}

document.addEventListener('DOMContentLoaded', async () => {
  const token = sessionStorage.getItem('scae_token');
  const role = sessionStorage.getItem('scae_role');
  if (!token || role !== 'citizen') {
    window.location.href = 'index.html';
    return;
  }
  const name = sessionStorage.getItem('scae_name');
  const loggedInEl = document.getElementById('loggedInName');
  if (loggedInEl) loggedInEl.textContent = name || 'Citizen';

  showPanel('p01');
  await Promise.all([
    loadMyComplaints(),
    loadGraphData()
  ]);
});


// ── Graph Data ──────────────────────────────────────────────────
let NODES = [
  { id: 0, x: 80, y: 120, l: 'Central Junction', area: 'Central Junction', label: 'Central Junction' },
  { id: 1, x: 220, y: 200, l: 'North Gate', area: 'North Gate', label: 'North Gate' },
  { id: 2, x: 340, y: 100, l: 'City Hall', area: 'City Hall', label: 'City Hall' },
  { id: 3, x: 480, y: 180, l: 'East Market', area: 'East Market', label: 'East Market' },
  { id: 4, x: 160, y: 280, l: 'West Park', area: 'West Park', label: 'West Park' },
  { id: 5, x: 320, y: 300, l: 'Downtown', area: 'Downtown', label: 'Downtown' },
  { id: 6, x: 260, y: 380, l: 'South Bridge', area: 'South Bridge', label: 'South Bridge' },
  { id: 7, x: 420, y: 360, l: 'River Road', area: 'River Road', label: 'River Road' },
  { id: 8, x: 520, y: 350, l: 'Industrial Zone', area: 'Industrial Zone', label: 'Industrial Zone' },
  { id: 9, x: 300, y: 220, l: 'Medical Hub', area: 'Medical Hub', label: 'Medical Hub' },
  { id: 10, x: 460, y: 290, l: 'Tech District', area: 'Tech District', label: 'Tech District' },
  { id: 11, x: 120, y: 380, l: 'Old Town', area: 'Old Town', label: 'Old Town' },
  { id: 12, x: 380, y: 430, l: 'Sports Complex', area: 'Sports Complex', label: 'Sports Complex' }
];
let EDGES = [
  [0, 1, 3], [0, 4, 4], [1, 2, 5], [1, 4, 2], [1, 9, 6], [2, 3, 4], [2, 9, 3],
  [3, 5, 5], [3, 10, 7], [4, 6, 8], [4, 11, 4], [5, 9, 2], [5, 10, 4], [6, 7, 5],
  [6, 11, 3], [7, 8, 4], [7, 10, 3], [8, 12, 5], [9, 10, 1]
];
let N = NODES.length;
// ── Map options ──────────────────────────────────────────────────
const ROUTE_OPTS = { NODES, EDGES, viewW: 620, viewH: 520 };
const BFS_OPTS = { NODES, EDGES, viewW: 620, viewH: 500 };

function buildAdj(skip) {
  const adj = Array.from({ length: N }, () => []);
  EDGES.forEach(([u, v, w], i) => { if (i !== skip) { adj[u].push([v, w]); adj[v].push([u, w]); } });
  return adj;
}

// ── Panel Switching ──────────────────────────────────────────────
const CITIZEN_PANELS = { p01: 'Find My Route / मार्ग खोजें', p02: 'File a Complaint / शिकायत दर्ज करें', p03: 'My Complaint History / मेरी शिकायत का इतिहास', p04: 'Find City Services / शहर सेवाएं खोजें', p05: 'My Zone Information / मेरे क्षेत्र की जानकारी' };
function showPanel(id) {
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(id)?.classList.add('active');
  document.querySelector(`[data-panel="${id}"]`)?.classList.add('active');
  const bc = document.getElementById('bc-panel'); if (bc) bc.textContent = CITIZEN_PANELS[id] || id;
}
document.querySelectorAll('.nav-item').forEach(n => { n.addEventListener('click', () => showPanel(n.dataset.panel)); });

function showToast(msg, type = 'success') {
  let t = document.getElementById('toast');
  if (!t) {
    t = document.createElement('div');
    t.id = 'toast';
    document.body.appendChild(t);
  }

  t.style.cssText = 'position:fixed;bottom:24px;right:24px;padding:12px 20px;border-radius:4px;font-size:13px;font-family:Arial,sans-serif;z-index:9999;max-width:400px;box-shadow:0 2px 10px rgba(0,0,0,.3);transition:opacity .3s;color:#fff;';
  t.style.background = type === 'error' ? '#b91c1c' : '#2E7D32';
  t.textContent = msg;
  t.style.opacity = '1';
  t.style.display = 'block';
  clearTimeout(t._scaeTimer);
  t._scaeTimer = setTimeout(() => { t.style.opacity = '0'; setTimeout(() => t.style.display = 'none', 300); }, 3500);
}
function openModal(html) { document.getElementById('modal-content').innerHTML = html; document.getElementById('modal').classList.add('open'); }
function closeModal() { document.getElementById('modal').classList.remove('open'); }
document.getElementById('modal')?.addEventListener('click', e => { if (e.target === e.currentTarget) closeModal(); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

// ── Draw Map SVG (MapEngine wrapper) ─────────────────────────
function drawMap(svgId, hlEdges = [], hlNodes = [], hopMap = {}) {
  const opts = svgId === 'bfs-map' ? BFS_OPTS : ROUTE_OPTS;
  const overrides = {};
  if (Object.keys(hopMap).length) {
    // BFS coloring: mark hopped nodes as visited
    const hopNodes = Object.keys(hopMap).map(Number).filter(i => hopMap[i] > 0);
    overrides.visitedNodes = hopNodes;
  }
  MapEngine.draw(svgId, hlEdges, hlNodes, overrides, opts);
}

// ── P01: Dijkstra ────────────────────────────────────────────────
// Dropdowns and maps are populated by loadGraphData() on DOMContentLoaded.

function dijkstra(start, end, skipEdge) {
  const adj = buildAdj(skipEdge);
  const dist = Array(N).fill(Infinity), prev = Array(N).fill(-1);
  const vis = new Set(); dist[start] = 0;
  for (let i = 0; i < N; i++) {
    let u = -1;
    for (let j = 0; j < N; j++) if (!vis.has(j) && (u === -1 || dist[j] < dist[u])) u = j;
    if (u === -1 || dist[u] === Infinity) break;
    vis.add(u);
    adj[u].forEach(([v, w]) => { if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; prev[v] = u; } });
  }
  if (dist[end] === Infinity) return { path: [], dist: Infinity };
  const path = []; let cur = end;
  while (cur !== -1) { path.unshift(cur); cur = prev[cur]; }
  return { path, dist: dist[end] };
}

function edgeIndex(a, b) { return EDGES.findIndex(([u, v]) => (u === a && v === b) || (u === b && v === a)); }

let lastPath = [];
async function runDijkstra() {
  const src = +document.getElementById('d-src').value;
  const dst = +document.getElementById('d-dst').value;
  showLoading('d-path');
  try {
    const res = await api('/algo/dijkstra', 'POST', { source: src, destination: dst });
    const path = res.data.path;
    lastPath = path;
    const hlE = [];
    for (let i = 0; i < path.length - 1; i++) { const ei = edgeIndex(path[i], path[i + 1]); if (ei >= 0) hlE.push(ei); }
    animatePath(path, 'route-map');
    const labels = res.data.path_labels || path.map(i => NODES[i].label);
    const chips = labels.map(l => `<span style="display:inline-flex;align-items:center;background:#0A2010;color:#7FD4A8;border:1px solid #2E6B3E;border-radius:14px;padding:2px 10px;font-size:11px;font-family:'Inter',sans-serif;margin:2px 1px">${l}</span>`).join('<span style="color:#2E6B3E;font-size:12px;margin:0 2px"> </span>');
    document.getElementById('d-path').innerHTML = path.length ? chips : 'No route available';
    document.getElementById('d-dist').textContent = res.data.distance === Infinity ? 'No connection' : res.data.distance + ' km';
    document.getElementById('d-time').textContent = 'API';
    document.getElementById('bf-toggle').checked = false;
    document.getElementById('bf-result').innerHTML = '';
  } catch (e) {
    const src = +document.getElementById('d-src').value;
    const dst = +document.getElementById('d-dst').value;
    const t0 = performance.now();
    const res = dijkstra(src, dst);
    const t1 = performance.now();
    lastPath = res.path;
    const hlE = [];
    for (let i = 0; i < res.path.length - 1; i++) { const ei = edgeIndex(res.path[i], res.path[i + 1]); if (ei >= 0) hlE.push(ei); }
    // ── Animated route draw (algorithm unchanged above) ────────────
    animatePath(res.path, 'route-map');
    // Breadcrumb chips
    const labels = res.path.map(i => (NODES[i]?.area || NODES[i]?.label || NODES[i]?.l || 'Node ' + i));
    const chips = labels.map(l => `<span style="display:inline-flex;align-items:center;background:#0A2010;color:#7FD4A8;border:1px solid #2E6B3E;border-radius:14px;padding:2px 10px;font-size:11px;font-family:'Inter',sans-serif;margin:2px 1px">${l}</span>`).join('<span style="color:#2E6B3E;font-size:12px;margin:0 2px">→</span>');
    document.getElementById('d-path').innerHTML = res.path.length ? chips : 'No route available';
    document.getElementById('d-dist').textContent = res.dist === Infinity ? 'No connection' : res.dist + ' km';
    document.getElementById('d-time').textContent = (t1 - t0).toFixed(3) + ' ms';
    document.getElementById('bf-toggle').checked = false;
    document.getElementById('bf-result').innerHTML = '';
  }
}
function bellmanFord(start, end, skipEdge) {
  const dist = Array(N).fill(Infinity), prev = Array(N).fill(-1);
  dist[start] = 0;
  for (let i = 0; i < N - 1; i++) {
    EDGES.forEach(([u, v, w], ei) => {
      if (ei === skipEdge) return;
      if (dist[u] + w < dist[v]) { dist[v] = dist[u] + w; prev[v] = u; }
      if (dist[v] + w < dist[u]) { dist[u] = dist[v] + w; prev[u] = v; }
    });
  }
  if (dist[end] === Infinity) return { path: [], dist: Infinity };
  const path = []; let cur = end;
  while (cur !== -1) { path.unshift(cur); cur = prev[cur]; }
  return { path, dist: dist[end] };
}

function toggleBF() {
  const on = document.getElementById('bf-toggle').checked;
  const el = document.getElementById('bf-result');
  if (!on) { el.innerHTML = ''; runDijkstra(); return; }
  if (lastPath.length < 2) { el.innerHTML = '<p style="color:var(--muted);font-size:12px;">Get directions first, then toggle road closure.</p>'; return; }
  const src = +document.getElementById('d-src').value;
  const dst = +document.getElementById('d-dst').value;
  const closeEI = edgeIndex(lastPath[0], lastPath[1]);
  const t0 = performance.now();
  const res = bellmanFord(src, dst, closeEI);
  const t1 = performance.now();
  const hlE = [];
  for (let i = 0; i < res.path.length - 1; i++) { const ei = edgeIndex(res.path[i], res.path[i + 1]); if (ei >= 0) hlE.push(ei); }
  animatePath(res.path, 'route-map');
  el.innerHTML = `<div class="result-box" style="border-left:3px solid #f59e0b;">
    <div style="font-size:11px;color:#92400e;font-weight:600;margin-bottom:8px;">⚠ Re-routed due to road closure (${NODES[lastPath[0]].l} – ${NODES[lastPath[1]].l})</div>
    <div class="rr">Alternate Route: <span>${res.path.map(i => NODES[i].l).join(' → ') || 'No route available'}</span></div>
    <div class="rr">Total Distance: <span>${res.dist === Infinity ? 'No connection' : res.dist + ' km'}</span></div>
    <div class="rr">Calculated in: <span>${(t1 - t0).toFixed(3)} ms</span></div>
  </div>`;
}

// ── P02: File Complaint ──────────────────────────────────────────
function updateUrg() {
  const v = +document.getElementById('fc-urg').value;
  document.getElementById('urg-val').textContent = v;
  const labels = { 1: 'Low', 2: 'Low', 3: 'Low', 4: 'Medium', 5: 'Medium', 6: 'Medium', 7: 'High', 8: 'High', 9: 'Critical', 10: 'Critical' };
  document.getElementById('urg-label').textContent = '(' + labels[v] + ')';
  const colors = { Low: '#166534', Medium: '#92400e', High: '#b91c1c', Critical: '#7f1d1d' };
  document.getElementById('urg-val').style.color = colors[labels[v]];
}

async function submitComplaint() {
  const cat  = document.getElementById('fc-cat').value;
  const loc  = document.getElementById('fc-loc').value;
  const urg  = +document.getElementById('fc-urg').value;
  const desc = document.getElementById('fc-desc').value.trim();
  const name = document.getElementById('fc-name').value.trim();
  if (!desc || desc.length < 10) { showToast('Description must be at least 10 characters.', 'error'); return; }
  if (!name) { showToast('Please enter your name.', 'error'); return; }

  // Disable button while submitting
  const btn = document.querySelector('#p02 .btn-g');
  if (btn) { btn.disabled = true; btn.textContent = 'Submitting...'; }

  try {
    const res = await api('/complaints', 'POST', { category: cat, zone: loc, urgency: urg, description: desc, name });
    const refId = res?.data?.id || res?.data?.complaint_id || 'submitted';
    showToast(`✅ Complaint filed! Reference: ${refId}`);
    // Clear form
    document.getElementById('fc-desc').value = '';
    document.getElementById('fc-name').value = '';
    if (document.getElementById('fc-contact')) document.getElementById('fc-contact').value = '';
    if (document.getElementById('char-ct')) document.getElementById('char-ct').textContent = '0/500';
    // Reload from backend so the list is always in sync
    await loadMyComplaints();
    showPanel('p03'); // Switch to history tab to see the new complaint
  } catch (e) {
    // Do NOT add locally — backend is the single source of truth
    showToast('❌ Could not submit complaint. Please check your connection and try again.', 'error');
    console.error('submitComplaint failed:', e);
  } finally {
    if (btn) { btn.disabled = false; btn.textContent = 'Submit Complaint'; }
  }
}

// ── P03: My Complaints ───────────────────────────────────────────
// All complaint data is loaded from the backend via loadMyComplaints().
let myComplaints = [];

function urgBadge(u) {
  if (u >= 9) return `<span class="urg urg-c">${u}</span>`;
  if (u >= 7) return `<span class="urg urg-h">${u}</span>`;
  if (u >= 4) return `<span class="urg urg-m">${u}</span>`;
  return `<span class="urg urg-l">${u}</span>`;
}
function statusPill(s) { const m = { Pending: 'sp-p', 'In Progress': 'sp-ip', Resolved: 'sp-r' }; return `<span class="sp ${m[s] || 'sp-p'}">${s}</span>`; }

function renderMC() {
  document.getElementById('mc-tbody').innerHTML = myComplaints.map(c =>
    `<tr><td style="font-family:var(--mono);">${c.id}</td><td>${c.cat}</td><td${c.status === 'Resolved' ? ' style="text-decoration:line-through;color:var(--muted);"' : ''}>${c.desc}</td><td>${urgBadge(c.urg)}</td><td>${c.zone}</td><td style="font-family:var(--mono);">${c.date}</td><td>${statusPill(c.status)}</td><td><button class="btn btn-ghost btn-sm" onclick="viewDetail('${c.id}')">View</button></td></tr>`
  ).join('');
}


function updateStats() {
  document.getElementById('mc-total').textContent = myComplaints.length;
  document.getElementById('mc-pending').textContent = myComplaints.filter(c => c.status !== 'Resolved').length;
  document.getElementById('mc-resolved').textContent = myComplaints.filter(c => c.status === 'Resolved').length;
}

function viewDetail(id) {
  const c = myComplaints.find(x => x.id === id); if (!c) return;
  const steps = ['Filed', 'Assigned', 'In Progress', 'Resolved'];
  const cur = c.status === 'Resolved' ? 3 : c.status === 'In Progress' ? 2 : 0;
  openModal(`<h3 style="font-family:'Merriweather',serif;margin-bottom:14px;">Complaint ${c.id}</h3>
    <div class="steps">${steps.map((s, i) => `<div class="step${i < cur ? ' done' : ''}${i === cur ? ' current' : ''}">${s}</div>`).join('')}</div>
    <div style="margin-top:16px;font-size:13px;"><p><strong>Category:</strong> ${c.cat}</p><p><strong>Description:</strong> ${c.desc}</p><p><strong>Priority:</strong> ${c.urg}/10</p><p><strong>Area:</strong> ${c.zone}</p><p><strong>Date Filed:</strong> ${c.date}</p><p><strong>Status:</strong> ${c.status}</p></div>`);
}

function searchComplaint(method) {
  const q = document.getElementById('mc-q').value.trim().toUpperCase();
  if (!q) { showToast('Enter a complaint ID to search.', 'error'); return; }
  const sorted = [...myComplaints].sort((a, b) => a.id.localeCompare(b.id));
  let binComps = 0, linComps = 0, found = null;
  let lo = 0, hi = sorted.length - 1;
  while (lo <= hi) { const mid = Math.floor((lo + hi) / 2); binComps++; if (sorted[mid].id === q) { found = sorted[mid]; break; } else if (sorted[mid].id < q) lo = mid + 1; else hi = mid - 1; }
  for (let i = 0; i < sorted.length; i++) { linComps++; if (sorted[i].id === q) break; }
  document.getElementById('search-result').innerHTML = `<div class="result-box" style="margin-bottom:14px;">
    <div class="rr">Result: <span>${found ? found.id + ' — ' + found.desc : 'Not found'}</span></div>
    <div class="rr">Fast Search: <span>Found in ${binComps} step${binComps !== 1 ? 's' : ''}</span></div>
    <div class="rr">Standard Search: <span>Found in ${linComps} step${linComps !== 1 ? 's' : ''}</span></div>
    <div class="rr">Speed Advantage: <span>Fast search is ${linComps > 0 ? (linComps / Math.max(binComps, 1)).toFixed(1) : '—'}x faster</span></div>
  </div>`;
}

function sortMC(by) {
  if (by === 'urgency') myComplaints.sort((a, b) => b.urg - a.urg);
  else if (by === 'date') myComplaints.sort((a, b) => b.date.localeCompare(a.date));
  else myComplaints.sort((a, b) => { const o = { Pending: 0, 'In Progress': 1, Resolved: 2 }; return o[a.status] - o[b.status]; });
  renderMC(); showToast(`Complaints sorted by ${by}.`);
}

// ── P04: Service Search ──────────────────────────────────────────
const SERVICES = [
  { name: 'ATM', zone: 'Central Junction' }, { name: 'Bank', zone: 'North Gate' }, { name: 'Bus Stop', zone: 'City Hall' },
  { name: 'Community Center', zone: 'West Park' }, { name: 'Emergency Shelter', zone: 'Old Town' },
  { name: 'Fire Station', zone: 'East Market' }, { name: 'Health Clinic', zone: 'South Bridge' },
  { name: 'Hospital', zone: 'Medical Hub' }, { name: 'Library', zone: 'Downtown' },
  { name: 'Market', zone: 'Downtown' }, { name: 'Metro Station', zone: 'City Hall' },
  { name: 'Park', zone: 'West Park' }, { name: 'Police Station', zone: 'Industrial Zone' },
  { name: 'Post Office', zone: 'River Road' }, { name: 'Power Substation', zone: 'East Market' },
  { name: 'School', zone: 'Tech District' }, { name: 'Sports Complex', zone: 'Sports Complex' },
  { name: 'Traffic Control Center', zone: 'Medical Hub' }, { name: 'Waste Management', zone: 'Old Town' },
  { name: 'Water Treatment Plant', zone: 'South Bridge' }
];

(function initServiceTable() {
  document.getElementById('ss-tbody').innerHTML = SERVICES.map((s, i) =>
    `<tr id="svc-${i}"><td>${i + 1}</td><td>${s.name}</td><td>${s.zone}</td></tr>`
  ).join('');
})();

function serviceSearch(method) {
  const q = document.getElementById('ss-q').value.trim().toLowerCase();
  if (!q) { showToast('Enter a service name to search.', 'error'); return; }
  let binComps = 0, binFound = -1, binSteps = [];
  let linComps = 0, linFound = -1;
  let lo = 0, hi = SERVICES.length - 1;
  while (lo <= hi) { const mid = Math.floor((lo + hi) / 2); binComps++; const cmp = SERVICES[mid].name.toLowerCase().localeCompare(q); binSteps.push({ lo, hi, mid, hit: cmp === 0 }); if (cmp === 0) { binFound = mid; break; } else if (cmp < 0) lo = mid + 1; else hi = mid - 1; }
  for (let i = 0; i < SERVICES.length; i++) { linComps++; if (SERVICES[i].name.toLowerCase() === q) { linFound = i; break; } }
  const found = binFound >= 0 ? SERVICES[binFound] : linFound >= 0 ? SERVICES[linFound] : null;
  let stepsHtml = binSteps.map(s => `<span class="search-step${s.hit ? ' hit' : ' miss'}">lo=${s.lo} mid=${s.mid} hi=${s.hi}</span>`).join(' → ');

  document.getElementById('ss-result').innerHTML = `
    <div class="two-col tc-50" style="gap:16px;margin-bottom:16px;">
      <div class="result-box"><strong style="color:var(--green);">Fast Search Result</strong>
        <div class="rr" style="margin-top:8px;">Found: <span>${found ? found.name + ' at ' + found.zone : 'Not found'}</span></div>
        <div class="rr">Steps taken: <span>${binComps} of ${SERVICES.length}</span></div>
        <div style="margin-top:6px;font-size:11px;">Search path: ${stepsHtml}</div>
      </div>
      <div class="result-box"><strong style="color:var(--muted);">Standard Search Result</strong>
        <div class="rr" style="margin-top:8px;">Found: <span>${found ? found.name + ' at ' + found.zone : 'Not found'}</span></div>
        <div class="rr">Steps taken: <span>${linComps} of ${SERVICES.length}</span></div>
        <div style="margin-top:6px;"><div style="background:#e9e6e0;height:8px;border-radius:4px;overflow:hidden;"><div style="background:var(--green);width:${linComps / SERVICES.length * 100}%;height:100%;"></div></div><span style="font-size:10px;color:var(--muted);">Scanned ${Math.round(linComps / SERVICES.length * 100)}% of list</span></div>
      </div>
    </div>
    <div class="callout">Fast search found the result in <strong>${binComps}</strong> steps. Standard search needed <strong>${linComps}</strong> steps. Fast search is <strong>${(linComps / Math.max(binComps, 1)).toFixed(1)}x faster</strong> on this sorted list of ${SERVICES.length} services.</div>`;
  document.querySelectorAll('#ss-table tbody tr').forEach(r => r.style.background = '');
  if (binFound >= 0) document.getElementById('svc-' + binFound).style.background = 'var(--green-bg)';
  else if (linFound >= 0) document.getElementById('svc-' + linFound).style.background = 'var(--green-bg)';
}

// ── P05: BFS Zone Info ───────────────────────────────────────────
const ZONE_SERVICES = {
  0: ['ATM', 'Pharmacy'], 1: ['Bank', 'Grocery Store'], 2: ['Bus Stop', 'Metro Station'],
  3: ['Fire Station', 'Power Substation'], 4: ['Park', 'Community Center'],
  5: ['Market', 'Library'], 6: ['Health Clinic', 'Water Plant'], 7: ['Post Office', 'Clinic'],
  8: ['Police Station', 'University'], 9: ['Hospital', 'Traffic Control'],
  10: ['School', 'Clinic'], 11: ['Emergency Shelter', 'Waste Mgmt'], 12: ['Sports Complex', 'Gym']
};

async function runBFS() {
  const start = +document.getElementById('bfs-src').value;
  const maxHops = +document.getElementById('bfs-hops').value;
  showLoading('bfs-result');
  try {
    const res = await api('/algo/bfs', 'POST', { source: start, max_hops: maxHops });
    throw new Error("Fallback required for rendering");
  } catch (e) {
    const start = +document.getElementById('bfs-src').value;
    const maxHops = +document.getElementById('bfs-hops').value;
    const adj = buildAdj();
    const dist = Array(N).fill(Infinity);
    dist[start] = 0;
    const queue = [start];
    while (queue.length) { const u = queue.shift(); if (dist[u] >= maxHops) continue; adj[u].forEach(([v]) => { if (dist[v] === Infinity) { dist[v] = dist[u] + 1; queue.push(v); } }); }
    const hopMap = {};
    dist.forEach((d, i) => { if (d > 0 && d <= maxHops) hopMap[i] = d; });
    hopMap[start] = 0;
    drawMap('bfs-map', [], [start], hopMap);
    const hopsArr = {};
    for (let h = 1; h <= maxHops; h++) hopsArr[h] = [];
    dist.forEach((d, i) => { if (d > 0 && d <= maxHops) hopsArr[d].push(i); });
    const reachable = Object.values(hopsArr).flat().length;
    let html = '<div style="margin-bottom:14px;">';
    for (let h = 1; h <= maxHops; h++) {
      const nodes = hopsArr[h];
      html += `<div style="margin-bottom:10px;padding:10px;border:1px solid var(--border);border-radius:3px;"><strong style="font-size:12px;color:var(--green);">${h} stop${h > 1 ? 's' : ''} away</strong> — ${nodes.length} zone${nodes.length !== 1 ? 's' : ''}<br/><span style="font-size:12px;color:var(--muted);">${nodes.map(i => NODES[i].area).join(', ') || 'None'}</span></div>`;
    }
    html += '</div><div style="margin-bottom:14px;"><strong style="font-size:13px;">Services in Reachable Zones:</strong></div>';
    const allReachable = [start, ...Object.values(hopsArr).flat()];
    allReachable.forEach(i => {
      const svcs = ZONE_SERVICES[i] || [];
      if (svcs.length) html += `<div style="font-size:12px;margin-bottom:4px;"><span style="color:var(--green);font-weight:600;">${NODES[i].area}</span>: ${svcs.join(', ')}</div>`;
    });
    html += `<div style="margin-top:16px;padding:12px;background:var(--green-bg);border-radius:3px;font-size:14px;font-weight:600;color:var(--green);">Connectivity Score: ${reachable + 1}/${N} zones reachable within ${maxHops} stop${maxHops > 1 ? 's' : ''}</div>`;
    document.getElementById('bfs-result').innerHTML = html;
  }
}






function renderCityMap(nodes, edges, svgId) {
  const svg = document.getElementById(svgId);
  if (!svg || !nodes || !edges) return;

  const W = svg.clientWidth || svg.getAttribute('width') || 620;
  const H = svg.clientHeight || svg.getAttribute('height') || 520;
  const PAD = 70;

  // Coordinate range matches admin dashboard (x:100–520, y:80–380)
  const MIN_X = 100, MAX_X = 520;
  const MIN_Y = 80, MAX_Y = 380;

  function sx(x) { return PAD + ((x - MIN_X) / (MAX_X - MIN_X)) * (W - 2 * PAD); }
  function sy(y) { return PAD + ((y - MIN_Y) / (MAX_Y - MIN_Y)) * (H - 2 * PAD); }

  if (!window.nodePositions) window.nodePositions = {};
  window.nodePositions[svgId] = {};
  nodes.forEach(n => {
    window.nodePositions[svgId][n.id] = { x: sx(n.x), y: sy(n.y), label: n.label };
  });

  svg.innerHTML = '';

  for (let gx = 0; gx < W; gx += 35) {
    for (let gy = 0; gy < H; gy += 35) {
      const dot = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      dot.setAttribute('cx', gx);
      dot.setAttribute('cy', gy);
      dot.setAttribute('r', '1.2');
      dot.setAttribute('fill', '#D0D8E4');
      dot.setAttribute('opacity', '0.5');
      svg.appendChild(dot);
    }
  }

  edges.forEach(edge => {
    const uNode = edge[0] !== undefined ? edge[0] : edge.node_u;
    const vNode = edge[1] !== undefined ? edge[1] : edge.node_v;
    const weight = edge[2] !== undefined ? edge[2] : edge.weight;
    const u = window.nodePositions[svgId][uNode];
    const v = window.nodePositions[svgId][vNode];
    if (!u || !v) return;

    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    line.setAttribute('x1', u.x); line.setAttribute('y1', u.y);
    line.setAttribute('x2', v.x); line.setAttribute('y2', v.y);
    line.setAttribute('stroke', edge.is_closed ? '#DC2626' : '#9DB8D2');
    line.setAttribute('stroke-width', edge.is_closed ? '2.5' : '2');
    line.setAttribute('stroke-dasharray', edge.is_closed ? '6,4' : 'none');
    line.setAttribute('id', `${svgId}-edge-${uNode}-${vNode}`);
    line.setAttribute('data-u', uNode);
    line.setAttribute('data-v', vNode);
    line.setAttribute('class', 'map-edge');
    svg.appendChild(line);

    const mx = (u.x + v.x) / 2;
    const my = (u.y + v.y) / 2;

    const bg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
    bg.setAttribute('x', mx - 10); bg.setAttribute('y', my - 9);
    bg.setAttribute('width', 20); bg.setAttribute('height', 16);
    bg.setAttribute('fill', 'white');
    bg.setAttribute('stroke', '#C8D4E0');
    bg.setAttribute('rx', '3');
    svg.appendChild(bg);

    const wt = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    wt.setAttribute('x', mx); wt.setAttribute('y', my + 4);
    wt.setAttribute('text-anchor', 'middle');
    wt.setAttribute('font-size', '10');
    wt.setAttribute('fill', '#445566');
    wt.setAttribute('font-weight', '600');
    wt.setAttribute('font-family', 'Arial');
    wt.textContent = weight;
    svg.appendChild(wt);
  });

  nodes.forEach(node => {
    const pos = window.nodePositions[svgId][node.id];
    if (!pos) return;

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', pos.x); circle.setAttribute('cy', pos.y);
    circle.setAttribute('r', '14');
    circle.setAttribute('fill', 'white');
    circle.setAttribute('stroke', '#2A5C9A');
    circle.setAttribute('stroke-width', '2.5');
    circle.setAttribute('id', `${svgId}-node-${node.id}`);
    circle.setAttribute('data-node-id', node.id);
    circle.setAttribute('class', 'map-node');
    circle.style.cursor = 'pointer';
    svg.appendChild(circle);

    const lbl = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    lbl.setAttribute('x', pos.x); lbl.setAttribute('y', pos.y + 28);
    lbl.setAttribute('text-anchor', 'middle');
    lbl.setAttribute('font-size', '11');
    lbl.setAttribute('fill', '#334155');
    lbl.setAttribute('font-family', 'Arial, sans-serif');
    lbl.setAttribute('font-weight', '500');
    lbl.textContent = pos.label;
    svg.appendChild(lbl);
  });
}

function animatePath(pathArray, svgId) {
  const svg = document.getElementById(svgId);
  if (!svg) return;

  svg.querySelectorAll('.map-edge').forEach(l => {
    l.setAttribute('stroke', '#9DB8D2');
    l.setAttribute('stroke-width', '2');
    l.classList.remove('path-edge');
  });
  svg.querySelectorAll('.map-node').forEach(c => {
    c.setAttribute('fill', 'white');
    c.setAttribute('stroke', '#2A5C9A');
    c.setAttribute('stroke-width', '2.5');
    c.setAttribute('r', '14');
  });
  svg.querySelectorAll('.path-label').forEach(el => el.remove());

  if (!pathArray || pathArray.length < 2) return;

  pathArray.forEach((nodeId, i) => {
    setTimeout(() => {
      if (i < pathArray.length - 1) {
        const u = pathArray[i];
        const v = pathArray[i + 1];
        const edge = document.getElementById(`${svgId}-edge-${u}-${v}`)
          || document.getElementById(`${svgId}-edge-${v}-${u}`);
        if (edge) {
          edge.setAttribute('stroke', '#003366');
          edge.setAttribute('stroke-width', '4');
          edge.classList.add('path-edge');
        }
      }
    }, i * 250);
  });

  setTimeout(() => {
    const addLabel = (nodeId, letter, color, fillColor) => {
      const circle = document.getElementById(`${svgId}-node-${nodeId}`);
      if (!circle || !svg) return;
      circle.setAttribute('fill', fillColor);
      circle.setAttribute('stroke', color);
      circle.setAttribute('stroke-width', '3');

      const cx = parseFloat(circle.getAttribute('cx'));
      const cy = parseFloat(circle.getAttribute('cy'));
      const t = document.createElementNS('http://www.w3.org/2000/svg', 'text');
      t.setAttribute('x', cx); t.setAttribute('y', cy + 5);
      t.setAttribute('text-anchor', 'middle');
      t.setAttribute('fill', color);
      t.setAttribute('font-weight', 'bold');
      t.setAttribute('font-size', '13');
      t.setAttribute('font-family', 'Arial');
      t.setAttribute('class', 'path-label');
      t.textContent = letter;
      svg.appendChild(t);
    };
    addLabel(pathArray[0], 'A', '#2E7D32', '#E8F5E9');
    addLabel(pathArray[pathArray.length - 1], 'B', '#C62828', '#FFEBEE');
  }, pathArray.length * 250);
}


async function loadGraphData() {
  try {
    const [nodesRes, edgesRes] = await Promise.all([
      api('/graph/nodes'),
      api('/graph/edges')
    ]);
    window.graphNodes = nodesRes.data;
    window.graphEdges = edgesRes.data.map(e => [e.node_u, e.node_v, e.weight]);
  } catch (e) {
    console.log('Using fallback graph data');
    window.graphNodes = [
      { "id": 0, "label": "Central Junction", "x": 310, "y": 260, "zone": "Central" },
      { "id": 1, "label": "North Gate", "x": 310, "y": 80, "zone": "North" },
      { "id": 2, "label": "City Hall", "x": 160, "y": 160, "zone": "West" },
      { "id": 3, "label": "Market Square", "x": 460, "y": 160, "zone": "East" },
      { "id": 4, "label": "University", "x": 100, "y": 260, "zone": "West" },
      { "id": 5, "label": "Downtown", "x": 460, "y": 260, "zone": "East" },
      { "id": 6, "label": "Old Town", "x": 160, "y": 360, "zone": "South-West" },
      { "id": 7, "label": "River Road", "x": 310, "y": 380, "zone": "South" },
      { "id": 8, "label": "East Market", "x": 460, "y": 360, "zone": "South-East" },
      { "id": 9, "label": "Medical Hub", "x": 220, "y": 180, "zone": "Central" },
      { "id": 10, "label": "Tech District", "x": 400, "y": 180, "zone": "East" },
      { "id": 11, "label": "Industrial Zone", "x": 100, "y": 380, "zone": "South-West" },
      { "id": 12, "label": "Sports Complex", "x": 520, "y": 380, "zone": "South-East" }
    ];
    window.graphEdges = [
      [0, 1, 3], [0, 4, 4], [1, 2, 5], [1, 4, 2], [1, 9, 6], [2, 3, 4], [2, 9, 3],
      [3, 5, 5], [3, 10, 7], [4, 6, 8], [4, 11, 4], [5, 9, 2], [5, 10, 4], [6, 7, 5],
      [6, 11, 3], [7, 8, 4], [7, 10, 3], [8, 12, 5], [9, 10, 1]
    ];
  }
  NODES = window.graphNodes;
  EDGES = window.graphEdges;
  N = NODES.length;

  // Populate Dropdowns
  const src = document.getElementById('d-src'), dst = document.getElementById('d-dst');
  if (src && dst) {
    src.innerHTML = ''; dst.innerHTML = '';
    NODES.forEach((n, i) => {
      const lbl = n.label || n.l || n.area;
      src.add(new Option(lbl, i));
      dst.add(new Option(lbl, i));
    });
    dst.selectedIndex = N - 1;
  }

  const loc = document.getElementById('fc-loc');
  if (loc) { loc.innerHTML = ''; NODES.forEach(n => loc.add(new Option(n.label || n.l || n.area, n.label || n.l || n.area))); }
  const bs = document.getElementById('bfs-src');
  if (bs) { bs.innerHTML = ''; NODES.forEach((n, i) => bs.add(new Option(n.label || n.l || n.area, i))); }

  // Render both maps with the correct id-based structure that animatePath() needs
  renderCityMap(window.graphNodes, window.graphEdges, 'route-map');
  renderCityMap(window.graphNodes, window.graphEdges, 'bfs-map');
}
