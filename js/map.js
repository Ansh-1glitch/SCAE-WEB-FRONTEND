/* ═══════════════════════════════════════════════════════════════════
   MapEngine — Clean City Graph Visualization
   Shared rendering module for admin & citizen route maps.
   All Dijkstra / BFS / BellmanFord algorithm logic stays in the
   caller files (admin.js, citizen.js). This file is visual only.
   ═══════════════════════════════════════════════════════════════════ */

// Full node labels (not abbreviated)
const FULL_LABELS = [
  'Central Junction','North Gate','City Hall','East Market',
  'West Park','Downtown','South Bridge','River Road',
  'Industrial Zone','Medical Hub','Tech District','Old Town',
  'Sports Complex'
];

// ── Inject CSS once ──────────────────────────────────────────────
(function injectMapCSS(){
  if(document.getElementById('map-engine-css')) return;
  const s=document.createElement('style'); s.id='map-engine-css';
  s.textContent=`
.map-svg-wrap{position:relative;width:100%;}
.map-svg-canvas{display:block;width:100%;cursor:default;background:#F8FAFD;border:1px solid #C8D4E0;border-radius:6px;}
.map-tooltip{position:absolute;pointer-events:none;background:#fff;border:1px solid #CBD5E1;color:#1A2E45;font-family:'Inter',sans-serif;font-size:12px;font-weight:600;padding:5px 12px;border-radius:6px;white-space:nowrap;z-index:99;display:none;box-shadow:0 2px 8px rgba(0,0,0,.1);}
.map-toast{position:absolute;top:12px;left:50%;transform:translateX(-50%);background:#fff;border:1px solid #DC2626;color:#DC2626;font-family:'Inter',sans-serif;font-size:12px;font-weight:600;padding:8px 18px;border-radius:6px;pointer-events:none;opacity:0;transition:opacity .3s;z-index:100;white-space:nowrap;box-shadow:0 2px 8px rgba(0,0,0,.1);}
.map-toast.show{opacity:1;}
.map-node circle{transition:fill .15s,stroke .15s;}
.map-node:hover circle.node-outer{fill:#EBF3FF!important;stroke:#1B3A6B!important;}
.map-legend-strip{display:flex;align-items:center;justify-content:center;gap:20px;padding:8px 0;font-family:'Inter',sans-serif;font-size:12px;color:#6B7280;}
.map-legend-item{display:flex;align-items:center;gap:5px;}
.map-legend-dot{width:12px;height:12px;border-radius:50%;flex-shrink:0;}
.map-legend-dash{width:18px;height:0;border-top:2.5px dashed #DC2626;flex-shrink:0;}
`;
  document.head.appendChild(s);
})();

// ── State store per SVG ──────────────────────────────────────────
const _mapState={};
function getState(svgId){
  if(!_mapState[svgId]) _mapState[svgId]={
    hlEdges:[],hlNodes:[],closedEdges:[],exploredEdges:[],
    visitedNodes:[],srcNode:-1,dstNode:-1,closureCb:null
  };
  return _mapState[svgId];
}

// ── Dot grid pattern ─────────────────────────────────────────────
function buildDotGrid(vw,vh){
  let dots='';
  for(let x=35;x<vw;x+=35) for(let y=35;y<vh;y+=35)
    dots+=`<circle cx="${x}" cy="${y}" r="1.2" fill="#D0D8E4" opacity="0.6"/>`;
  return `<g class="dot-grid">${dots}</g>`;
}

// ── Build edge ───────────────────────────────────────────────────
function buildEdge(u,v,w,NODES,state,idx){
  const x1=NODES[u].x, y1=NODES[u].y, x2=NODES[v].x, y2=NODES[v].y;
  const mx=(x1+x2)/2, my=(y1+y2)/2;
  const isClosed=state.closedEdges&&state.closedEdges.includes(idx);
  const isPath=state.hlEdges&&state.hlEdges.includes(idx);
  const isExplored=state.exploredEdges&&state.exploredEdges.includes(idx);

  let stroke,sw,dash;
  if(isClosed){stroke='#DC2626';sw=2.5;dash='6,4';}
  else if(isPath){stroke='#1565C0';sw=3.5;dash='';}
  else if(isExplored){stroke='#F59E0B';sw=2.5;dash='';}
  else{stroke='#9DB8D2';sw=2;dash='';}

  const dashAttr=dash?`stroke-dasharray="${dash}"`:'';
  const line=`<line id="edge-${idx}" data-edge="${idx}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${stroke}" stroke-width="${sw}" ${dashAttr} stroke-linecap="round"/>`;

  // Weight label with white rect background
  const tw=String(w).length*7+10, th=16;
  const pill=`<rect x="${mx-tw/2}" y="${my-th/2-1}" width="${tw}" height="${th}" rx="3" fill="#fff" stroke="#C8D4E0" stroke-width="1"/>
<text x="${mx}" y="${my+4}" fill="#445566" font-size="10" font-family="'Inter',sans-serif" font-weight="600" text-anchor="middle">${w}</text>`;

  // Red ✕ for closed
  let marker='';
  if(isClosed) marker=`<text x="${mx}" y="${my+18}" fill="#DC2626" font-size="14" text-anchor="middle" font-weight="700">×</text>`;

  return `<g class="road-edge" data-edge="${idx}" style="cursor:pointer">${line}${pill}${marker}</g>`;
}

// ── Build node ───────────────────────────────────────────────────
function buildNode(n,i,state){
  const {x,y}=n;
  const label=FULL_LABELS[i]||n.l;
  const isSrc=state.srcNode===i;
  const isDst=state.dstNode===i;
  const isPath=state.hlNodes&&state.hlNodes.includes(i)&&!isSrc&&!isDst;
  const isVis=state.visitedNodes&&state.visitedNodes.includes(i);

  let fill,stroke,sw,innerText='';
  if(isSrc){fill='#E8F5E9';stroke='#2E7D32';sw=3;innerText=`<text x="${x}" y="${y+4}" fill="#2E7D32" font-size="11" font-family="'Inter',sans-serif" font-weight="800" text-anchor="middle">A</text>`;}
  else if(isDst){fill='#FFEBEE';stroke='#C62828';sw=3;innerText=`<text x="${x}" y="${y+4}" fill="#C62828" font-size="11" font-family="'Inter',sans-serif" font-weight="800" text-anchor="middle">B</text>`;}
  else if(isPath){fill='#E3F2FD';stroke='#1565C0';sw=3;}
  else if(isVis){fill='#FFF8E1';stroke='#F9A825';sw=2.5;}
  else{fill='#FFFFFF';stroke='#2A5C9A';sw=2.5;}

  // Label position — below by default, above if node is near bottom
  const labelAbove=y>420;
  const ly=labelAbove?y-22:y+24;

  return `
<g class="map-node" data-node="${i}" style="cursor:pointer">
  <circle class="node-outer" cx="${x}" cy="${y}" r="14" fill="${fill}" stroke="${stroke}" stroke-width="${sw}"/>
  ${innerText}
  <text x="${x}" y="${ly}" fill="#334155" font-size="11" font-family="'Inter',sans-serif" font-weight="500" text-anchor="middle">${label}</text>
</g>`;
}

// ── Core draw ────────────────────────────────────────────────────
function drawCleanMap(svgId,hlEdges,hlNodes,overrides,options){
  const svg=document.getElementById(svgId); if(!svg) return;
  const NODES=options.NODES, EDGES=options.EDGES;
  const vw=options.viewW||620, vh=options.viewH||520;

  const state=getState(svgId);
  state.hlEdges=hlEdges||[];
  state.hlNodes=hlNodes||[];
  if(overrides) Object.assign(state,overrides);

  let html='';
  // Background
  html+=`<rect width="${vw}" height="${vh}" fill="#F8FAFD"/>`;
  // Dot grid
  html+=buildDotGrid(vw,vh);
  // Edges
  EDGES.forEach(([u,v,w],i)=>{ html+=buildEdge(u,v,w,NODES,state,i); });
  // Nodes
  NODES.forEach((n,i)=>{ html+=buildNode(n,i,state); });

  svg.innerHTML=html;

  // Re-attach closure handlers
  if(state.closureCb){
    svg.querySelectorAll('.road-edge').forEach(g=>{
      const idx=+g.dataset.edge;
      g.addEventListener('mouseenter',()=>{
        const line=g.querySelector('line');
        if(line&&!state.closedEdges.includes(idx)){
          line.setAttribute('stroke','#DC2626'); line.setAttribute('opacity','0.6');
          svg.style.cursor='pointer';
        }
      });
      g.addEventListener('mouseleave',()=>{
        drawCleanMap(svgId,state.hlEdges,state.hlNodes,null,options);
        if(state.closureCb) reattachHandlers(svgId,options);
      });
      g.addEventListener('click',()=>{state.closureCb(idx);});
    });
  }
  // Tooltips
  attachTooltips(svgId,NODES);
}

function reattachHandlers(svgId,options){
  const svg=document.getElementById(svgId); if(!svg) return;
  const state=getState(svgId);
  svg.querySelectorAll('.road-edge').forEach(g=>{
    const idx=+g.dataset.edge;
    g.addEventListener('mouseenter',()=>{
      const line=g.querySelector('line');
      if(line&&!state.closedEdges.includes(idx)){
        line.setAttribute('stroke','#DC2626');line.setAttribute('opacity','0.6');
        svg.style.cursor='pointer';
      }
    });
    g.addEventListener('mouseleave',()=>{
      drawCleanMap(svgId,state.hlEdges,state.hlNodes,null,options);
      if(state.closureCb) reattachHandlers(svgId,options);
    });
    g.addEventListener('click',()=>{state.closureCb(idx);});
  });
}

// ── Tooltip ──────────────────────────────────────────────────────
function attachTooltips(svgId,NODES){
  const svg=document.getElementById(svgId); if(!svg) return;
  let tip=svg.parentElement&&svg.parentElement.querySelector('.map-tooltip');
  if(!tip){tip=document.createElement('div');tip.className='map-tooltip';if(svg.parentElement)svg.parentElement.appendChild(tip);}
  svg.querySelectorAll('.map-node').forEach(g=>{
    const i=+g.dataset.node;
    g.addEventListener('mouseenter',(e)=>{
      tip.textContent=NODES[i].l;tip.style.display='block';
    });
    g.addEventListener('mousemove',(e)=>{
      if(svg.parentElement){const pr=svg.parentElement.getBoundingClientRect();tip.style.left=(e.clientX-pr.left+12)+'px';tip.style.top=(e.clientY-pr.top-10)+'px';}
    });
    g.addEventListener('mouseleave',()=>{tip.style.display='none';});
  });
}

// ── Toast ────────────────────────────────────────────────────────
function showMapToast(svgId,msg){
  const svg=document.getElementById(svgId); if(!svg||!svg.parentElement) return;
  let toast=svg.parentElement.querySelector('.map-toast');
  if(!toast){toast=document.createElement('div');toast.className='map-toast';svg.parentElement.appendChild(toast);}
  toast.textContent=msg;toast.classList.add('show');
  setTimeout(()=>toast.classList.remove('show'),3500);
}

// ── Enable closure ───────────────────────────────────────────────
function enableEdgeClickClosure(svgId,onClose,options){
  const state=getState(svgId);
  state.closedEdges=[];
  state.closureCb=(idx)=>{
    if(!state.closedEdges.includes(idx)) state.closedEdges.push(idx);
    drawCleanMap(svgId,state.hlEdges,state.hlNodes,null,options);
    reattachHandlers(svgId,options);
    const [u,v]=options.EDGES[idx];
    showMapToast(svgId,`Road closed: ${options.NODES[u].l} ↔ ${options.NODES[v].l}`);
    if(onClose) onClose(idx);
  };
  drawCleanMap(svgId,state.hlEdges,state.hlNodes,null,options);
  reattachHandlers(svgId,options);
}

// ── Reset closures ───────────────────────────────────────────────
function resetClosures(svgId,options){
  const state=getState(svgId);
  state.closedEdges=[];state.closureCb=null;
  drawCleanMap(svgId,state.hlEdges,state.hlNodes,null,options);
}

// ── Animated path draw ───────────────────────────────────────────
function animatePath(svgId,pathEdgeIndices,srcNode,dstNode,options){
  const NODES=options.NODES, EDGES=options.EDGES;
  const state=getState(svgId);
  state.srcNode=srcNode; state.dstNode=dstNode;
  state.hlNodes=[]; state.hlEdges=[];
  state.exploredEdges=[];

  // 1) Draw base with src/dst marked
  drawCleanMap(svgId,[],[],{srcNode,dstNode,exploredEdges:[]},options);
  const svg=document.getElementById(svgId); if(!svg) return;

  // 2) Explore edges one by one (amber), 200ms apart
  const allIdxs=EDGES.map((_,i)=>i);
  const nonPath=allIdxs.filter(i=>!pathEdgeIndices.includes(i));
  const exploreOrder=nonPath.slice(0,Math.min(nonPath.length,8));
  let step=0;
  const exploreTimer=setInterval(()=>{
    if(step>=exploreOrder.length){clearInterval(exploreTimer);drawFinalPath();return;}
    state.exploredEdges.push(exploreOrder[step]);
    drawCleanMap(svgId,[],[],{srcNode,dstNode,exploredEdges:[...state.exploredEdges]},options);
    step++;
  },200);

  function drawFinalPath(){
    // 3) Draw final path edges with stroke-dashoffset animation
    state.hlEdges=pathEdgeIndices;
    state.exploredEdges=[];
    drawCleanMap(svgId,pathEdgeIndices,[],{srcNode,dstNode},options);

    pathEdgeIndices.forEach((ei,idx)=>{
      const line=svg.querySelector(`#edge-${ei}`);
      if(!line) return;
      const dx=+line.getAttribute('x2')-(+line.getAttribute('x1'));
      const dy=+line.getAttribute('y2')-(+line.getAttribute('y1'));
      const len=Math.hypot(dx,dy)||100;
      line.style.strokeDasharray=len;
      line.style.strokeDashoffset=len;
      line.style.transition=`stroke-dashoffset 0.4s ease ${idx*0.15}s`;
      requestAnimationFrame(()=>{line.style.strokeDashoffset='0';});
    });

    // After anim completes, reattach tooltips
    setTimeout(()=>{
      attachTooltips(svgId,NODES);
      if(state.closureCb) reattachHandlers(svgId,options);
    },pathEdgeIndices.length*150+500);
  }
}

// ── Init ─────────────────────────────────────────────────────────
function initCleanMap(svgId,options){
  const svg=document.getElementById(svgId); if(!svg) return;
  const vw=options.viewW||620, vh=options.viewH||520;
  svg.setAttribute('viewBox',`0 0 ${vw} ${vh}`);
  svg.style.background='#F8FAFD';
  svg.classList.add('map-svg-canvas');

  // Wrap for tooltips
  if(svg.parentElement&&!svg.parentElement.classList.contains('map-svg-wrap')){
    const wrap=document.createElement('div');wrap.className='map-svg-wrap';
    svg.parentElement.insertBefore(wrap,svg);wrap.appendChild(svg);
  }

  // Add legend strip below SVG
  let legendWrap=svg.parentElement&&svg.parentElement.querySelector('.map-legend-strip');
  if(!legendWrap&&svg.parentElement){
    legendWrap=document.createElement('div');legendWrap.className='map-legend-strip';
    legendWrap.innerHTML=`
      <div class="map-legend-item"><div class="map-legend-dot" style="background:#1565C0"></div>Your Route</div>
      <div class="map-legend-item"><div class="map-legend-dot" style="background:#F59E0B"></div>Exploring</div>
      <div class="map-legend-item"><div class="map-legend-dot" style="background:#2E7D32"></div>Start Point</div>
      <div class="map-legend-item"><div class="map-legend-dot" style="background:#C62828"></div>End Point</div>
      <div class="map-legend-item"><div class="map-legend-dash"></div>Closed Road</div>`;
    svg.parentElement.appendChild(legendWrap);
  }

  const state=getState(svgId);
  state.srcNode=-1;state.dstNode=-1;
  drawCleanMap(svgId,[],[],{},options);
}

// ── Public API (same interface) ──────────────────────────────────
window.MapEngine={
  init:initCleanMap,
  draw:drawCleanMap,
  animate:animatePath,
  enableClosure:enableEdgeClickClosure,
  resetClosures:resetClosures,
  getState:getState,
  showToast:showMapToast
};
