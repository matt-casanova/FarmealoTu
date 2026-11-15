"""Build FarmealoTu plan: generate Gantt SVG, assemble HTML, render PDF with Edge (2 passes for TOC pages)."""
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).parent
OUT_PDF = HERE / "FarmealoTu_Plan_de_Proyecto.pdf"   # salida local (no se versiona); se copia a la raíz
EDGE = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
PARTS = ["p0_head.html", "p1_problem_solution.html", "p2_app_sim.html",
         "p3_proto.html", "p4_team.html", "p5_schedule_rest.html"]

# ---------------------------------------------------------------- Gantt
SHARED = [(0, 0.75, "Inicio"), (3.0, 4.25, "Almuerzo"), (8.0, 8.5, "CP1"),
          (12.0, 12.5, "CP2"), (21, 22, "QA"), (22, 23, "Pitch"), (23, 23.5, "Env."),
          (23.5, 24, "")]
PEOPLE = [
    ("Alvin", "app", [(0.75, 2.0, "POWER diario"), (2.0, 2.5, "Horario"), (2.5, 3.0, "Prueba A"),
                      (4.25, 5.5, "Indicadores"), (5.5, 6.5, "ET0+bal."), (6.5, 8, "Endpoints"),
                      (8.5, 10, "Capas GIBS"), (10, 12, "Despliegue"),
                      (12.5, 16, "Integración · README · form."), (16, 18.5, "Guardia: form. + pitch"),
                      (18.5, 21, "rest")]),
    ("Matías", "app", [(0.75, 1.5, "Mocks"), (1.5, 3, "crops.json"), (4.25, 5.0, "Reglas"),
                       (5.0, 8, "Motor + compare"), (8.5, 10.5, "Bootstrap"), (10.5, 12, "Explic. + nodo"),
                       (12.5, 16, "Pruebas B–E · rendimiento"), (16, 18.5, "rest"),
                       (18.5, 21, "Guardia: correcciones")]),
    ("Kendra", "app", [(0.75, 1.5, "Scaffold"), (1.5, 3, "Inicio + entrada"),
                       (4.25, 8, "Comparar + Ventajas"), (8.5, 12, "Riesgos + Incertidumbre"),
                       (12.5, 16, "Visor + Datos de terreno"), (16, 18.5, "rest"),
                       (18.5, 21, "Guardia: pulido + prueba F")]),
    ("Vanesa", "hw", [(0.75, 2, "Inventario"), (2, 3, "Experim."), (4.25, 6, "Macetas"),
                      (6, 8, "Calibración P1–P2"), (8.5, 10.5, "P3 + ajuste"), (10.5, 12, "QA sim"),
                      (12.5, 16, "Riego · análisis · informe"), (16, 18.5, "Guardia: análisis"),
                      (18.5, 21, "rest")]),
    ("Christian", "hw", [(0.75, 2.5, "Agua + calor"), (2.5, 3, "Forz."), (4.25, 5.5, "physics.py"),
                         (5.5, 8, "LDR · sensores · export"), (8.5, 10.5, "fit + replay"),
                         (10.5, 12, "S2 S3 S5"), (12.5, 16, "Integración física · k"),
                         (16, 18.5, "Guardia: monitoreo"), (18.5, 21, "rest")]),
    ("Mateo", "hw", [(0.75, 3, "Sensores + Wokwi"), (4.25, 6, "Firmware v1"), (6, 8, "Red + cableado"),
                     (8.5, 10.5, "Armado físico"), (10.5, 12, "Calib. + POST"),
                     (12.5, 16, "Estabilidad + demo"), (16, 18.5, "rest"),
                     (18.5, 21, "Guardia: nodo + video")]),
]
MILESTONES = [(8, "CP1 17:00"), (12, "CP2 21:00"), (16, "Congelación 01:00"), (23, "Envío 08:00")]
COL = {"app": "#2a78d6", "hw": "#eb6834", "shared": "#52514e"}


def gantt_svg() -> str:
    W, x0, x1 = 1000, 92, 992
    pph = (x1 - x0) / 24.0
    top, row_h, bar_h = 46, 38, 28
    rows = [("Todos", "shared", SHARED)] + PEOPLE
    H = top + row_h * len(rows) + 30
    X = lambda h: x0 + h * pph
    s = [f'<svg viewBox="0 0 {W} {H}" xmlns="http://www.w3.org/2000/svg" font-family="Segoe UI, sans-serif">',
         '<defs><pattern id="hatch" width="6" height="6" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">'
         '<rect width="6" height="6" fill="#efeee9"/><line x1="0" y1="0" x2="0" y2="6" stroke="#c3c2b7" stroke-width="2"/></pattern></defs>']
    # hour grid + labels
    for h in range(25):
        x = X(h)
        s.append(f'<line x1="{x:.1f}" y1="{top-6}" x2="{x:.1f}" y2="{top + row_h*len(rows)}" stroke="#e1e0d9" stroke-width="1"/>')
        label = f"{(9 + h) % 24:02d}"
        s.append(f'<text x="{x:.1f}" y="{top-10}" text-anchor="middle" font-size="9.5" fill="#898781">{label}</text>')
    s.append(f'<text x="{X(0):.1f}" y="14" font-size="10" fill="#52514e">Sábado 14</text>')
    s.append(f'<text x="{X(15):.1f}" y="14" font-size="10" fill="#52514e">Domingo 15 →</text>')
    # rows
    for i, (name, kind, blocks) in enumerate(rows):
        y = top + i * row_h + (row_h - bar_h) / 2
        if i % 2 == 0:
            s.append(f'<rect x="0" y="{top + i*row_h}" width="{W}" height="{row_h}" fill="#fafaf8"/>')
        s.append(f'<text x="{x0-8}" y="{y + bar_h/2 + 4:.1f}" text-anchor="end" font-size="11.5" font-weight="700" fill="#0b0b0b">{name}</text>')
        # shared blocks behind every person row
        if kind != "shared":
            for a, b, _ in SHARED:
                s.append(f'<rect x="{X(a)+1:.1f}" y="{y:.1f}" width="{(b-a)*pph-2:.1f}" height="{bar_h}" rx="4" fill="#52514e" opacity="0.85"/>')
        for a, b, lab in blocks:
            w = (b - a) * pph - 2
            if lab == "rest":
                s.append(f'<rect x="{X(a)+1:.1f}" y="{y:.1f}" width="{w:.1f}" height="{bar_h}" rx="4" fill="url(#hatch)" stroke="#c3c2b7"/>')
                s.append(f'<text x="{X(a)+1+w/2:.1f}" y="{y + bar_h/2 + 4:.1f}" text-anchor="middle" font-size="10" fill="#52514e">Descanso</text>')
                continue
            fill = COL["shared"] if kind == "shared" else COL[kind]
            s.append(f'<rect x="{X(a)+1:.1f}" y="{y:.1f}" width="{w:.1f}" height="{bar_h}" rx="4" fill="{fill}"/>')
            fs = 8.2 if kind == "shared" else 9.3
            k = 0.5 if kind == "shared" else 0.52
            pad = 2 if kind == "shared" else 6
            if lab and len(lab) * fs * k + pad <= w:
                s.append(f'<text x="{X(a)+1+w/2:.1f}" y="{y + bar_h/2 + 3.5:.1f}" text-anchor="middle" font-size="{fs}" fill="#ffffff">{lab}</text>')
    # milestones
    for h, lab in MILESTONES:
        x = X(h)
        s.append(f'<line x1="{x:.1f}" y1="{top-4}" x2="{x:.1f}" y2="{top + row_h*len(rows) + 4}" stroke="#d03b3b" stroke-width="1.6"/>')
        s.append(f'<text x="{x:.1f}" y="{top + row_h*len(rows) + 18}" text-anchor="middle" font-size="10" font-weight="700" fill="#c53030">{lab}</text>')
    s.append("</svg>")
    return "\n".join(s)


# ---------------------------------------------------------------- assemble + render
def assemble(page_map=None, markers=True) -> str:
    html = "".join((HERE / p).read_text(encoding="utf-8") for p in PARTS)
    html = html.replace("{{GANTT}}", gantt_svg())
    # invisible markers inside each section's h1 so we can locate pages in pass 1
    def add_marker(m):
        sid = m.group(1)
        mk = f'<span style="font-size:1px;color:#fff">@@{sid}@@</span>' if markers else ""
        return m.group(0) + mk
    html = re.sub(r'<div class="section" id="(s\d+)">\s*<h1>', add_marker, html)
    if page_map:
        for sid, pg in page_map.items():
            html = html.replace(f'<span class="pg" data-sec="{sid}"></span>', f'<span class="pg" data-sec="{sid}">{pg}</span>')
    return html


def render(html: str, pdf: Path):
    src = HERE / "plan.html"
    src.write_text(html, encoding="utf-8")
    pdf.parent.mkdir(parents=True, exist_ok=True)
    cmd = [EDGE, "--headless", "--disable-gpu", "--no-pdf-header-footer",
           f"--print-to-pdf={pdf}", src.resolve().as_uri()]
    subprocess.run(cmd, check=True, timeout=180, capture_output=True)


def locate_sections(pdf: Path) -> dict:
    from pypdf import PdfReader
    r = PdfReader(str(pdf))
    found = {}
    for i, page in enumerate(r.pages):
        txt = page.extract_text() or ""
        for sid in re.findall(r"@@(s\d+)@@", txt):
            found.setdefault(sid, i + 1)
    return found, len(r.pages)


if __name__ == "__main__":
    tmp = HERE / "pass1.pdf"
    render(assemble(markers=True), tmp)
    pages, n1 = locate_sections(tmp)
    print("pass1 pages:", n1, pages)
    render(assemble(page_map=pages, markers=False), OUT_PDF)
    pages2, n2 = None, None
    from pypdf import PdfReader
    n2 = len(PdfReader(str(OUT_PDF)).pages)
    print("final pages:", n2, "->", OUT_PDF)
    missing = [f"s{i}" for i in range(14) if f"s{i}" not in pages]
    if missing:
        print("WARNING missing markers:", missing)
        sys.exit(1)
