document.addEventListener('DOMContentLoaded', async () => {
    const token = sessionStorage.getItem('scae_token');
    const role = sessionStorage.getItem('scae_role');
    if (!token || (role !== 'dept' && role !== 'admin')) {
        window.location.href = 'index.html';
        return;
    }
    const name = sessionStorage.getItem('scae_name');
    const loggedInEl = document.getElementById('loggedInName');
    if(loggedInEl) loggedInEl.textContent = name || 'Dept Operator';
    
    await Promise.all([
        // loadWorkOrders(),
        // loadEmergencies()
    ]);
});


// ── Shared ────────────────────────────────────────────────────────
const ZONES=['Central Junction','North Gate','City Hall','East Market','West Park','Downtown','South Bridge','River Road','Industrial Zone','Medical Hub','Tech District','Old Town','Sports Complex'];
const PANEL_LABELS={p01:'Work Order Queue',p02:'Schedule Jobs',p03:'Activity Planner',p04:'Resource Allocator',p05:'Emergency Dispatch',p06:'Sensor Data Logging',p07:'Operations Optimization'};
function showPanel(id){
  document.querySelectorAll('.panel').forEach(p=>p.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n=>n.classList.remove('active'));
  document.getElementById(id)?.classList.add('active');
  document.querySelector(`[data-panel="${id}"]`)?.classList.add('active');
  const bc=document.getElementById('bc-panel');if(bc)bc.textContent=PANEL_LABELS[id]||id;
}
document.querySelectorAll('.nav-item').forEach(n=>n.addEventListener('click',()=>showPanel(n.dataset.panel)));
function showToast(msg,type='success'){const t=document.getElementById('toast');t.textContent=msg;t.className='toast'+(type==='error'?' error':'')+' show';setTimeout(()=>t.classList.remove('show'),3000);}
function openModal(html){document.getElementById('modal-content').innerHTML=html;document.getElementById('modal').classList.add('open');}
function closeModal(){document.getElementById('modal').classList.remove('open');}
document.getElementById('modal')?.addEventListener('click',e=>{if(e.target===e.currentTarget)closeModal();});
document.addEventListener('keydown',e=>{if(e.key==='Escape')closeModal();});
function urgBadge(u){if(u>=8)return`<span class="urg urg-h">${u}</span>`;if(u>=4)return`<span class="urg urg-m">${u}</span>`;return`<span class="urg urg-l">${u}</span>`;}

// ── P01: Work Order Queue ─────────────────────────────────────────
let jobs=[
  {id:'WO-001',title:'Road Repair',zone:'North Gate',urg:9,deadline:'2025-04-01',crew:'Crew Alpha',status:'Pending'},
  {id:'WO-002',title:'Power Line Fix',zone:'East Market',urg:8,deadline:'2025-03-30',crew:'Crew Bravo',status:'In Progress'},
  {id:'WO-003',title:'Water Main Check',zone:'South Bridge',urg:6,deadline:'2025-04-05',crew:'Crew Alpha',status:'Pending'},
  {id:'WO-004',title:'Signal Repair',zone:'Medical Hub',urg:10,deadline:'2025-03-28',crew:'Crew Charlie',status:'Overdue'},
  {id:'WO-005',title:'Drain Cleaning',zone:'West Park',urg:3,deadline:'2025-04-10',crew:'Crew Delta',status:'Completed'},
  {id:'WO-006',title:'Pothole Fill',zone:'Downtown',urg:7,deadline:'2025-04-02',crew:'Crew Bravo',status:'In Progress'},
  {id:'WO-007',title:'Street Light',zone:'City Hall',urg:5,deadline:'2025-04-08',crew:'Crew Alpha',status:'Pending'},
  {id:'WO-008',title:'Sewer Inspection',zone:'Old Town',urg:4,deadline:'2025-04-12',crew:'Crew Delta',status:'In Progress'},
  {id:'WO-009',title:'Bridge Check',zone:'Tech District',urg:9,deadline:'2025-03-29',crew:'Crew Charlie',status:'In Progress'},
  {id:'WO-010',title:'Pavement Marking',zone:'Central Junction',urg:2,deadline:'2025-04-15',crew:'Crew Bravo',status:'Completed'},
  {id:'WO-011',title:'Tree Trimming',zone:'River Road',urg:5,deadline:'2025-04-06',crew:'Crew Delta',status:'In Progress'},
  {id:'WO-012',title:'CCTV Install',zone:'Industrial Zone',urg:8,deadline:'2025-04-03',crew:'Crew Charlie',status:'Completed'},
];
function statusPill(s){const m={Pending:'sp-p','In Progress':'sp-ip',Completed:'sp-c',Overdue:'sp-o'};return`<span class="sp ${m[s]||'sp-p'}">${s}</span>`;}
function renderJobs(){
  document.getElementById('jq-tbody').innerHTML=jobs.map((j,i)=>
    `<tr><td style="font-family:var(--mono);">${j.id}</td><td>${j.title}</td><td>${j.zone}</td><td>${urgBadge(j.urg)}</td><td style="font-family:var(--mono);">${j.deadline}</td><td>${j.crew}</td><td>${statusPill(j.status)}</td><td>${j.status!=='Completed'?`<button class="btn btn-ghost btn-sm" onclick="completeJob(${i})">Complete</button>`:''}</td></tr>`
  ).join('');
  document.getElementById('jq-total').textContent=jobs.length;
  document.getElementById('jq-high').textContent=jobs.filter(j=>j.urg>=8).length;
  document.getElementById('jq-ip').textContent=jobs.filter(j=>j.status==='In Progress').length;
  document.getElementById('jq-comp').textContent=jobs.filter(j=>j.status==='Completed').length;
}
renderJobs();
function completeJob(i){jobs[i].status='Completed';renderJobs();showToast(`${jobs[i].title} marked as completed.`);}
function heapify(a,n,i,key){let lg=i,l=2*i+1,r=2*i+2;if(l<n&&a[l][key]>a[lg][key])lg=l;if(r<n&&a[r][key]>a[lg][key])lg=r;if(lg!==i){[a[i],a[lg]]=[a[lg],a[i]];heapify(a,n,lg,key);}}
function sortJobs(by){
  if(by==='urgency'){
    for(let i=Math.floor(jobs.length/2)-1;i>=0;i--)heapify(jobs,jobs.length,i,'urg');
    for(let i=jobs.length-1;i>0;i--){[jobs[0],jobs[i]]=[jobs[i],jobs[0]];heapify(jobs,i,0,'urg');}
    jobs.reverse();
    document.getElementById('sort-msg').textContent='Jobs sorted by priority level';
  } else {
    for(let i=1;i<jobs.length;i++){const key=jobs[i];let j=i-1;while(j>=0&&jobs[j].deadline>key.deadline){jobs[j+1]=jobs[j];j--;}jobs[j+1]=key;}
    document.getElementById('sort-msg').textContent='Jobs sorted by deadline';
  }
  renderJobs();
}
function extractMax(){
  const pend=jobs.filter(j=>j.status==='Pending').sort((a,b)=>b.urg-a.urg);
  if(!pend.length){showToast('No pending jobs to assign.','error');return;}
  const top=pend[0]; top.status='In Progress';
  renderJobs();
  showToast(`Work order assigned successfully. ${top.crew} is being notified for "${top.title}".`);
}
function openJobModal(){
  openModal(`<h3 style="margin-bottom:14px;color:#003366;">Add New Work Order</h3>
    <label class="field">Job Title</label><input type="text" id="nj-title"/>
    <label class="field">Area</label><select id="nj-zone">${ZONES.map((z,i)=>`<option value="${z}">${z}</option>`).join('')}</select>
    <label class="field">Priority (1-10)</label><input type="number" id="nj-urg" min="1" max="10" value="5"/>
    <label class="field">Deadline</label><input type="date" id="nj-dead"/>
    <label class="field">Assign To</label><select id="nj-crew"><option>Crew Alpha</option><option>Crew Bravo</option><option>Crew Charlie</option><option>Crew Delta</option></select>
    <label class="field">Description</label><textarea id="nj-desc"></textarea>
    <button class="btn btn-a btn-full" style="margin-top:8px;" onclick="addJob()">Submit Work Order</button>`);
}
function addJob(){
  const title=document.getElementById('nj-title').value.trim();
  if(!title){showToast('Enter a job title.','error');return;}
  const job={id:'WO-'+String(jobs.length+1).padStart(3,'0'),title,zone:document.getElementById('nj-zone').value,urg:+document.getElementById('nj-urg').value,deadline:document.getElementById('nj-dead').value||'2025-04-15',crew:document.getElementById('nj-crew').value,status:'Pending'};
  jobs.push(job);
  for(let i=jobs.length-1;i>0;i--){if(jobs[i].urg>jobs[i-1].urg)[jobs[i],jobs[i-1]]=[jobs[i-1],jobs[i]];else break;}
  renderJobs();closeModal();showToast(`Work order "${title}" created and queued.`);
}

// ── P02: Job Scheduler ───────────────────────────────────────────
let jsJobs=[
  {name:'Road Repair North',d:2,p:85},{name:'Power Grid Fix',d:1,p:60},
  {name:'Water Main',d:3,p:75},{name:'Signal Repair',d:2,p:90},
  {name:'Drain Fix',d:4,p:45},{name:'Bridge Check',d:3,p:70}
];
function renderJSTable(){
  document.getElementById('js-tbody').innerHTML=jsJobs.map((j,i)=>
    `<tr><td><input class="edit-input-wide" value="${j.name}" onchange="jsJobs[${i}].name=this.value"/></td><td><input class="edit-input" type="number" min="1" max="8" value="${j.d}" onchange="jsJobs[${i}].d=+this.value"/></td><td><input class="edit-input" type="number" min="1" value="${j.p}" onchange="jsJobs[${i}].p=+this.value"/></td></tr>`
  ).join('');
}
renderJSTable();
function addJSRow(){if(jsJobs.length>=8){showToast('Max 8 jobs.','error');return;}jsJobs.push({name:'New Job',d:3,p:50});renderJSTable();}
function runJobSeq(){
  const sorted=[...jsJobs].sort((a,b)=>b.p-a.p);
  const maxD=Math.max(...sorted.map(j=>j.d));
  const slots=Array(maxD).fill(null);
  const scheduled=[],rejected=[];
  sorted.forEach(j=>{for(let s=Math.min(j.d,maxD)-1;s>=0;s--){if(!slots[s]){slots[s]=j;scheduled.push(j);return;}}rejected.push(j);});
  document.getElementById('js-gantt').innerHTML=slots.map((s,i)=>
    `<div class="gantt-slot${s?' filled':''}" style="animation-delay:${i*0.15}s;">${s?s.name:'Slot '+(i+1)}</div>`
  ).join('');
  const totalP=scheduled.reduce((a,j)=>a+j.p,0);
  document.getElementById('js-result').innerHTML=
    `<div style="font-size:13px;"><strong>Scheduled Jobs:</strong> ${scheduled.map(j=>'✓ '+j.name+' (rating: '+j.p+')').join(', ')}</div>`+
    `<div style="font-size:28px;font-weight:bold;color:#003366;margin:10px 0;">Total Score: ${totalP}</div>`+
    (rejected.length?`<div style="color:var(--muted);font-size:12px;"><strong>Deferred:</strong> ${rejected.map(j=>j.name).join(', ')}</div>`:'');
}

// ── P03: Activity Selection ────────────────────────────────────────
let asActs=[
  {name:'Road Inspection',s:'06:00',e:'08:00'},{name:'Power Check',s:'07:30',e:'09:00'},
  {name:'Water Survey',s:'08:00',e:'10:00'},{name:'Signal Test',s:'09:30',e:'11:00'},
  {name:'Drain Check',s:'10:00',e:'12:00'},{name:'Bridge Survey',s:'11:30',e:'13:00'},
  {name:'CCTV Install',s:'12:00',e:'14:00'},{name:'Pavement Mark',s:'13:30',e:'15:00'}
];
function renderASTable(){
  document.getElementById('as-tbody').innerHTML=asActs.map((a,i)=>
    `<tr><td><input class="edit-input-wide" value="${a.name}" onchange="asActs[${i}].name=this.value"/></td><td><input class="edit-input" value="${a.s}" onchange="asActs[${i}].s=this.value"/></td><td><input class="edit-input" value="${a.e}" onchange="asActs[${i}].e=this.value"/></td></tr>`
  ).join('');
}
renderASTable();
function addASRow(){if(asActs.length>=10)return;asActs.push({name:'New Activity',s:'14:00',e:'15:30'});renderASTable();}
function timeToMin(t){const[h,m]=t.split(':').map(Number);return h*60+m;}
function runActivitySel(){
  const sorted=[...asActs].map((a,i)=>({...a,idx:i})).sort((a,b)=>timeToMin(a.e)-timeToMin(b.e));
  const selected=[sorted[0]],rejected=[];
  for(let i=1;i<sorted.length;i++){
    if(timeToMin(sorted[i].s)>=timeToMin(selected[selected.length-1].e)) selected.push(sorted[i]);
    else rejected.push({...sorted[i],reason:selected[selected.length-1].name});
  }
  const minT=360,maxT=960,range=maxT-minT;
  const tl=document.getElementById('as-timeline');tl.innerHTML='';
  for(let h=6;h<=16;h++){
    const pct=((h*60-minT)/range)*100;
    tl.innerHTML+=`<div style="position:absolute;left:${pct}%;top:0;bottom:0;border-left:1px dashed var(--border);"></div><div style="position:absolute;left:${pct}%;bottom:2px;font-size:9px;color:var(--muted);font-family:monospace;">${String(h).padStart(2,'0')}:00</div>`;
  }
  asActs.forEach((a,i)=>{
    const left=((timeToMin(a.s)-minT)/range)*100;
    const width=((timeToMin(a.e)-timeToMin(a.s))/range)*100;
    const sel=selected.find(s=>s.name===a.name);
    const bar=document.createElement('div');bar.className='timeline-bar';
    bar.style.cssText=`left:${left}%;width:${width}%;top:${10+i*24}px;background:${sel?'#003366':'#ccc'};color:${sel?'#fff':'#888'};animation-delay:${i*0.1}s;`;
    bar.textContent=a.name;if(!sel) bar.style.textDecoration='line-through';
    tl.appendChild(bar);
  });
  tl.style.height=(20+asActs.length*24)+'px';
  document.getElementById('as-result').innerHTML=
    `<div><strong>Scheduled (${selected.length} of ${asActs.length}):</strong> ${selected.map(a=>`✓ ${a.name} (${a.s}–${a.e})`).join(', ')}</div>`+
    (rejected.length?`<div style="color:var(--muted);margin-top:6px;"><strong>Deferred:</strong> ${rejected.map(r=>`${r.name} — conflicts with ${r.reason}`).join(', ')}</div>`:'');
}

// ── P04: Fractional Knapsack ──────────────────────────────────────
const fkItems=[
  {name:'Medical Kit',w:30,v:90},{name:'First Aid Box',w:10,v:50},
  {name:'Emergency Food',w:80,v:60},{name:'Water Tanks',w:120,v:70},
  {name:'Generator',w:150,v:85},{name:'Rescue Tools',w:60,v:75},
  {name:'Blankets',w:40,v:40},{name:'Medicines',w:20,v:95}
];
(function initFK(){
  document.getElementById('fk-tbody').innerHTML=fkItems.map(it=>
    `<tr><td>${it.name}</td><td style="font-family:var(--mono);">${it.w}</td><td style="font-family:var(--mono);">${it.v}</td><td style="font-family:var(--mono);color:#8B4513;font-weight:600;">${(it.v/it.w).toFixed(2)}</td></tr>`
  ).join('');
})();
let fkChart=null;
function runFracKnapsack(){
  const cap=+document.getElementById('fk-cap').value;
  const sorted=[...fkItems].sort((a,b)=>(b.v/b.w)-(a.v/a.w));
  let rem=cap,totalV=0,totalW=0;const alloc=[];
  sorted.forEach(it=>{if(rem<=0)return;if(it.w<=rem){alloc.push({...it,taken:it.w,frac:1});totalV+=it.v;totalW+=it.w;rem-=it.w;}else{const f=rem/it.w;alloc.push({...it,taken:rem,frac:f});totalV+=it.v*f;totalW+=rem;rem=0;}});
  if(fkChart)fkChart.destroy();
  fkChart=new Chart(document.getElementById('fk-chart'),{type:'doughnut',data:{labels:['Loaded','Available'],datasets:[{data:[totalW,cap-totalW],backgroundColor:['#8B4513','#e9e6e0']}]},options:{responsive:true,plugins:{legend:{position:'bottom'}}}});
  let html='<table><thead><tr><th>Item</th><th>Weight Loaded</th><th>Fraction</th><th>Value</th></tr></thead><tbody>';
  alloc.forEach(a=>{html+=`<tr${a.frac<1?' style="background:var(--amber-bg);"':''}><td>${a.name}${a.frac<1?' (partial)':''}</td><td style="font-family:var(--mono);">${a.taken.toFixed(1)} kg</td><td style="font-family:var(--mono);">${(a.frac*100).toFixed(1)}%</td><td style="font-family:var(--mono);">${(a.v*a.frac).toFixed(1)}</td></tr>`;});
  html+='</tbody></table>';
  html+=`<div style="font-size:28px;font-weight:bold;color:#003366;margin-top:14px;">Total Utility: ${totalV.toFixed(1)}</div>`;
  html+=`<div style="margin-top:8px;background:#F2F2F2;height:10px;border-radius:3px;overflow:hidden;"><div style="background:#003366;height:100%;width:${totalW/cap*100}%;"></div></div><span style="font-size:11px;color:#666666;">${totalW.toFixed(1)} / ${cap} kg loaded</span>`;
  document.getElementById('fk-result').innerHTML=html;
}

// ── P05: Emergency Dispatch ───────────────────────────────────────
const EM_TYPES=['Power Outage','Road Flood','Gas Leak','Water Break','Fire Hazard','Signal Failure','Structural Damage','Medical Emergency','Traffic Accident','Bridge Alert'];
const CREWS=['Alpha','Bravo','Charlie','Delta'];
let emergencies=[],dispatched=[];let emChart=null;
function genEmergencies(){
  emergencies=Array.from({length:10},(_,i)=>({id:i+1,type:EM_TYPES[Math.floor(Math.random()*EM_TYPES.length)],zone:ZONES[Math.floor(Math.random()*ZONES.length)],urg:Math.floor(Math.random()*10)+1,time:String(Math.floor(Math.random()*12)+6).padStart(2,'0')+':'+String(Math.floor(Math.random()*60)).padStart(2,'0'),crew:CREWS[Math.floor(Math.random()*4)],status:'Queued'}));
  dispatched=[];renderEM();showToast('Generated 10 emergency calls.');
}
function renderEM(){
  document.getElementById('em-tbody').innerHTML=emergencies.map(e=>
    `<tr><td>${e.id}</td><td>${e.type}</td><td>${e.zone}</td><td>${urgBadge(e.urg)}</td><td style="font-family:var(--mono);">${e.time}</td><td>Crew ${e.crew}</td><td>${e.status==='Dispatched'?'<span class="sp sp-c">Dispatched</span>':'<span class="sp sp-p">Queued</span>'}</td></tr>`
  ).join('');
  updateEMChart();
  document.getElementById('em-history').innerHTML=dispatched.map(e=>
    `<div class="dispatch-card" style="border-left-color:${e.urg>=8?'#b91c1c':e.urg>=4?'#f59e0b':'#2E6B3E'};"><span style="font-family:var(--mono);font-size:11px;color:var(--muted);">#${e.id}</span><strong>${e.type}</strong><span style="flex:1;font-size:12px;color:var(--muted);">${e.zone} | Crew ${e.crew}</span>${urgBadge(e.urg)}</div>`
  ).join('');
}
function sortEmergencies(method){
  if(method==='heap'){
    const a=emergencies;for(let i=Math.floor(a.length/2)-1;i>=0;i--)heapify(a,a.length,i,'urg');for(let i=a.length-1;i>0;i--){[a[0],a[i]]=[a[i],a[0]];heapify(a,i,0,'urg');}a.reverse();
    showToast('Emergencies sorted by urgency.');
  } else {
    const sorted=[];ZONES.forEach(z=>{sorted.push(...emergencies.filter(e=>e.zone===z));});
    emergencies.splice(0,emergencies.length,...sorted);
    let countHTML='<table style="margin-top:8px;max-width:400px;"><thead><tr><th>Area</th><th>Count</th></tr></thead><tbody>';
    ZONES.forEach(z=>{const c=emergencies.filter(e=>e.zone===z).length;if(c)countHTML+=`<tr><td>${z}</td><td style="font-family:var(--mono);">${c}</td></tr>`;});
    countHTML+='</tbody></table>';
    document.getElementById('em-count').innerHTML='<label class="field">Area Distribution</label>'+countHTML;
    showToast('Emergencies grouped by area.');
  }
  renderEM();
}
function dispatchNext(){
  const queued=emergencies.filter(e=>e.status==='Queued');
  if(!queued.length){showToast('No queued emergencies.','error');return;}
  queued.sort((a,b)=>b.urg-a.urg);queued[0].status='Dispatched';dispatched.unshift(queued[0]);
  renderEM();showToast(`Dispatched: ${queued[0].type} (Priority ${queued[0].urg}) → Crew ${queued[0].crew}`);
}
function updateEMChart(){
  const counts=[0,0,0,0];
  emergencies.filter(e=>e.status==='Queued').forEach(e=>{if(e.urg>=9)counts[0]++;else if(e.urg>=7)counts[1]++;else if(e.urg>=4)counts[2]++;else counts[3]++;});
  if(emChart)emChart.destroy();const ctx=document.getElementById('em-chart');if(!ctx)return;
  emChart=new Chart(ctx,{type:'bar',data:{labels:['Critical','High','Medium','Low'],datasets:[{label:'Count',data:counts,backgroundColor:['#b91c1c','#f59e0b','#D4956A','#2E6B3E']}]},options:{responsive:true,plugins:{legend:{display:false}},scales:{y:{beginAtZero:true,ticks:{stepSize:1}}}}});
}

// ── P06: Huffman Coding ───────────────────────────────────────────
const HUF_SAMPLES={traffic:'aabbbcccdddeeeffggaabbcdeefg',air:'aaaabbbbbbccdddeeeffffffggghhh',temp:'tttteeeeemmmmppprrraaa'};
function setHufSample(key){document.getElementById('huf-input').value=HUF_SAMPLES[key]||'';}
function runHuffman(){
  const str=document.getElementById('huf-input').value;if(!str){showToast('Enter data to compress.','error');return;}
  const freq={};for(const c of str)freq[c]=(freq[c]||0)+1;
  const chars=Object.entries(freq).sort((a,b)=>a[1]-b[1]);
  let heap=chars.map(([ch,f])=>({ch,f,left:null,right:null}));
  while(heap.length>1){heap.sort((a,b)=>a.f-b.f);const l=heap.shift(),r=heap.shift();heap.push({ch:null,f:l.f+r.f,left:l,right:r});}
  const root=heap[0];const codes={};
  function traverse(node,code){if(!node)return;if(node.ch!==null){codes[node.ch]=code||'0';return;}traverse(node.left,code+'0');traverse(node.right,code+'1');}
  traverse(root,'');
  const origBits=str.length*8;let compBits=0;for(const c of str)compBits+=codes[c].length;
  const saved=((origBits-compBits)/origBits*100).toFixed(1);
  document.getElementById('huf-orig').textContent=origBits+' bits';
  document.getElementById('huf-comp').textContent=compBits+' bits';
  document.getElementById('huf-saved').textContent=saved+'%';
  document.getElementById('huf-ratio').textContent=(origBits/compBits).toFixed(2)+':1';
  document.getElementById('huf-freq').innerHTML='<table><thead><tr><th>Char</th><th>Freq</th><th>%</th></tr></thead><tbody>'+chars.map(([c,f])=>`<tr><td style="font-family:var(--mono);font-weight:700;">'${c}'</td><td>${f}</td><td>${(f/str.length*100).toFixed(1)}%</td></tr>`).join('')+'</tbody></table>';
  const sortedCodes=Object.entries(codes).sort((a,b)=>a[1].length-b[1].length);
  document.getElementById('huf-codes').innerHTML='<table><thead><tr><th>Char</th><th>Code</th><th>Len</th></tr></thead><tbody>'+sortedCodes.map(([c,code])=>`<tr${code.length<=2?` style="background:var(--amber-bg);"`:''}><td style="font-family:var(--mono);font-weight:700;">'${c}'</td><td style="font-family:var(--mono);">${code}</td><td>${code.length}</td></tr>`).join('')+'</tbody></table>';
  drawHufTree(root);
}
function drawHufTree(root){
  const svg=document.getElementById('huf-tree');if(!svg||!root)return;
  const W=svg.clientWidth||600,H=300;svg.setAttribute('viewBox',`0 0 ${W} ${H}`);let html='';
  function draw(node,x,y,dx){
    if(!node)return;const r=node.ch!==null?14:10;const fill=node.ch!==null?'#8B4513':'#ccc';const tc=node.ch!==null?'#fff':'#333';
    if(node.left){const cx=x-dx,cy=y+55;html+=`<line x1="${x}" y1="${y}" x2="${cx}" y2="${cy}" stroke="#aaa"/><text x="${(x+cx)/2-6}" y="${(y+cy)/2}" fill="#8B4513" font-size="10" font-family="monospace">0</text>`;draw(node.left,cx,cy,dx/2);}
    if(node.right){const cx=x+dx,cy=y+55;html+=`<line x1="${x}" y1="${y}" x2="${cx}" y2="${cy}" stroke="#aaa"/><text x="${(x+cx)/2+3}" y="${(y+cy)/2}" fill="#8B4513" font-size="10" font-family="monospace">1</text>`;draw(node.right,cx,cy,dx/2);}
    html+=`<circle cx="${x}" cy="${y}" r="${r}" fill="${fill}" stroke="#8B4513"/><text x="${x}" y="${y+4}" class="huf-node" fill="${tc}">${node.ch!==null?node.ch:node.f}</text>`;
  }
  draw(root,W/2,22,W/4.5);svg.innerHTML=html;
}

// ── P07: Matrix Chain DP ──────────────────────────────────────────
const mcDims=[30,35,15,5,10,20,25];
(function initMC(){
  const el=document.getElementById('mc-dims');if(!el)return;
  el.innerHTML=mcDims.map((d,i)=>`<input class="edit-input" style="width:50px;" type="number" value="${d}" id="mcd${i}"/>`).join('×');
  const n=mcDims.length-1;
  document.getElementById('mc-labels').innerHTML='Reports: '+Array.from({length:n},(_,i)=>`R${i+1}(${mcDims[i]}×${mcDims[i+1]})`).join(' × ');
})();
function runMatrixChain(){
  const p=[];for(let i=0;i<mcDims.length;i++){const v=document.getElementById('mcd'+i);p.push(v?+v.value:mcDims[i]);}
  const n=p.length-1;const m=Array.from({length:n},()=>Array(n).fill(0));const s=Array.from({length:n},()=>Array(n).fill(0));
  for(let l=2;l<=n;l++)for(let i=0;i<=n-l;i++){const j=i+l-1;m[i][j]=Infinity;for(let k=i;k<j;k++){const q=m[i][k]+m[k+1][j]+p[i]*p[k+1]*p[j+1];if(q<m[i][j]){m[i][j]=q;s[i][j]=k;}}}
  let naive=0,rows=p[0],cols=p[1];for(let i=1;i<n;i++){naive+=rows*cols*p[i+1];cols=p[i+1];}
  const maxVal=m[0][n-1]||1;
  let html='<table style="border-collapse:collapse;"><thead><tr><th style="background:#8B4513;color:#fff;padding:4px 6px;"></th>';
  for(let j=0;j<n;j++)html+=`<th style="background:#8B4513;color:#fff;padding:4px 6px;font-size:10px;">R${j+1}</th>`;
  html+='</tr></thead><tbody>';
  for(let i=0;i<n;i++){html+=`<tr><td style="background:#8B4513;color:#fff;padding:4px 6px;font-size:10px;font-weight:600;">R${i+1}</td>`;for(let j=0;j<n;j++){if(j<i)html+=`<td style="background:#f0ece5;padding:4px 6px;border:1px solid var(--border);"></td>`;else{const intensity=m[i][j]/maxVal;const bg=j===i?'#f0ece5':`rgba(139,69,19,${Math.min(intensity*0.3+0.05,0.35)})`;html+=`<td style="padding:4px 6px;border:1px solid var(--border);text-align:center;font-family:var(--mono);font-size:10px;background:${bg};">${m[i][j]}</td>`;}}html+='</tr>';}
  html+='</tbody></table>';document.getElementById('mc-table').innerHTML=html;
  function paren(i,j){if(i===j)return`R${i+1}`;return`(${paren(i,s[i][j])} × ${paren(s[i][j]+1,j)})`;}
  const opt=paren(0,n-1);
  document.getElementById('mc-result').innerHTML=
    `<div style="font-size:16px;margin-bottom:10px;word-break:break-all;"><strong>Optimal Processing Order:</strong> ${opt}</div>`+
    `<div style="font-size:28px;font-weight:bold;color:#003366;">Minimum Operations: ${m[0][n-1].toLocaleString()}</div>`+
    `<div style="font-size:13px;color:var(--muted);margin-top:6px;">Without optimization (sequential): <strong>${naive.toLocaleString()}</strong> operations — <strong>${((1-m[0][n-1]/naive)*100).toFixed(1)}% time saved</strong></div>`;
}
