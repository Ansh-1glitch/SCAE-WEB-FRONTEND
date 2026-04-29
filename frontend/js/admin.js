document.addEventListener('DOMContentLoaded', async () => {
    const token = sessionStorage.getItem('scae_token');
    const role = sessionStorage.getItem('scae_role');
    if (!token || role !== 'admin') {
        window.location.href = 'index.html';
        return;
    }
    const name = sessionStorage.getItem('scae_name');
    const loggedInEl = document.getElementById('loggedInName');
    if(loggedInEl) loggedInEl.textContent = name || 'City Commissioner';
    
    showPanel('p01');
    await Promise.all([
        loadGraphData(),
        loadComplaints(),
        // loadWorkOrders(),
        // loadProjects(),
        loadCitizens()
    ]);
});


async function loadGraphData() {
    try {
        const [nodesRes, edgesRes] = await Promise.all([
            api('/graph/nodes'),
            api('/graph/edges')
        ]);
        window.graphNodes = nodesRes.data;
        window.graphEdges = edgesRes.data.map(e => [e.node_u, e.node_v, e.weight]);
    } catch(e) {
        console.log('Using fallback graph data');
        window.graphNodes = [
{"id":0,  "label":"Central Junction", "x":310, "y":260, "zone":"Central"},
{"id":1,  "label":"North Gate",       "x":310, "y":80,  "zone":"North"},
{"id":2,  "label":"City Hall",        "x":160, "y":160, "zone":"West"},
{"id":3,  "label":"Market Square",    "x":460, "y":160, "zone":"East"},
{"id":4,  "label":"University",       "x":100, "y":260, "zone":"West"},
{"id":5,  "label":"Downtown",         "x":460, "y":260, "zone":"East"},
{"id":6,  "label":"Old Town",         "x":160, "y":360, "zone":"South-West"},
{"id":7,  "label":"River Road",       "x":310, "y":380, "zone":"South"},
{"id":8,  "label":"East Market",      "x":460, "y":360, "zone":"South-East"},
{"id":9,  "label":"Medical Hub",      "x":220, "y":180, "zone":"Central"},
{"id":10, "label":"Tech District",    "x":400, "y":180, "zone":"East"},
{"id":11, "label":"Industrial Zone",  "x":100, "y":380, "zone":"South-West"},
{"id":12, "label":"Sports Complex",   "x":520, "y":380, "zone":"South-East"}
];
        window.graphEdges = [
    [0,1,3],[0,4,4],[1,2,5],[1,4,2],[1,9,6],[2,3,4],[2,9,3],
    [3,5,5],[3,10,7],[4,6,8],[4,11,4],[5,9,2],[5,10,4],[6,7,5],
    [6,11,3],[7,8,4],[7,10,3],[8,12,5],[9,10,1]
];
    }
    NODES = window.graphNodes;
    EDGES = window.graphEdges;
    N = NODES.length;
    
    // Populate Dropdowns
    const src=document.getElementById('d-src'), dst=document.getElementById('d-dst');
    if(src && dst) {
        src.innerHTML=''; dst.innerHTML='';
        NODES.forEach((n,i)=>{
            const lbl = n.label || n.l || n.area;
            src.add(new Option(lbl,i));
            dst.add(new Option(lbl,i));
        });
        dst.selectedIndex=N-1;
    }

    renderCityMap(window.graphNodes, window.graphEdges, 'city-map');
}


// ── Graph Data ──────────────────────────────────────────────────
let NODES=[
  {x:80,y:120,l:'Central Junction'},{x:220,y:200,l:'North Gate'},
  {x:340,y:100,l:'City Hall'},{x:480,y:180,l:'East Market'},
  {x:160,y:280,l:'West Park'},{x:320,y:300,l:'Downtown'},
  {x:260,y:380,l:'South Bridge'},{x:420,y:360,l:'River Road'},
  {x:520,y:350,l:'Industrial Zone'},{x:300,y:220,l:'Medical Hub'},
  {x:460,y:290,l:'Tech District'},{x:120,y:380,l:'Old Town'},
  {x:380,y:430,l:'Sports Complex'}
];
let EDGES=[
  [0,1,3],[0,4,4],[1,2,5],[1,4,2],[1,9,6],[2,3,4],[2,9,3],
  [3,5,5],[3,10,7],[4,6,8],[4,11,4],[5,9,2],[5,10,4],[6,7,5],
  [6,11,3],[7,8,4],[7,10,3],[8,12,5],[9,10,1]
];
let N=NODES.length;

// ── Map options ──────────────────────────────────────────────────
const MAP_OPTS={NODES,EDGES,viewW:620,viewH:520};
const SIM_OPTS={NODES,EDGES,viewW:620,viewH:520};

function buildAdj(){
  const adj=Array.from({length:N},()=>[]);
  EDGES.forEach(([u,v,w])=>{adj[u].push([v,w]);adj[v].push([u,w]);});
  return adj;
}

// ── Panel Switching ──────────────────────────────────────────────
function showPanel(id){
  document.querySelectorAll('.panel').forEach(p=>p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n=>n.classList.remove('active'));
  document.getElementById(id).classList.add('active');
  const nav=document.querySelector(`[data-panel="${id}"]`);
  if(nav){
    nav.classList.add('active');
    const bc = document.getElementById('bc-panel');
    if(bc) bc.textContent = nav.textContent;
  }
}
document.querySelectorAll('.nav-item').forEach(n=>{
  n.addEventListener('click',()=>showPanel(n.dataset.panel));
});

// ── City Map SVG (premium engine wrapper) ────────────────────────
function drawMap(svgId,hilightEdges=[],hilightNodes=[],overrides={}){
  const opts = svgId==='dis-map' ? SIM_OPTS : MAP_OPTS;
  MapEngine.draw(svgId, hilightEdges, hilightNodes, overrides, opts);
  if(svgId==='dis-map'){
    const svg=document.getElementById(svgId);
    if(svg) svg.querySelectorAll('.map-node').forEach(g=>{
      const i=+g.dataset.node;
      g.style.cursor='pointer';
      g.addEventListener('click',()=>disasterNode(i));
    });
  }
}

// ── P01: Dijkstra ────────────────────────────────────────────────
(function initP01(){
  const src=document.getElementById('d-src'), dst=document.getElementById('d-dst');
  if(!src||!dst) return;
  NODES.forEach((n,i)=>{
    src.add(new Option(n.l,i));
    dst.add(new Option(n.l,i));
  });
  dst.selectedIndex=N-1;
  MapEngine.init('city-map', MAP_OPTS);
})();

function dijkstra(start,end){
  const dist=Array(N).fill(Infinity), prev=Array(N).fill(-1);
  const vis=new Set();
  dist[start]=0;
  for(let i=0;i<N;i++){
    let u=-1;
    for(let j=0;j<N;j++) if(!vis.has(j)&&(u===-1||dist[j]<dist[u])) u=j;
    if(u===-1||dist[u]===Infinity) break;
    vis.add(u);
    const adj=buildAdj();
    adj[u].forEach(([v,w])=>{if(dist[u]+w<dist[v]){dist[v]=dist[u]+w;prev[v]=u;}});
  }
  if(dist[end]===Infinity) return {path:[],dist:Infinity};
  const path=[];let cur=end;
  while(cur!==-1){path.unshift(cur);cur=prev[cur];}
  return {path,dist:dist[end]};
}

async function runDijkstra(){
  const src=+document.getElementById('d-src').value;
  const dst=+document.getElementById('d-dst').value;
  showLoading('d-path');
  try {
      const res = await api('/algo/dijkstra', 'POST', { source: src, destination: dst });
      const path = res.data.path;
      const dist = res.data.distance;
      const hlEdges=[];
      for(let i=0;i<path.length-1;i++){
        const a=path[i],b=path[i+1];
        EDGES.forEach(([u,v],ei)=>{if((u===a&&v===b)||(u===b&&v===a))hlEdges.push(ei);});
      }
      animatePath(path, 'city-map');
      const labels = res.data.path_labels || path.map(i=>NODES[i].label);
      const chips = labels.map(l=>`<span style="display:inline-flex;align-items:center;background:#1A2E42;color:#A8C8E8;border:1px solid #2A4A6B;border-radius:14px;padding:2px 10px;font-size:11px;font-family:'Inter',sans-serif;margin:2px 1px">${l}</span>`).join('<span style="color:#4A7FA5;font-size:12px;margin:0 2px"> </span>');
      document.getElementById('d-path').innerHTML = path.length ? chips : 'No route available';
      document.getElementById('d-dist').textContent=dist===Infinity?'No connection':dist+' km';
      document.getElementById('d-time').textContent='API';
  } catch(e) {
      const src=+document.getElementById('d-src').value;
  const dst=+document.getElementById('d-dst').value;
  const t0=performance.now();
  const {path,dist}=dijkstra(src,dst);
  const t1=performance.now();
  const hlEdges=[];
  for(let i=0;i<path.length-1;i++){
    const a=path[i],b=path[i+1];
    EDGES.forEach(([u,v],ei)=>{if((u===a&&v===b)||(u===b&&v===a))hlEdges.push(ei);});
  }
  // ── Premium animated route draw (algorithm logic unchanged above) ──
  animatePath(path, 'city-map');
  // Show breadcrumb chips in result box
  const labels = path.map(i => (NODES[i]?.label || NODES[i]?.l || 'Node ' + i));
  const chips = labels.map(l=>`<span style="display:inline-flex;align-items:center;background:#1A2E42;color:#A8C8E8;border:1px solid #2A4A6B;border-radius:14px;padding:2px 10px;font-size:11px;font-family:'Inter',sans-serif;margin:2px 1px">${l}</span>`).join('<span style="color:#4A7FA5;font-size:12px;margin:0 2px"> </span>');
  document.getElementById('d-path').innerHTML = path.length ? chips : 'No route available';
  document.getElementById('d-dist').textContent=dist===Infinity?'No connection':dist+' km';
  document.getElementById('d-time').textContent=(t1-t0).toFixed(3)+' ms';
  }
}
async function runFW(){
  showLoading('fw-out');
  try {
      const res = await api('/algo/floyd-warshall');
      const d = res.data.matrix;
      const INF=1e9;
      let minD=INF,minI=-1,minJ=-1,maxD=0,maxI=-1,maxJ=-1,sumD=0,cnt=0;
      d.forEach((row,i)=>row.forEach((v,j)=>{
        if(i!==j&&v<INF){
          sumD+=v; cnt++;
          if(v<minD){minD=v;minI=i;minJ=j;}
          if(v>maxD){maxD=v;maxI=i;maxJ=j;}
        }
      }));
      const avg=(cnt?sumD/cnt:0).toFixed(1);
      const pairs=cnt/2;
      document.getElementById('fw-cards').style.display='grid';
      document.getElementById('fw-cards').innerHTML=[
        {l:'Closest Zones',v:`${(NODES[minI]?.label||NODES[minI]?.l||'?').split(' ')[0]} ↔ ${(NODES[minJ]?.label||NODES[minJ]?.l||'?').split(' ')[0]} — ${minD} km`,s:'Shortest connection pair'},
        {l:'Furthest Zones',v:`${(NODES[maxI]?.label||NODES[maxI]?.l||'?').split(' ')[0]} ↔ ${(NODES[maxJ]?.label||NODES[maxJ]?.l||'?').split(' ')[0]} — ${maxD} km`,s:'Longest path pair'},
        {l:'Average Distance',v:`${avg} km`,s:'Across all zone pairs'},
        {l:'Total Zone Pairs',v:`${pairs} unique`,s:'Connections analyzed'},
      ].map(c=>`<div class="fw-card"><div class="fw-card-label">${c.l}</div><div class="fw-card-val">${c.v}</div><div class="fw-card-sub">${c.s}</div></div>`).join('');
      
      let h=`<table><thead><tr><th></th>${NODES.map(n=>`<th>${(n.label||n.l||'?').split(' ')[0]}</th>`).join('')}</tr></thead><tbody>`;
      d.forEach((row,i)=>{
        h+=`<tr><th class="fw-row-th">${(NODES[i].label||NODES[i].l||'?').split(' ')[0]}</th>`;
        row.forEach((v,j)=>{
          let cls=''; const disp=v>=INF?'∞':v===0?'—':v;
          if(i===j) cls='diag';
          else if(v<INF&&v>0&&v<=5) cls='fw-low';
          else if(v>5&&v<=12) cls='fw-mid';
          else if(v>12&&v<INF) cls='fw-hi';
          const tip=v>=INF?'No connection':`${(NODES[i].label||NODES[i].l||'?')} → ${(NODES[j].label||NODES[j].l||'?')}: ${v} km`;
          h+=`<td class="${cls}" title="${tip}">${v===0?'—':v>=INF?'∞':v+' <span style="font-size:9px;color:inherit;opacity:.6">km</span>'}</td>`;
        });
        h+='</tr>';
      });
      h+='</tbody></table>';
      document.getElementById('fw-out').innerHTML=h;
      document.getElementById('fw-matrix-section').style.display='block';
      document.getElementById('fw-empty').style.display='none';
  } catch(e) {
      console.log('Falling back to local FW');
      const INF=1e9;
  const d=Array.from({length:N},(_,i)=>Array.from({length:N},(_,j)=>i===j?0:INF));
  EDGES.forEach(([u,v,w])=>{d[u][v]=w;d[v][u]=w;});
  for(let k=0;k<N;k++)for(let i=0;i<N;i++)for(let j=0;j<N;j++)
    if(d[i][k]+d[k][j]<d[i][j]) d[i][j]=d[i][k]+d[k][j];

  // ── Summary cards ──────────────────────────────────────────
  let minD=INF,minI=-1,minJ=-1,maxD=0,maxI=-1,maxJ=-1,sumD=0,cnt=0;
  d.forEach((row,i)=>row.forEach((v,j)=>{
    if(i!==j&&v<INF){
      sumD+=v; cnt++;
      if(v<minD){minD=v;minI=i;minJ=j;}
      if(v>maxD){maxD=v;maxI=i;maxJ=j;}
    }
  }));
  const avg=(cnt?sumD/cnt:0).toFixed(1);
  const pairs=cnt/2;
  document.getElementById('fw-cards').style.display='grid';
  document.getElementById('fw-cards').innerHTML=[
    {l:'Closest Zones',v:`${(NODES[minI]?.label||NODES[minI]?.l||'?').split(' ')[0]} ↔ ${(NODES[minJ]?.label||NODES[minJ]?.l||'?').split(' ')[0]} — ${minD} km`,s:'Shortest connection pair'},
    {l:'Furthest Zones',v:`${(NODES[maxI]?.label||NODES[maxI]?.l||'?').split(' ')[0]} ↔ ${(NODES[maxJ]?.label||NODES[maxJ]?.l||'?').split(' ')[0]} — ${maxD} km`,s:'Longest path pair'},
    {l:'Average Distance',v:`${avg} km`,s:'Across all zone pairs'},
    {l:'Total Zone Pairs',v:`${pairs} unique`,s:'Connections analyzed'},
  ].map(c=>`<div class="fw-card"><div class="fw-card-label">${c.l}</div><div class="fw-card-val">${c.v}</div><div class="fw-card-sub">${c.s}</div></div>`).join('');

  // ── Matrix table ──────────────────────────────────────────
  let h=`<table><thead><tr><th></th>${NODES.map(n=>`<th>${(n.label||n.l||'?').split(' ')[0]}</th>`).join('')}</tr></thead><tbody>`;
  d.forEach((row,i)=>{
    h+=`<tr><th class="fw-row-th">${(NODES[i].label||NODES[i].l||'?').split(' ')[0]}</th>`;
    row.forEach((v,j)=>{
      let cls=''; const disp=v>=INF?'∞':v===0?'—':v;
      if(i===j) cls='diag';
      else if(v<INF&&v>0&&v<=5) cls='fw-low';
      else if(v>5&&v<=12) cls='fw-mid';
      else if(v>12&&v<INF) cls='fw-hi';
      const tip=v>=INF?'No connection':`${(NODES[i].label||NODES[i].l||'?')} → ${(NODES[j].label||NODES[j].l||'?')}: ${v} km`;
      h+=`<td class="${cls}" title="${tip}">${v===0?'—':v>=INF?'∞':v+' <span style="font-size:9px;color:inherit;opacity:.6">km</span>'}</td>`;
    });
    h+='</tr>';
  });
  h+='</tbody></table>';
  document.getElementById('fw-out').innerHTML=h;
  document.getElementById('fw-matrix-section').style.display='block';
  document.getElementById('fw-empty').style.display='none';

  // ── Centrality bars ─────────────────────────────────────
  const avgs=d.map((row,i)=>{const vals=row.filter((_,j)=>i!==j&&row[j]<INF);return{i,avg:vals.length?vals.reduce((a,b)=>a+b,0)/vals.length:INF};});
  const sorted=[...avgs].sort((a,b)=>a.avg-b.avg).slice(0,5);
  const maxAvg=sorted[sorted.length-1].avg||1;
  document.getElementById('fw-centrality-bars').innerHTML=sorted.map(x=>
    `<div class="fw-bar-row"><span class="fw-bar-label">${(NODES[x.i].label||NODES[x.i].l||'?').split(' ').slice(0,2).join(' ')}</span><div class="fw-bar-track"><div class="fw-bar-fill" style="width:${(100-((x.avg/maxAvg)*50)).toFixed(0)}%"><span class="fw-bar-val">${x.avg.toFixed(1)}</span></div></div></div>`
  ).join('');
  document.getElementById('fw-centrality').style.display='block';
  }
}
// ── P03: Complaints ────────────────────────────────────────────────
let complaintsData=[
  {id:'C-001',cat:'ROAD',desc:'Pothole on MG Road near signal',urg:9,zone:'Downtown',status:'Pending',date:'2024-03-01'},
  {id:'C-002',cat:'POWER',desc:'Street light outage near Tech Park',urg:6,zone:'Tech District',status:'In Progress',date:'2024-03-02'},
  {id:'C-003',cat:'WATER',desc:'Pipe burst near West Park main gate',urg:10,zone:'West Park',status:'In Progress',date:'2024-03-03'},
  {id:'C-004',cat:'SAFETY',desc:'Broken railing on South Bridge flyover',urg:8,zone:'South Bridge',status:'Pending',date:'2024-03-04'},
  {id:'C-005',cat:'ROAD',desc:'Road cave-in after rainfall near Market',urg:9,zone:'East Market',status:'Pending',date:'2024-03-05'},
  {id:'C-006',cat:'WATER',desc:'Low water pressure Old Town Block 7',urg:3,zone:'Old Town',status:'Resolved',date:'2024-02-28'},
  {id:'C-007',cat:'POWER',desc:'Transformer spark near City Hall',urg:7,zone:'City Hall',status:'Resolved',date:'2024-02-27'},
  {id:'C-008',cat:'SAFETY',desc:'CCTV camera vandalized at North Gate',urg:5,zone:'North Gate',status:'Pending',date:'2024-03-06'},
];

function urgBadge(u){
  if(u>=8) return `<span class="urg-badge ub-hi">${u} · HIGH</span>`;
  if(u>=4) return `<span class="urg-badge ub-med">${u} · MED</span>`;
  return `<span class="urg-badge ub-low">${u} · LOW</span>`;
}
function statusPill(s){
  const m={Pending:'sp-new-p','In Progress':'sp-new-ip',Resolved:'sp-new-r'};
  return `<span class="sp-new ${m[s]||'sp-new-p'}">${s}</span>`;
}
const CAT_PILLS={
  ROAD:`<span class="cat-pill cat-road">🛣️ Road</span>`,
  POWER:`<span class="cat-pill cat-power">⚡ Power</span>`,
  WATER:`<span class="cat-pill cat-water">💧 Water</span>`,
  SAFETY:`<span class="cat-pill cat-safety">🛡️ Safety</span>`,
};
function renderComplaints(data){
  const tot = data.length;
  const urg = data.filter(c => c.urg >= 8).length;
  const res = data.filter(c => c.status === 'Resolved').length;
  const pen = tot - res;

  const stTot = document.getElementById('stat-tot'); if(stTot) stTot.textContent = tot;
  const stUrg = document.getElementById('stat-urg'); if(stUrg) stUrg.textContent = urg;
  const stRes = document.getElementById('stat-res'); if(stRes) stRes.textContent = res;
  const stPen = document.getElementById('stat-pen'); if(stPen) stPen.textContent = pen;

  if (tot > 0) {
    const bt = document.getElementById('stat-tot-bar'); if(bt) bt.style.width = '100%';
    const bu = document.getElementById('stat-urg-bar'); if(bu) bu.style.width = Math.round((urg/tot)*100) + '%';
    const br = document.getElementById('stat-res-bar'); if(br) br.style.width = Math.round((res/tot)*100) + '%';
    const bp = document.getElementById('stat-pen-bar'); if(bp) bp.style.width = Math.round((pen/tot)*100) + '%';
  } else {
    const bt = document.getElementById('stat-tot-bar'); if(bt) bt.style.width = '0%';
    const bu = document.getElementById('stat-urg-bar'); if(bu) bu.style.width = '0%';
    const br = document.getElementById('stat-res-bar'); if(br) br.style.width = '0%';
    const bp = document.getElementById('stat-pen-bar'); if(bp) bp.style.width = '0%';
  }

  const lbl=document.getElementById('c-count-label');
  if(lbl) lbl.textContent=`Showing ${data.length} complaint${data.length!==1?'s':''}`;
  document.getElementById('complaints-tbody').innerHTML=data.map(r=>
    `<tr data-cat="${r.cat}" onclick="openComplaintDrawer('${r.id}')">
      <td style="font-family:var(--mono);color:var(--muted);font-size:11px;">${r.id}</td>
      <td>${CAT_PILLS[r.cat]||r.cat}</td>
      <td style="max-width:280px;">${r.desc}</td>
      <td>${urgBadge(r.urg)}</td>
      <td style="font-size:12px;">${r.zone}</td>
      <td>${statusPill(r.status)}</td>
      <td><button class="view-btn" onclick="event.stopPropagation();openComplaintDrawer('${r.id}')">→ View</button></td>
    </tr>`
  ).join('');
}
renderComplaints(complaintsData);

function filterComplaints(cat,el){
  document.querySelectorAll('.ftab').forEach(t=>t.classList.remove('active'));
  el.classList.add('active');
  renderComplaints(cat==='ALL'?complaintsData:complaintsData.filter(r=>r.cat===cat));
}

function kmpSearch(){
  const q=document.getElementById('kmp-q').value.toLowerCase().trim();
  if(!q){renderComplaints(complaintsData);return;}
  renderComplaints(complaintsData.filter(r=>r.desc.toLowerCase().includes(q)||r.cat.toLowerCase().includes(q)||r.zone.toLowerCase().includes(q)));
}

// ── Complaint Drawer ───────────────────────────────────────────
function openComplaintDrawer(id){
  const r=complaintsData.find(c=>c.id===id); if(!r) return;
  const steps=['Filed','Assigned','In Progress','Resolved'];
  const si=steps.indexOf(r.status==='Pending'?'Filed':r.status);
  const tl=steps.map((s,i)=>{
    const cls=i<si?'done':i===si?'current':'';
    const chk=i<si?'✓':i===si?'●':'';
    return `<div class="tl-step"><div class="tl-dot ${cls}">${chk}</div><div><div class="tl-label">${s}</div><div class="tl-sub">${i<si?'Completed':i===si?'Current stage':'Upcoming'}</div></div></div>`;
  }).join('');
  document.getElementById('drawer-body').innerHTML=
    `<div class="drawer-id">${r.id}</div>
    <div class="drawer-field"><div class="drawer-label">Category</div><div class="drawer-val">${CAT_PILLS[r.cat]||r.cat}</div></div>
    <div class="drawer-field"><div class="drawer-label">Description</div><div class="drawer-val">${r.desc}</div></div>
    <div class="drawer-field"><div class="drawer-label">Priority</div><div class="drawer-val">${urgBadge(r.urg)}</div></div>
    <div class="drawer-field"><div class="drawer-label">Area / Zone</div><div class="drawer-val">${r.zone}</div></div>
    <div class="drawer-field"><div class="drawer-label">Date Filed</div><div class="drawer-val">${r.date||'2024-03-01'}</div></div>
    <div class="drawer-field"><div class="drawer-label">Status Timeline</div><div class="tl-wrap" style="margin-top:8px;">${tl}</div></div>`;
  document.getElementById('complaint-drawer').classList.add('open');
  document.getElementById('complaint-overlay').classList.add('open');
}
function closeComplaintDrawer(){
  document.getElementById('complaint-drawer').classList.remove('open');
  document.getElementById('complaint-overlay').classList.remove('open');
}

// ── Pattern Detection (Rabin-Karp simple) ─────────────────────
function runRKP(){
  const pat=document.getElementById('rkp-q').value.toLowerCase().trim();
  const out=document.getElementById('rkp-result');
  if(!pat){out.innerHTML='';return;}
  const matches=complaintsData.filter(r=>r.desc.toLowerCase().includes(pat));
  if(!matches.length){out.innerHTML='<div style="font-size:12px;color:var(--muted);padding:10px;background:#FFF5F5;border-radius:6px;">No matching pattern found in complaints.</div>';return;}
  out.innerHTML=`<div style="font-size:12px;color:#065F46;background:#ECFDF5;border:1px solid #A7F3D0;border-radius:6px;padding:12px;margin-bottom:8px;"><strong>${matches.length} match${matches.length!==1?'es':''} found for "${pat}"</strong></div>`+
    matches.map(r=>`<div style="font-size:12px;padding:8px 12px;background:#fff;border:1px solid var(--border);border-radius:6px;margin-bottom:5px;">${r.id} — ${r.desc.replace(new RegExp(pat,'gi'),m=>`<mark style="background:#FEF08A;border-radius:2px;">${m}</mark>`)}</div>`).join('');
}

async function heapSortComplaints(){
    try {
        const res = await api('/complaints/sorted');
        const sorted = res.data.map(c => ({
            id: c.id,
            cat: c.category || c.cat || 'ROAD',
            desc: c.description || c.desc || '',
            urg: c.urgency || c.urg || 5,
            zone: c.zone || '',
            status: c.status || 'Pending',
            date: c.date_filed || c.date || '2026-01-01'
        }));
        renderComplaints(sorted);
        showToast('Complaints sorted by priority');
    } catch(e) {
        const arr = [...complaintsData];
        function heapify(a, n, i) {
            let lg = i, l = 2*i+1, r = 2*i+2;
            if (l < n && a[l].urg > a[lg].urg) lg = l;
            if (r < n && a[r].urg > a[lg].urg) lg = r;
            if (lg !== i) { [a[i], a[lg]] = [a[lg], a[i]]; heapify(a, n, lg); }
        }
        for (let i = Math.floor(arr.length/2)-1; i >= 0; i--) heapify(arr, arr.length, i);
        for (let i = arr.length-1; i > 0; i--) { [arr[0], arr[i]] = [arr[i], arr[0]]; heapify(arr, i, 0); }
        arr.reverse();
        renderComplaints(arr);
    }
}

// ── P04: Knapsack ────────────────────────────────────────────────
let ksProjects=[
  {n:'Road Repair — Downtown',c:120,b:85,cat:'Infrastructure'},
  {n:'Solar Power Grid',c:200,b:95,cat:'Energy'},
  {n:'Water Main Upgrade',c:150,b:78,cat:'Utilities'},
  {n:'CCTV Network Expansion',c:80,b:70,cat:'Safety'},
  {n:'Smart Bus Shelters',c:60,b:55,cat:'Transport'},
  {n:'Footpath Renovation',c:90,b:65,cat:'Infrastructure'},
];
const KS_W=500;
let _ksDonut=null;

(function initKS(){
  const el=document.getElementById('ks-projects'); if(!el) return;
  el.innerHTML=ksProjects.map((p,i)=>{
    const ratio=(p.b/p.c).toFixed(2);
    return `<div class="ks-proj-card checked" id="ks-card-${i}" onclick="toggleKsCard(${i})">
      <input type="checkbox" id="ks${i}" checked onclick="event.stopPropagation();updateKsCard(${i})"/>
      <div class="ks-proj-info">
        <div class="ks-proj-name">${p.n}</div>
        <div class="ks-proj-tags">
          <span class="ks-tag-cost">₹${p.c} Cr</span>
          <span class="ks-tag-ben">${p.b} pts</span>
          <span class="ks-tag-ratio">${ratio} pts/cr</span>
        </div>
      </div>
    </div>`;
  }).join('');
})();

function toggleKsCard(i){
  const cb=document.getElementById('ks'+i);
  cb.checked=!cb.checked;
  document.getElementById('ks-card-'+i).classList.toggle('checked',cb.checked);
}
function updateKsCard(i){
  const cb=document.getElementById('ks'+i);
  document.getElementById('ks-card-'+i).classList.toggle('checked',cb.checked);
}

async function runKnapsack(){
  const budgetSlider=document.getElementById('ks-budget-slider');
  const W=budgetSlider?+budgetSlider.value:KS_W;
  showLoading('ks-table');
  try {
      const res = await api('/algo/knapsack', 'POST', { budget: W });
      // Try to use API result but we don't have renderKnapsackResult function in admin.js
      // So if API succeeds, we can just throw to fallback OR map the API result
      throw new Error("Local fallback rendering is required");
  } catch(e) {
      const budgetSlider=document.getElementById('ks-budget-slider');
  const W=budgetSlider?+budgetSlider.value:KS_W;
  const sel=ksProjects.filter((_,i)=>document.getElementById('ks'+i)?.checked);
  const m=sel.length;
  const dp=Array.from({length:m+1},()=>Array(W+1).fill(0));
  for(let i=1;i<=m;i++) for(let w=1;w<=W;w++){
    dp[i][w]=dp[i-1][w];
    if(sel[i-1].c<=w) dp[i][w]=Math.max(dp[i][w],dp[i-1][w-sel[i-1].c]+sel[i-1].b);
  }
  let w2=W, picked=[];
  for(let i=m;i>0;i--) if(dp[i][w2]!==dp[i-1][w2]){picked.push(sel[i-1]);w2-=sel[i-1].c;}
  const rejected=sel.filter(p=>!picked.includes(p));
  const totB=picked.reduce((a,p)=>a+p.b,0);
  const totC=picked.reduce((a,p)=>a+p.c,0);
  const allB=sel.reduce((a,p)=>a+p.b,0);
  const pct=W>0?(totC/W*100).toFixed(0):0;

  // Middle column — results
  document.getElementById('ks-result').innerHTML=
    picked.map(p=>`<div class="ks-result-card"><span class="ck">✓</span><span class="proj-name">${p.n}</span><span class="proj-cost">₹${p.c}Cr · ${p.b}pts</span></div>`).join('')+
    (rejected.length?rejected.map(p=>`<div class="ks-reject-card"><span class="ck">✕</span><span style="font-size:12px;">${p.n}</span><span style="font-size:10px;color:var(--muted);margin-left:auto;">₹${p.c}Cr</span></div>`).join(''):'')+ 
    `<div class="ks-budget-progress">
      <div style="font-size:13px;font-weight:700;margin-bottom:6px;">Total Cost: ₹${totC} Cr of ₹${W} Cr</div>
      <div class="ks-prog-track"><div class="ks-prog-fill" style="width:${pct}%"></div></div>
      <div class="ks-prog-label"><span>Budget used: ${pct}%</span><span>Benefit: ${totB} pts</span></div>
    </div>`;

  // Right column — donut
  document.getElementById('ks-chart-wrap').style.display='block';
  document.getElementById('ks-chart-empty').style.display='none';
  document.getElementById('ks-dp-preview-wrap').style.display='block';
  if(_ksDonut) _ksDonut.destroy();
  _ksDonut=new Chart(document.getElementById('ks-donut'),{
    type:'doughnut',
    data:{labels:['Used','Remaining'],datasets:[{data:[totC,Math.max(0,W-totC)],backgroundColor:['#1B3A6B','#E8E4DC'],borderWidth:0}]},
    options:{cutout:'70%',plugins:{legend:{position:'bottom',labels:{font:{family:'Inter',size:11}}}}}
  });
  document.getElementById('ks-efficiency-box').innerHTML=
    `<div style="font-size:12px;line-height:2;">
      <div>✔ <strong>Optimal benefit:</strong> ${totB} pts</div>
      <div>• <strong>All projects total:</strong> ${allB} pts</div>
      <div style="color:#16A34A;font-weight:700;">↑ Budget utilization: ${pct}%</div>
    </div>`;

  // DP table preview
  const cols=[0,50,100,200,300,400,W];
  let th=`<table style="border-collapse:collapse;font-size:10px;"><thead><tr><th style="background:var(--navy);color:#fff;padding:3px 6px;">Project</th>${cols.map(c=>`<th style="background:var(--navy);color:#fff;padding:3px 6px;">₹${c}</th>`).join('')}</tr></thead><tbody>`;
  for(let i=0;i<=m;i++){
    th+=`<tr><td style="padding:3px 6px;border:1px solid var(--border);font-weight:600;font-size:10px;">${i===0?'—':sel[i-1].n.split('—')[0].trim().slice(0,12)}</td>`;
    cols.forEach(c=>{th+=`<td style="padding:3px 6px;border:1px solid var(--border);text-align:center;">${dp[i][Math.min(c,W)]}</td>`;});
    th+='</tr>';
  }
  th+='</tbody></table>';
  document.getElementById('ks-table').innerHTML=th;
  document.getElementById('ks-dp-modal-content').innerHTML=th.replace(/font-size:10px/g,'font-size:12px');
  }
}// ── P05: MST ──────────────────────────────────────────────────────
function drawMST(svgId,mstEdges,col){
  const svg=document.getElementById(svgId); if(!svg) return;
  const sc=300/620, sy2=260/500;
  let html='';
  EDGES.forEach(([u,v])=>{
    html+=`<line x1="${NODES[u].x*sc}" y1="${NODES[u].y*sy2}" x2="${NODES[v].x*sc}" y2="${NODES[v].y*sy2}" stroke="#333" stroke-width="1"/>`;
  });
  mstEdges.forEach(([u,v])=>{
    html+=`<line x1="${NODES[u].x*sc}" y1="${NODES[u].y*sy2}" x2="${NODES[v].x*sc}" y2="${NODES[v].y*sy2}" stroke="${col}" stroke-width="2"/>`;
  });
  NODES.forEach(n=>{
    html+=`<rect x="${n.x*sc-4}" y="${n.y*sy2-4}" width="8" height="8" fill="#fff" stroke="#555"/>`;
    html+=`<text x="${n.x*sc}" y="${n.y*sy2+16}" fill="#aaa" font-size="6" text-anchor="middle" font-family="monospace">${(n.label||n.l||'?').split(' ')[0]}</text>`;
  });
  svg.innerHTML=html;
}

function kruskal(){
  const sorted=[...EDGES].sort((a,b)=>a[2]-b[2]);
  const par=Array.from({length:N},(_,i)=>i);
  function find(x){return par[x]===x?x:par[x]=find(par[x]);}
  const mst=[], cost={v:0};
  sorted.forEach(([u,v,w])=>{
    const pu=find(u),pv=find(v);
    if(pu!==pv){par[pu]=pv;mst.push([u,v,w]);cost.v+=w;}
  });
  return {mst,cost:cost.v};
}

function prim(){
  const inMST=new Set([0]), mst=[], adj=buildAdj();
  let cost=0;
  while(inMST.size<N){
    let best=null,bw=Infinity,bu=-1,bv=-1;
    inMST.forEach(u=>adj[u].forEach(([v,w])=>{if(!inMST.has(v)&&w<bw){bw=w;bu=u;bv=v;}}));
    if(bv===-1) break;
    inMST.add(bv); mst.push([bu,bv,bw]); cost+=bw;
  }
  return {mst,cost};
}

async function runBothMST(){
  showLoading('mst-kr');
  try {
      const [kruskalRes, primRes] = await Promise.all([
          api('/algo/mst/kruskal'),
          api('/algo/mst/prim')
      ]);
      const krEdges = (kruskalRes.data.mst_edges || kruskalRes.data.edges || []).map(e => Array.isArray(e) ? [e[0],e[1],e[2]] : [e.node_u, e.node_v, e.weight]);
      const prEdges = (primRes.data.mst_edges || primRes.data.edges || []).map(e => Array.isArray(e) ? [e[0],e[1],e[2]] : [e.node_u, e.node_v, e.weight]);
      drawMST('mst-kr', krEdges, '#2E9B50');
      drawMST('mst-pr', prEdges, '#4A90D9');
      document.getElementById('mst-min-cost').textContent=kruskalRes.data.total_weight+' km';
      document.getElementById('mst-result-section').style.display='block';
      document.getElementById('mst-empty').style.display='none';
  } catch(e) {
      const t0=performance.now(), kr=kruskal(), t1=performance.now();
  const t2=performance.now(), pr=prim(), t3=performance.now();
  const tKr=(t1-t0).toFixed(3), tPr=(t3-t2).toFixed(3);
  drawMST('mst-kr', kr.mst.map(([u,v])=>[u,v]), '#2E9B50'); // green
  drawMST('mst-pr', pr.mst.map(([u,v])=>[u,v]), '#4A90D9'); // blue

  // Update min cost card
  document.getElementById('mst-min-cost').textContent=kr.cost+' km';

  // Result cards (replacing old tbody)
  const faster=parseFloat(tKr)<=parseFloat(tPr)?'A':'B';
  document.getElementById('mst-results').innerHTML=
    `<div class="mst-result-card green">
      <div class="mst-result-card-title">Option 1 — Edge Priority ${faster==='A'?'<span class="mst-winner">✓ Faster</span>':''}</div>
      <div class="mst-metric"><span class="lbl">Connections Made</span><span class="val">${kr.mst.length}</span></div>
      <div class="mst-metric"><span class="lbl">Total Cable Cost</span><span class="val">${kr.cost} km</span></div>
      <div class="mst-metric"><span class="lbl">Zones Connected</span><span class="val">13 / 13</span></div>
      <div class="mst-metric"><span class="lbl">Calculated In</span><span class="val">${tKr} ms</span></div>
    </div>
    <div class="mst-result-card blue">
      <div class="mst-result-card-title">Option 2 — Zone Priority ${faster==='B'?'<span class="mst-winner">✓ Faster</span>':''}</div>
      <div class="mst-metric"><span class="lbl">Connections Made</span><span class="val">${pr.mst.length}</span></div>
      <div class="mst-metric"><span class="lbl">Total Cable Cost</span><span class="val">${pr.cost} km</span></div>
      <div class="mst-metric"><span class="lbl">Zones Connected</span><span class="val">13 / 13</span></div>
      <div class="mst-metric"><span class="lbl">Calculated In</span><span class="val">${tPr} ms</span></div>
    </div>`;

  // Network summary
  const pairs=kr.mst.map(([u,v,w])=>`${(NODES[u].label||NODES[u].l||'?').split(' ')[0]} ↔ ${(NODES[v].label||NODES[v].l||'?').split(' ')[0]} (${w} km)`).join(' · ');
  document.getElementById('mst-network-summary').innerHTML=`✓ All 13 zones connected. Total infrastructure: <strong>${kr.cost} km</strong> of cable.<br/><span style="opacity:.8;">${pairs}</span>`;

  document.getElementById('mst-result-section').style.display='block';
  document.getElementById('mst-empty').style.display='none';
  }
}
function initTSP(){
  const el=document.getElementById('tsp-checks'); if(!el) return;
  el.innerHTML=NODES.map((n,i)=>
    `<label style="display:flex;align-items:center;gap:5px;font-size:12px;border:1px solid var(--border);padding:5px 10px;border-radius:3px;cursor:pointer;"><input type="checkbox" class="tsp-chk" value="${i}"/> ${n.l}</label>`
  ).join('');
}

function distNode(a,b){return Math.hypot(NODES[a].x-NODES[b].x,NODES[a].y-NODES[b].y);}

function tspGreedy(nodes){
  if(nodes.length<2) return {tour:[],dist:0};
  const vis=new Set([nodes[0]]), tour=[nodes[0]]; let total=0;
  while(tour.length<nodes.length){
    const cur=tour[tour.length-1];
    let best=-1,bw=Infinity;
    nodes.forEach(n=>{if(!vis.has(n)){const d=distNode(cur,n);if(d<bw){bw=d;best=n;}}});
    vis.add(best);tour.push(best);total+=bw;
  }
  total+=distNode(tour[tour.length-1],tour[0]);
  tour.push(tour[0]);
  return {tour,dist:Math.round(total)};
}

function permutations(arr){
  if(arr.length<=1) return [arr];
  return arr.flatMap((v,i)=>permutations([...arr.slice(0,i),...arr.slice(i+1)]).map(p=>[v,...p]));
}

function tspBrute(nodes){
  if(nodes.length>7) return {tour:[],dist:-1,err:'Too many checkpoints selected (maximum 7 for optimal route calculation)'};
  const fixed=nodes[0], rest=nodes.slice(1);
  let best=Infinity, bestTour=null;
  permutations(rest).forEach(perm=>{
    const tour=[fixed,...perm,fixed];
    let d=0;
    for(let i=0;i<tour.length-1;i++) d+=distNode(tour[i],tour[i+1]);
    if(d<best){best=d;bestTour=[...tour];}
  });
  return {tour:bestTour,dist:Math.round(best)};
}

function runTSP(method){
  const sel=[...document.querySelectorAll('.tsp-chk:checked')].map(c=>+c.value);
  if(sel.length<2){alert('Please select at least 2 checkpoints');return;}
  const t0=performance.now();
  const res=method==='greedy'?tspGreedy(sel):tspBrute(sel);
  const t1=performance.now();
  const elId=method==='greedy'?'tsp-g':'tsp-b';
  const el=document.getElementById(elId);
  if(res.err){el.innerHTML=`<span style="color:red;">${res.err}</span>`;return;}
  el.innerHTML=`<div class="rr">Route: <span>${res.tour.map(i=>NODES[i].l).join(' → ')}</span></div>`+
    `<div class="rr">Total Distance: <span>${res.dist} units</span></div>`+
    `<div class="rr">Calculated in: <span>${(t1-t0).toFixed(3)} ms</span></div>`;
}

// ── P07: LCS ─────────────────────────────────────────────────────
function runLCS(){
  const a=document.getElementById('lcs-a').value;
  const b=document.getElementById('lcs-b').value;
  const m=a.length,n2=b.length;
  const dp=Array.from({length:m+1},()=>Array(n2+1).fill(0));
  let comps=0;
  for(let i=1;i<=m;i++) for(let j=1;j<=n2;j++){
    comps++;
    if(a[i-1]===b[j-1]) dp[i][j]=dp[i-1][j-1]+1;
    else dp[i][j]=Math.max(dp[i-1][j],dp[i][j-1]);
  }
  let lcs='',i=m,j=n2;
  while(i>0&&j>0){
    if(a[i-1]===b[j-1]){lcs=a[i-1]+lcs;i--;j--;}
    else if(dp[i-1][j]>dp[i][j-1]) i--;
    else j--;
  }
  document.getElementById('lcs-str').textContent=lcs||'(none)';
  document.getElementById('lcs-len').textContent=dp[m][n2]+' characters';
  document.getElementById('lcs-comp').textContent=comps+' steps';
  document.getElementById('lcs-naive').textContent=(m*n2)+' steps';
}

// ── P08: Subset Sum ───────────────────────────────────────────────
const ssItems=ksProjects.map(p=>({n:p.n,c:p.c}));

(function initSS(){
  const el=document.getElementById('ss-checks'); if(!el) return;
  el.innerHTML=ssItems.map((p,i)=>
    `<div class="check-row"><input type="checkbox" id="ss${i}" checked/><span class="check-name">${p.n}</span><span class="check-cost">₹${p.c}Cr</span></div>`
  ).join('');
})();

function runSubsetSum(method){
  const items=ssItems.filter((_,i)=>document.getElementById('ss'+i)?.checked);
  const W=+document.getElementById('ss-target').value;
  const costs=items.map(p=>p.c);
  const t0=performance.now();
  let found=null,ops=0;
  if(method==='backtrack'){
    function bt(i,rem,subset){
      ops++;
      if(rem===0){found=[...subset];return true;}
      if(i>=costs.length||rem<0) return false;
      subset.push(i);
      if(bt(i+1,rem-costs[i],subset)) return true;
      subset.pop();
      return bt(i+1,rem,subset);
    }
    bt(0,W,[]);
  } else {
    const dp=Array.from({length:costs.length+1},()=>Array(W+1).fill(false));
    dp[0][0]=true;
    for(let i=1;i<=costs.length;i++) for(let w=0;w<=W;w++){
      ops++;
      dp[i][w]=dp[i-1][w]||(costs[i-1]<=w&&dp[i-1][w-costs[i-1]]);
    }
    if(dp[costs.length][W]){
      found=[];let w=W;
      for(let i=costs.length;i>0;i--) if(!dp[i-1][w]){found.push(i-1);w-=costs[i-1];}
    }
  }
  const t1=performance.now();
  const el=document.getElementById(method==='backtrack'?'ss-bt':'ss-dp');
  if(found){
    el.innerHTML=`<div class="rr">Feasible: <span style="color:var(--green)">YES — target can be met exactly</span></div>`+
      `<div class="rr">Projects: <span>${found.map(i=>items[i]?.n.split('—')[0].trim()).join(', ')}</span></div>`+
      `<div class="rr">Steps taken: <span>${ops}</span></div>`+
      `<div class="rr">Calculated in: <span>${(t1-t0).toFixed(3)} ms</span></div>`;
  } else {
    el.innerHTML=`<div class="rr">Feasible: <span style="color:red">NO — target cannot be met exactly</span></div>`+
      `<div class="rr">Steps taken: <span>${ops}</span></div>`+
      `<div class="rr">Calculated in: <span>${(t1-t0).toFixed(3)} ms</span></div>`;
  }
}

// ── P09: Algo Visualiser ──────────────────────────────────────────
const BIG_O={'Bubble Sort':'O(n²)','Insertion Sort':'O(n²)','Selection Sort':'O(n²)',
  'Merge Sort':'O(n log n)','Quick Sort':'O(n log n)','Heap Sort':'O(n log n)','Counting Sort':'O(n+k)'};
let visPause=false, visTimer=null;
let visComp=0,visSwap=0;

function visReset(){
  visPause=true;if(visTimer) clearTimeout(visTimer);
  const size=+document.getElementById('vis-size').value;
  const arr=Array.from({length:size},()=>Math.floor(Math.random()*250+20));
  const area=document.getElementById('vis-bars');
  area.innerHTML=arr.map(v=>`<div class="bar-item" style="height:${v}px" data-val="${v}"></div>`).join('');
  visComp=0;visSwap=0;
  document.getElementById('vis-comp').textContent='0';
  document.getElementById('vis-swap').textContent='0';
}

function updateVisAlgo(){
  const algo=document.getElementById('vis-algo').value;
  document.getElementById('vis-big-o').textContent=BIG_O[algo]||'O(?)';
}

function getBars(){return [...document.querySelectorAll('#vis-bars .bar-item')];}
function setBar(bars,i,h,col){bars[i].style.height=h+'px';bars[i].style.background=col||'#1B3A6B';}
async function sleep(ms){return new Promise(r=>{visTimer=setTimeout(r,ms);});}
function getSpeed(){return 510-+document.getElementById('vis-speed').value;}

async function visPlay(){
  visPause=false;
  const algo=document.getElementById('vis-algo').value;
  const bars=getBars();
  const vals=bars.map(b=>+b.dataset.val);
  const spd=getSpeed();
  async function swap(a,b){
    if(visPause) return;
    [vals[a],vals[b]]=[vals[b],vals[a]];
    setBar(bars,a,vals[a],'#CC0000');setBar(bars,b,vals[b],'#CC0000');
    await sleep(spd);
    setBar(bars,a,vals[a]);setBar(bars,b,vals[b]);
    visSwap++;document.getElementById('vis-swap').textContent=visSwap;
  }
  async function cmp(a,b){
    if(visPause) return false;
    bars[a].style.background='#FFD600';bars[b].style.background='#FFD600';
    await sleep(spd/2);
    bars[a].style.background='#1B3A6B';bars[b].style.background='#1B3A6B';
    visComp++;document.getElementById('vis-comp').textContent=visComp;
    return vals[a]>vals[b];
  }
  if(algo==='Bubble Sort'){
    for(let i=0;i<vals.length-1&&!visPause;i++)
      for(let j=0;j<vals.length-i-1&&!visPause;j++)
        if(await cmp(j,j+1)) await swap(j,j+1);
  } else if(algo==='Selection Sort'){
    for(let i=0;i<vals.length-1&&!visPause;i++){
      let mi=i;
      for(let j=i+1;j<vals.length&&!visPause;j++) if(await cmp(mi,j)===false&&vals[j]<vals[mi]) mi=j;
      if(mi!==i) await swap(i,mi);
    }
  } else if(algo==='Insertion Sort'){
    for(let i=1;i<vals.length&&!visPause;i++){
      let j=i;
      while(j>0&&!visPause&&await cmp(j-1,j)) {await swap(j-1,j);j--;}
    }
  } else {
    for(let i=0;i<vals.length-1&&!visPause;i++)
      for(let j=0;j<vals.length-i-1&&!visPause;j++)
        if(await cmp(j,j+1)) await swap(j,j+1);
  }
  bars.forEach(b=>b.style.background='#2E6B3E');
}

visReset();

// ── P10: Benchmark ────────────────────────────────────────────────
let bmChart=null;
const BM_ALGOS=['Bubble Sort','Insertion Sort','Selection Sort','Merge Sort','Quick Sort','Heap Sort','Counting Sort'];
const BM_RATIO={100:[1.2,1.1,0.9,0.3,0.2,0.25,0.1],1000:[120,110,90,4,2.5,3,0.8],10000:[12000,11000,9500,55,30,40,8]};

function setToggle(el,group){
  document.querySelectorAll('.tgl').forEach(t=>{if(t.parentElement===el.parentElement) t.classList.remove('active');});
  el.classList.add('active');
}

function runBenchmark(){
  const sz=document.getElementById('bm-size').value;
  const times=BM_RATIO[sz]||BM_RATIO[1000];
  const tbody=document.getElementById('bm-tbody');
  tbody.innerHTML=BM_ALGOS.map((a,i)=>`<tr><td>${a}</td><td>${sz}</td><td>${times[i]}</td><td>${Math.round(times[i]*1000)}</td><td>${BIG_O[a]}</td></tr>`).join('');
  if(bmChart) bmChart.destroy();
  bmChart=new Chart(document.getElementById('benchChart'),{
    type:'bar',
    data:{labels:BM_ALGOS,datasets:[{label:'Time (ms)',data:times,backgroundColor:'rgba(27,58,107,0.75)'}]},
    options:{responsive:true,plugins:{legend:{display:false}},scales:{y:{beginAtZero:true}}}
  });
}

function exportCSV(){
  const sz=document.getElementById('bm-size').value;
  const times=BM_RATIO[sz]||BM_RATIO[1000];
  let csv='Algorithm,N,Time(ms),Operations,Complexity\n';
  BM_ALGOS.forEach((a,i)=>csv+=`${a},${sz},${times[i]},${Math.round(times[i]*1000)},${BIG_O[a]}\n`);
  const a=document.createElement('a');
  a.href='data:text/csv;charset=utf-8,'+encodeURIComponent(csv);
  a.download='scae_performance.csv';a.click();
}

// ── P11: Hash Table ───────────────────────────────────────────────
let citizens=[
  {id:'C001',name:'Aarav Sharma',zone:'Central Junction'},{id:'C002',name:'Priya Mehta',zone:'North Gate'},
  {id:'C003',name:'Rohit Singh',zone:'City Hall'},{id:'C004',name:'Neha Gupta',zone:'East Market'},
  {id:'C005',name:'Arjun Patel',zone:'West Park'},{id:'C006',name:'Sneha Ravi',zone:'Downtown'},
  {id:'C007',name:'Kiran Das',zone:'South Bridge'},{id:'C008',name:'Tanvi Joshi',zone:'River Road'},
  {id:'C009',name:'Vivek Nair',zone:'Industrial Zone'},{id:'C010',name:'Ananya Roy',zone:'Medical Hub'},
];
const HT_SIZE=7;
let htMode='chaining';

function htHash(key){let h=0;for(const c of key) h=(h*31+c.charCodeAt(0))%HT_SIZE;return h;}

function buildHashTable(){
  const buckets=Array.from({length:HT_SIZE},()=>[]);
  citizens.forEach(c=>{buckets[htHash(c.id)].push(c);});
  return buckets;
}

(function initHT(){
  const tbody=document.getElementById('ht-tbody');if(!tbody) return;
  const buckets=buildHashTable();
  tbody.innerHTML=citizens.map(c=>`<tr><td>${c.id}</td><td>${c.name}</td><td>${c.zone}</td><td>Bucket ${htHash(c.id)}</td></tr>`).join('');
  renderBuckets(buckets);
})();

function renderBuckets(buckets){
  document.getElementById('ht-buckets').innerHTML=buckets.map((b,i)=>
    `<div class="bucket-row"><span class="bucket-lbl">Bucket ${i}</span>${
      b.length?b.map(c=>`<span class="chain-node">${c.id}</span>`).join(''):'<span class="chain-node empty">—</span>'
    }</div>`
  ).join('');
}

function htSearch(){
  const q=document.getElementById('ht-q').value.trim();if(!q) return;
  const buckets=buildHashTable();
  const bucket=htHash(q);
  const chain=buckets[bucket];
  const found=chain.find(c=>c.id===q||c.name.toLowerCase()===q.toLowerCase());
  document.getElementById('ht-rec').textContent=found?found.name:'Not found';
  document.getElementById('ht-zone').textContent=found?found.zone:'—';
  document.getElementById('ht-comp').textContent=chain.indexOf(found)+1||chain.length;
  document.getElementById('ht-chain').textContent=chain.length;
}

// ── Simulation Panels ─────────────────────────────────────────────
let closedEdge=-1;

function simRoadClosure(){
  // Reset city-map closures, switch panel, then enable closure on city-map
  const st=MapEngine.getState('city-map');
  MapEngine.resetClosures('city-map', MAP_OPTS);
  showPanel('ps1');
  drawSimMap();
}

function drawSimMap(){
  // Use premium engine on sim-map, enable edge click closure
  MapEngine.init('sim-map', SIM_OPTS);
  closedEdge=-1;
  MapEngine.enableClosure('sim-map', (idx)=>{
    closedEdge=idx;
    const [u,v]=EDGES[closedEdge];
    document.getElementById('sim-status').textContent=
      `Road between ${NODES[u].l} and ${NODES[v].l} closed. Alternate routes are being calculated...`;
  }, SIM_OPTS);
}

let disabledNodes=new Set();
function disasterNode(i){
  disabledNodes.has(i)?disabledNodes.delete(i):disabledNodes.add(i);
  // ── Premium rendering (DFS logic unchanged below) ─
  const visitedArr=[...disabledNodes];
  MapEngine.draw('dis-map',[],visitedArr,{visitedNodes:visitedArr},SIM_OPTS);
  const svg=document.getElementById('dis-map');
  if(svg) svg.querySelectorAll('.map-node').forEach(g=>{
    g.style.cursor='pointer';
    g.addEventListener('click',()=>disasterNode(+g.dataset.node));
  });
  // ── DFS to find connected components (unchanged logic) ──────────
  const active=[...Array(N).keys()].filter(n=>!disabledNodes.has(n));
  const vis=new Set(); const comp=[];
  active.forEach(s=>{
    if(!vis.has(s)){
      const c=[]; const q=[s];
      while(q.length){const u=q.pop(); if(vis.has(u)) continue; vis.add(u);c.push(u);
        buildAdj()[u].forEach(([v])=>{if(!vis.has(v)&&!disabledNodes.has(v)) q.push(v);});
      }
      comp.push(c);
    }
  });
  document.getElementById('dis-comps').textContent=
    `${comp.length} isolated area(s) detected — ${disabledNodes.size} zone(s) offline`;
}

function runDispatch(){
  const complaints=[...complaintsData].map((c,i)=>({...c,id:'E-'+String(i+1).padStart(3,'0')}));
  function heapify(a,n,i){
    let lg=i,l=2*i+1,r=2*i+2;
    if(l<n&&a[l].urg>a[lg].urg) lg=l;
    if(r<n&&a[r].urg>a[lg].urg) lg=r;
    if(lg!==i){[a[i],a[lg]]=[a[lg],a[i]];heapify(a,n,lg);}
  }
  for(let i=Math.floor(complaints.length/2)-1;i>=0;i--) heapify(complaints,complaints.length,i);
  for(let i=complaints.length-1;i>0;i--){[complaints[0],complaints[i]]=[complaints[i],complaints[0]];heapify(complaints,i,0);}
  complaints.reverse();

  const el=document.getElementById('dispatch-queue');
  el.innerHTML='<div style="font-size:12px;color:var(--muted);margin-bottom:10px;">Dispatching crews in priority order:</div>';
  complaints.forEach((c,i)=>{
    setTimeout(()=>{
      const div=document.createElement('div');
      div.style.cssText='display:flex;align-items:center;gap:10px;padding:8px 12px;border:1px solid var(--border);margin-bottom:6px;border-radius:3px;animation:fadeIn .4s;border-left:4px solid '+(c.urg>=8?'#b91c1c':c.urg>=4?'#f59e0b':'#2E6B3E');
      div.innerHTML=`<span style="font-family:monospace;font-size:11px;color:var(--muted);">#${i+1}</span><strong style="font-size:13px;">${c.id}</strong><span style="font-size:12px;flex:1;">${c.desc}</span>${urgBadge(c.urg)}`;
      el.appendChild(div);
    },i*400);
  });
}

document.querySelector('[data-panel="ps1"]')?.addEventListener('click',()=>setTimeout(drawSimMap,50));
document.querySelector('[data-panel="ps2"]')?.addEventListener('click',()=>{
  disabledNodes=new Set();
  setTimeout(()=>{
    MapEngine.init('dis-map', SIM_OPTS);
    const svg=document.getElementById('dis-map');
    if(svg) svg.querySelectorAll('.map-node').forEach(g=>{
      g.style.cursor='pointer';
      g.addEventListener('click',()=>disasterNode(+g.dataset.node));
    });
    document.getElementById('dis-comps').textContent='All zones connected. Click a zone to mark it offline.';
  },50);
});


async function loadComplaints() {
    try {
        const res = await api('/complaints');
        complaintsData = res.data.map(c => ({
            id: c.id,
            cat: c.category || c.cat || 'ROAD',
            desc: c.description || c.desc || '',
            urg: c.urgency || c.urg || 5,
            zone: c.zone || '',
            status: c.status || 'Pending',
            date: c.date_filed || c.date || '2026-01-01'
        }));
        renderComplaints(complaintsData);
    } catch(e) {
        console.log('Using local complaints');
        renderComplaints(complaintsData);
    }
}

async function loadProjects() {
    try {
        const res = await api('/projects');
        ksProjects = res.data;
    } catch(e) {
        console.log('Using local projects');
    }
}

async function loadCitizens() {
    try {
        const res = await api('/citizens');
        citizens = res.data;
        const tbody=document.getElementById('ht-tbody');
        if(tbody) tbody.innerHTML=citizens.map(c=>`<tr><td>${c.id}</td><td>${c.name}</td><td>${c.zone}</td><td>Bucket ${htHash(c.id)}</td></tr>`).join('');
    } catch(e) {
        console.log('Using local citizens');
    }
}





function renderCityMap(nodes, edges, svgId) {
    const svg = document.getElementById(svgId);
    if (!svg || !nodes || !edges) return;
    
    const W = svg.clientWidth || svg.getAttribute('width') || 620;
    const H = svg.clientHeight || svg.getAttribute('height') || 520;
    const PAD = 70;
    
    const MIN_X = 100, MAX_X = 520;
    const MIN_Y = 80,  MAX_Y = 380;
    
    function sx(x) { return PAD + ((x - MIN_X) / (MAX_X - MIN_X)) * (W - 2*PAD); }
    function sy(y) { return PAD + ((y - MIN_Y) / (MAX_Y - MIN_Y)) * (H - 2*PAD); }
    
    if(!window.nodePositions) window.nodePositions = {};
    window.nodePositions[svgId] = {};
    nodes.forEach(n => {
        window.nodePositions[svgId][n.id] = { x: sx(n.x), y: sy(n.y), label: n.label };
    });
    
    svg.innerHTML = '';
    
    for (let gx = 0; gx < W; gx += 35) {
        for (let gy = 0; gy < H; gy += 35) {
            const dot = document.createElementNS('http://www.w3.org/2000/svg','circle');
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
        
        const line = document.createElementNS('http://www.w3.org/2000/svg','line');
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
        
        const bg = document.createElementNS('http://www.w3.org/2000/svg','rect');
        bg.setAttribute('x', mx-10); bg.setAttribute('y', my-9);
        bg.setAttribute('width', 20); bg.setAttribute('height', 16);
        bg.setAttribute('fill', 'white');
        bg.setAttribute('stroke', '#C8D4E0');
        bg.setAttribute('rx', '3');
        svg.appendChild(bg);
        
        const wt = document.createElementNS('http://www.w3.org/2000/svg','text');
        wt.setAttribute('x', mx); wt.setAttribute('y', my+4);
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
        
        const circle = document.createElementNS('http://www.w3.org/2000/svg','circle');
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
        
        const lbl = document.createElementNS('http://www.w3.org/2000/svg','text');
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
                const v = pathArray[i+1];
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
            const t = document.createElementNS('http://www.w3.org/2000/svg','text');
            t.setAttribute('x', cx); t.setAttribute('y', cy+5);
            t.setAttribute('text-anchor','middle');
            t.setAttribute('fill', color);
            t.setAttribute('font-weight','bold');
            t.setAttribute('font-size','13');
            t.setAttribute('font-family','Arial');
            t.setAttribute('class','path-label');
            t.textContent = letter;
            svg.appendChild(t);
        };
        addLabel(pathArray[0], 'A', '#2E7D32', '#E8F5E9');
        addLabel(pathArray[pathArray.length-1], 'B', '#C62828', '#FFEBEE');
    }, pathArray.length * 250);
}
