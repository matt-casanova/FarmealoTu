"""GET /monitor — página web de desarrollo para ver en vivo la API, los datos NASA y la base del nodo.

Herramienta del equipo (no es parte de la app del agricultor). Solo LEE endpoints públicos de la API.
"""
from fastapi import APIRouter
from fastapi.responses import HTMLResponse

router = APIRouter(tags=["monitor (desarrollo)"])

PAGE = r"""<!doctype html>
<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Monitor FarmealoTu</title>
<style>
:root{--bg:#f6f7f9;--card:#fff;--ink:#14181f;--muted:#5d6675;--line:#e2e5ea;--ok:#127a3a;--warn:#a15c00;--bad:#b42318;
--sim:#a8431b;--simbg:#fdeee7;--wok:#5b3fc4;--wokbg:#efeafd;--phy:#14572d;--phybg:#dcefe3;--acc:#1c5cab}
@media (prefers-color-scheme:dark){:root{--bg:#111418;--card:#1a1e24;--ink:#e8eaee;--muted:#9aa3b2;--line:#2b313a;
--ok:#4cc27a;--warn:#e0a142;--bad:#f07167;--sim:#f3a382;--simbg:#3a2219;--wok:#b8a8ff;--wokbg:#2a2342;--phy:#7ad69c;--phybg:#173022;--acc:#7fb0f0}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:14px/1.45 system-ui,"Segoe UI",sans-serif}
header{padding:14px 20px;border-bottom:1px solid var(--line);display:flex;gap:12px;align-items:center;flex-wrap:wrap}
h1{font-size:18px;margin:0}main{padding:16px 20px;display:grid;gap:14px}
.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px 14px}
.k{color:var(--muted);font-size:12px;text-transform:uppercase;letter-spacing:.04em}.v{font-size:20px;font-weight:650;margin-top:2px}
.s{color:var(--muted);font-size:12px}.ok{color:var(--ok)}.warn{color:var(--warn)}.bad{color:var(--bad)}
table{width:100%;border-collapse:collapse;font-size:13px;font-variant-numeric:tabular-nums}
th,td{padding:6px 8px;border-bottom:1px solid var(--line);text-align:left;white-space:nowrap}th{color:var(--muted);font-weight:600;font-size:12px}
.tw{overflow-x:auto}.badge{display:inline-block;padding:1px 7px;border-radius:9px;font-size:11px;font-weight:700}
.simulated{color:var(--sim);background:var(--simbg)}.wokwi{color:var(--wok);background:var(--wokbg)}.physical{color:var(--phy);background:var(--phybg)}
tr.new td{animation:flash 1.6s ease-out}@keyframes flash{from{background:rgba(255,200,0,.25)}to{background:transparent}}
a{color:var(--acc)}.pill{border:1px solid var(--line);border-radius:999px;padding:3px 10px;font-size:12px}
.dot{display:inline-block;width:9px;height:9px;border-radius:50%;margin-right:6px;background:var(--muted)}
</style></head><body>
<header><h1>Monitor FarmealoTu</h1><span class="pill"><span id="dot" class="dot"></span><span id="conn">conectando…</span></span>
<span class="s">Se actualiza cada 3 s · solo lectura · <a href="/docs">/docs</a> · <a href="/api/indicators">/api/indicators</a> ·
<a href="/api/map/layers">/api/map/layers</a> · <a href="/api/field-node/readings">/api/field-node/readings</a></span></header>
<main>
<section class="grid">
 <div class="card"><div class="k">Lecturas del nodo</div><div class="v" id="total">—</div><div class="s" id="bysrc"></div></div>
 <div class="card"><div class="k">Último contacto del nodo</div><div class="v" id="last">—</div><div class="s" id="lastsrc"></div></div>
 <div class="card"><div class="k">NASA POWER · última observación</div><div class="v" id="nasaobs">—</div><div class="s" id="nasadl"></div></div>
 <div class="card"><div class="k">Contexto regional POWER (aire)</div><div class="v" id="ctx">—</div><div class="s" id="ctxs">dato regional ~50 km: contexto, no tu campo</div></div>
</section>
<section class="card"><div class="k" style="margin-bottom:8px">Fuentes NASA</div><div class="grid" id="sources"></div></section>
<section class="card"><div class="k" style="margin-bottom:8px">Últimas lecturas (más nuevas arriba)</div><div class="tw"><table>
<thead><tr><th>hora (UTC)</th><th>origen</th><th>nodo</th><th>esc.</th><th>A hum/temp</th><th>B hum/temp</th><th>C hum/temp</th>
<th>BME680 T / HR</th><th>DHT11 T / HR</th><th>luz</th><th>banderas</th></tr></thead><tbody id="rows">
<tr><td colspan="11" class="s">cargando…</td></tr></tbody></table></div></section>
</main>
<script>
const $=id=>document.getElementById(id);const B={simulated:"SIMULADO",wokwi:"WOKWI",physical:"FÍSICO"};
const f=(v,d=1,u="")=>v===null||v===undefined?"—":Number(v).toFixed(d)+u;let seen=new Set(),first=true;
function zone(z){if(!z)return"—";const h=z.soil_moisture_vwc!=null?(z.soil_moisture_vwc*100).toFixed(1)+"%":(z.soil_moisture_index_pct!=null?"índ. "+Number(z.soil_moisture_index_pct).toFixed(0)+"% ("+z.soil_moisture_raw+")":(z.soil_moisture_raw!=null?"crudo "+z.soil_moisture_raw:"—"));return h+" / "+f(z.soil_temp_c,1,"°")}
async function j(u){const r=await fetch(u,{cache:"no-store"});if(!r.ok)throw new Error(u+" "+r.status);return r.json()}
async function tick(){
 try{
  const [rd,st,ns]=await Promise.all([j("/api/field-node/readings?limit=50"),j("/api/field-node/status"),j("/api/nasa/status")]);
  $("dot").style.background="var(--ok)";$("conn").textContent="API conectada";
  const c=st.counts_by_source||{};const tot=Object.values(c).reduce((a,b)=>a+b,0);$("total").textContent=tot;
  $("bysrc").textContent=Object.entries(c).map(([k,v])=>(B[k]||k)+": "+v).join(" · ")||"sin lecturas todavía";
  $("last").textContent=st.last_contact?st.last_contact.slice(11,19)+" UTC":"—";
  $("lastsrc").textContent=st.last_contact?(B[st.source]||st.source)+" · "+(st.node_id||""):"esperando el primer envío";
  $("nasaobs").textContent=ns.last_observation_date||"—";$("nasadl").textContent="descarga: "+(ns.last_download||"—").replace("T"," ").slice(0,16)+" UTC (caché)";
  const p=st.power_context||{};$("ctx").textContent=p.t2m_c==null?"—":f(p.t2m_c,1," °C")+" · "+f(p.rh2m_pct,0," %");
  $("ctxs").textContent=(p.date?"NASA POWER "+p.date+" · ":"")+"dato regional ~50 km: contexto, no tu campo";
  $("sources").innerHTML=ns.sources.map(s=>{const cl=s.status==="available"?"ok":s.status==="partial"?"warn":"bad";
   return `<div><div class="${cl}" style="font-weight:650">${s.status}</div><div>${s.name}</div><div class="s">${s.last_observation_date?"obs. "+s.last_observation_date:(s.reason||"")}</div></div>`}).join("");
  const rows=rd.readings.slice().reverse();
  $("rows").innerHTML=rows.length?rows.map(r=>{const z={};(r.internal?.zones||[]).forEach(q=>z[q.id]=q);const e=r.external||{};
   const isNew=!first&&!seen.has(r.id);
   return `<tr class="${isNew?"new":""}"><td>${(r.ts||"").slice(11,19)}</td><td><span class="badge ${r.source}">${B[r.source]||r.source}</span></td>
   <td>${r.node_id}</td><td>${r.scenario||""}</td><td>${zone(z.A)}</td><td>${zone(z.B)}</td><td>${zone(z.C)}</td>
   <td>${f(e.bme680?.t_c,1,"°")} / ${f(e.bme680?.rh_pct,0,"%")}</td><td>${f(e.dht11?.t_c,0,"°")} / ${f(e.dht11?.rh_pct,0,"%")}</td>
   <td>${f(e.ldr?.light_pct,0,"%")}</td><td class="s">${(r.quality_flags||[]).join(", ")}</td></tr>`}).join("")
   :`<tr><td colspan="11" class="s">Todavía no llegó ninguna lectura. Para probar: <code>python -m tools.demo_sender</code></td></tr>`;
  rows.forEach(r=>seen.add(r.id));first=false;
 }catch(e){$("dot").style.background="var(--bad)";$("conn").textContent="API sin respuesta";}
}
tick();setInterval(tick,3000);
</script></body></html>"""


@router.get("/monitor", response_class=HTMLResponse, include_in_schema=False)
def monitor_page():
    return PAGE
