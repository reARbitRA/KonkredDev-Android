#!/usr/bin/env python3
"""Deterministic MVP readiness scoring engine — ARBITER-MVP Phase 3.
Run: python3 audit/mc_sim.py pre   |   python3 audit/mc_sim.py post
Seed 424242, N=10000, triangular bands per Phase 3 rubric."""
import json, random, sys, statistics

WHICH = sys.argv[1] if len(sys.argv) > 1 else "pre"

WEIGHTS = {"D1":18,"D2":14,"D3":14,"D4":8,"D5":8,"D6":8,"D7":7,"D8":6,"D9":6,"D10":5,"D11":3,"D12":3}
BASE = {"P0":45,"P1":18,"P2":6,"P3":1.5}
GMULT = {"A":1.00,"B":0.90,"C":0.70,"D":0.45}
HALF = {"A":3,"B":8,"C":15,"D":25}

# finding: (id, dimension, severity, confidence, evidence_grade)
FINDINGS = {
"pre": [
 ("F-EXEC-005","D1","P0",0.95,"B"),
 ("F-DATA-001","D1","P1",0.85,"B"),
 ("F-QUAL-004","D1","P3",1.00,"B"),
 ("F-EXEC-004","D2","P2",0.90,"B"),
 ("F-QUAL-003","D2","P2",1.00,"B"),
 ("F-RELY-003","D2","P2",0.50,"D"),
 ("F-SEC-001","D3","P1",0.90,"B"),
 ("F-SEC-002","D3","P1",0.85,"B"),
 ("F-SEC-003","D3","P2",0.95,"B"),
 ("F-SEC-005","D3","P2",0.80,"B"),
 ("F-DATA-002","D4","P2",1.00,"B"),
 ("F-DATA-003","D4","P2",0.90,"B"),
 ("F-EXEC-001","D5","P1",1.00,"A"),
 ("F-EXEC-002","D5","P1",0.80,"B"),
 ("F-OPS-001","D5","P1",1.00,"A"),
 ("F-OPS-002","D6","P2",0.90,"B"),
 ("F-OBS-001","D7","P2",1.00,"B"),
 ("F-RELY-002","D7","P3",0.90,"B"),
 ("F-RELY-001","D8","P2",0.90,"B"),
 ("F-QUAL-001","D10","P2",1.00,"A"),
 ("F-QUAL-002","D10","P3",1.00,"B"),
 ("F-LEGAL-001","D12","P1",1.00,"A"),
 ("F-LEGAL-002","D12","P2",0.90,"B"),
 # F-EXEC-003 (sandbox limitation) and F-QUAL-004 mapped above; F-EXEC-003 penalizes no dimension (audit env, not repo)
],
"post": [
 # STATE AT HEAD 8901f0a+ (post-remediation). Evidence: grep/structural checks executed (A);
 # NO build/test execution was possible (no JDK/SDK/network; CI push blocked by GitHub App
 # 'workflows' permission -> observed rejection). Fixes therefore remain remediated-UNVERIFIED
 # and keep residual findings; F-EXEC-005 stays P0 until an executed verification exists.
 ("F-EXEC-005R","D1","P0",0.60,"C"),  # model alias fixed in code (A-grep), live call never executed; gemini-flash-latest supported per docs (C)
 ("F-DATA-001R","D1","P2",0.50,"C"),  # rowId fix committed + test written; test never executed
 ("F-QUAL-004R","D1","P3",0.50,"B"),  # SIMULATED labels committed (A-grep); panels remain simulations by design
 ("F-QUAL-003R","D2","P2",0.80,"C"),  # 5 journey tests authored (A-file exists); zero executed
 ("F-RELY-003","D2","P2",0.50,"D"),   # untouched
 ("F-SEC-001R","D3","P2",0.90,"B"),   # header-based key committed (A-grep); APK-embedded key remains until backend proxy (HUMAN T-014)
 ("F-SEC-002R","D3","P3",0.50,"C"),   # WebView hardening committed (A-grep); runtime behavior unverified
 ("F-SEC-005","D3","P2",0.80,"B"),    # untouched (backup policy = product decision)
 ("F-DATA-002","D4","P2",1.00,"B"),   # untouched (migration policy pre-1.0)
 ("F-DATA-003R","D4","P3",0.40,"C"),  # LIKE fix committed (A-grep); unexecuted
 ("F-EXEC-001R","D5","P2",0.60,"C"),  # wrapper committed, jar structurally valid (A: unzip+PK header); full build unexecuted
 ("F-EXEC-002R","D5","P3",0.40,"C"),  # broken debug signingConfig removed (A-read); build unexecuted
 ("F-OPS-001","D5","P1",1.00,"A"),    # CI STILL ABSENT: workflow push rejected (server-side App permission) [E:push stderr]
 ("F-OPS-002","D6","P2",0.90,"B"),    # untouched
 ("F-OBS-001R","D7","P3",0.60,"B"),   # Log.e tags committed (A-grep); no crash reporting
 ("F-RELY-001","D8","P2",0.90,"B"),   # untouched (deferred M4)
 ("F-QUAL-001","D10","P2",1.00,"A"),  # untouched (deferred M4)
 ("F-LEGAL-001","D12","P1",1.00,"A"), # HUMAN: license choice
 ("F-LEGAL-002","D12","P2",0.90,"B"), # HUMAN: privacy policy
],
}

# direct-verification caps per Phase 3.2 (computed from journey/test execution state)
CAPS = {
"pre": {
  "D1": ("journeys", 0, 7),                  # 0 VERIFIED_WORKING of 7 (none executable in sandbox; J5 statically BROKEN)
  "D2": ("exec_failed_and_testcap", 0.0, 0.0),# execution attempted, impossible -> <=30; pass_rate=0, coverage=0 -> cap 0
  "D5": ("exec_failed", None, None),          # documented ./gradlew does not exist -> attempted, failed -> <=30
  "D9": ("grade_CD_only", None, None),        # integration correctness only declared/URL-verified -> <=55
  "D11":("exec_failed", None, None),          # README onboarding command missing -> <=30
},
"post": {
  "D1": ("journeys", 0, 7),                   # still 0 VERIFIED_WORKING: no execution venue (sandbox+CI both blocked); J5 no longer BROKEN (dead model string removed, A-grep) -> PARTIAL-unverified
  "D2": ("exec_failed_and_testcap", 0.0, 0.0), # tests authored but never executed; pass_rate=0; measured_coverage=0 (no coverage tool configured - stated explicitly)
  "D5": ("exec_failed", None, None),           # build still unexecuted anywhere -> <=30
  "D9": ("grade_CD_only", None, None),         # live Gemini contract only doc-verified -> <=55
  "D11":("exec_failed", None, None),           # onboarding flow cannot be executed in sandbox -> <=30
},
}

DOMINANT_GRADE = {
"pre": {"D1":"B","D2":"A","D3":"B","D4":"B","D5":"A","D6":"B","D7":"B","D8":"B","D9":"C","D10":"A","D11":"A","D12":"A"},
"post":{"D1":"C","D2":"A","D3":"B","D4":"B","D5":"A","D6":"B","D7":"B","D8":"B","D9":"C","D10":"A","D11":"A","D12":"A"},
}

def clamp(x, lo=0.0, hi=100.0): return max(lo, min(hi, x))

def triangular(lo, mode, hi, rng):
    if hi <= lo: return mode
    u = rng.random(); c = (mode - lo) / (hi - lo) if hi > lo else 0.5
    return lo + (hi - lo) * (math_sqrt(u * c) if u < c else 1 - math_sqrt((1 - u) * (1 - c)))
def math_sqrt(x): return x ** 0.5

def compute(which):
    findings = FINDINGS[which]; caps = CAPS[which]; grades = DOMINANT_GRADE[which]
    s = {}; detail = {}
    for d in WEIGHTS:
        pen = 0.0; items = []
        for (fid, dim, sev, conf, g) in findings:
            if dim != d: continue
            p = BASE[sev] * conf * GMULT[g]; pen += p
            items.append(f"{fid}[{sev},c={conf},g={g}]={p:.3f}")
        raw = clamp(100 - pen)
        cap_applied = None
        if d in caps:
            kind = caps[d][0]
            if kind == "journeys":
                v, t = caps[d][1], caps[d][2]; cap_applied = 100.0 * v / t
            elif kind == "exec_failed":
                cap_applied = 30.0
            elif kind == "grade_CD_only":
                cap_applied = 55.0
            elif kind == "exec_failed_and_testcap":
                pr, cov = caps[d][1], caps[d][2]
                cap_applied = min(30.0, 100.0 * (0.5 * pr + 0.5 * min(cov / 70.0, 1.0)))
            elif kind == "exec_ci":
                pr, cov = caps[d][1], caps[d][2]
                cap_applied = 100.0 * (0.5 * pr + 0.5 * min(cov / 70.0, 1.0))
        final = raw if cap_applied is None else min(raw, cap_applied)
        s[d] = final
        hw = HALF[grades[d]]
        detail[d] = {"penalty": round(pen,3), "raw": round(raw,3), "cap": None if cap_applied is None else round(cap_applied,3),
                     "s_d": round(final,3), "grade": grades[d], "min": round(clamp(final-hw),3), "max": round(clamp(final+hw),3),
                     "contrib": items}
    R_point = sum(WEIGHTS[d]/100.0 * s[d] for d in WEIGHTS)
    rng = random.Random(424242)
    N = 10000
    sims = []
    for _ in range(N):
        r = sum(WEIGHTS[d]/100.0 * triangular(detail[d]["min"], s[d], detail[d]["max"], rng) for d in WEIGHTS)
        sims.append(r)
    sims_sorted = sorted(sims)
    lo95 = sims_sorted[int(0.025*N)]; hi95 = sims_sorted[int(0.975*N)-1]
    p_go_raw = sum(1 for r in sims if r >= 75) / N
    # hard gates
    p0 = sum(1 for f in findings if f[2]=="P0"); p1 = sum(1 for f in findings if f[2]=="P1")
    p2 = sum(1 for f in findings if f[2]=="P2"); p3 = sum(1 for f in findings if f[2]=="P3")
    gates = []
    p_go = p_go_raw
    if p0 >= 1: p_go = min(p_go_raw, 0.05); gates.append("P0>=1 -> cap 0.05, forced NO-GO")
    if p1 >= 5: p_go = min(p_go, 0.35); gates.append("P1>=5 -> cap 0.35")
    broken = {"pre": True, "post": False}[which]  # pre: J5 BROKEN; post: no journey BROKEN
    if broken: p_go = min(p_go, 0.10); gates.append("journey BROKEN -> cap 0.10")
    grade_letter = (lambda r: "A+" if r>=95 else "A" if r>=90 else "A-" if r>=85 else "B+" if r>=80 else "B" if r>=75 else
                    "B-" if r>=70 else "C+" if r>=65 else "C" if r>=60 else "C-" if r>=55 else "D" if r>=45 else "F")(R_point)
    out = {"which": which, "dimensions": detail, "R_point": round(R_point,2), "R_mean": round(statistics.mean(sims),2),
           "CI95": [round(lo95,2), round(hi95,2)], "P_GO_raw": round(p_go_raw,4), "P_GO": round(p_go,4),
           "counts": {"P0":p0,"P1":p1,"P2":p2,"P3":p3}, "hard_gates": gates or ["NONE"],
           "grade_letter": grade_letter, "N": N, "seed": 424242}
    return out

if __name__ == "__main__":
    res = compute(WHICH)
    print(json.dumps(res, indent=2))
