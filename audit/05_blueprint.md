# EXECUTION BLUEPRINT — KonkredDev-Android @ cb16706 (MVP → GO)

## 1. Executive summary (≤200 words)
The repo is a single-commit, AI-Studio-generated Android "VS Code-style" editor (Kotlin/Compose, ~4.2k LOC) whose flagship AI integration pointed at a Gemini model retired in Sept 2025, whose documented build command could not run (no Gradle wrapper), whose debug build referenced a missing keystore, and which had zero CI, zero LICENSE, zero product-logic tests, and a silent data-loss bug on newly created files. Target state: buildable-from-clone, CI-green, journey-tested, legally shippable MVP. Gap closed this session: 10 of 16 blueprint tasks executed as commits (AI endpoint, wrapper, signing, WebView hardening, persistence bug, LIKE bug, PII, labels, logging, tests authored). Gap that remains is **verification-shaped, not code-shaped**: no JDK/SDK/network in the audit sandbox and the GitHub App lacks `workflows` permission, so nothing was executed; plus four human-gated items (CI permission, license, privacy policy, backend key proxy / real API key). Readiness moved 49.29 → 54.05 but P_GO stays 0.00% under the unverified-P0 hard gate until CI runs green and J5 is live-verified.

## 2. Critical path DAG (longest path ★)
```
T-001(model fix)──┬─>★T-015(live J5 verify · HUMAN key)
T-002(wrapper)──>★T-003(debug signing)──>★T-004(CI · HUMAN permission)──>★T-011(journey tests EXECUTED)──> M2 re-score ──> M3
T-005(rowId fix)──────────────────────────────────────────────┘
T-006..T-010 (parallel, done)          T-012/T-013 (HUMAN legal, parallel)      T-014 (HUMAN backend, parallel)      T-016 (M4 backlog)
```
Critical-path remaining hours (human-unblocked): T-004 0.5h + T-011 CI iteration 1h + T-015 0.5h ≈ 2h after permissions/key exist.

## 3. Milestones
| M | Exit gate | Tasks | Status |
|---|---|---|---|
| M0 Unblock | P0==0 (verified) | T-001 | code DONE; certification BLOCKED on execution venue |
| M1 De-risk | P1≤2, no journey BROKEN | T-002,T-003,T-004,T-005,T-006,T-011 | P1 7→2 DONE; T-004 HUMAN-BLOCKED |
| M2 Harden | R_point≥75 | T-007,T-008,T-010 (+T-012/T-014 human) | partial; R=54.05 |
| M3 Launch | P_GO≥0.85 | T-013,T-015 | BLOCKED (human) |
| M4 Backlog | — | T-016 (debounce, cursor, god-files, migrations, backup rules, release docs, coverage tool) | DEFERRED |

Full task JSON (schema-compliant, every task → source_findings): `audit/05_blueprint.json`.

## 4. Projected readiness curve (recomputed via audit/mc_sim.py, not hand-waved)
| stage | R_point | P_GO | binding constraint |
|---|---|---|---|
| pre (cb16706) | 49.29 | 0.00% | P0 dead-model; D1/D2 execution caps = 0 |
| post-commits (HEAD) | 54.05 | 0.00% | unverified-P0 gate; D1/D2 caps still 0 |
| after T-004+T-011 execute green (projected) | ~72–78* | ~0.10–0.35* | D1 cap 5/7, D2 cap 50 (no coverage tool), F-OPS-001 closed |
| after T-012+T-013+T-015 (projected) | ~80–85* | ≥0.60* | D12 → ~95, D9 cap lifts with live verify |
*projection arithmetic: caps only (D1 71.4, D2 50, D5 raw ~95, D9 raw ~85, D11 raw ~95, D12 ~95, others ≈ current); Monte Carlo NOT run on projections — they are planning figures, flagged as such.

## 5. Test strategy
Authored (T-011, Robolectric/JVM, `CodeEditorViewModelTest`): J1 seed+auto-open, J2 edit-persist, J3 create-file real-id+persist (regression for F-DATA-001), J6 global replace, J7 terminal sim. Execution venue: CI `testDebugUnitTest`. J4 (WebView) needs instrumented/emulator test — deferred M4. J5 live path needs owner API key (T-015); offline-mock branch is unit-testable. Coverage target once Kover/Jacoco added in M4: ≥70% VM+data layer.

## 6. Human-required items (hard ceiling)
| task | what a human must provide | unblocking effort |
|---|---|---|
| T-004 CI | grant Arena GitHub App `workflows` permission (or copy audit/artifacts/android-ci.yml into .github/workflows/ manually) | 5 min |
| T-012 LICENSE | owner's license choice (Apache-2.0 recommended for this stack) | 30 min |
| T-013 Privacy | legal review/publication of privacy policy + AI data disclosure | 2 h |
| T-014 Key proxy | cloud account + product decision (backend proxy vs restricted client key) | 8 h |
| T-015 Live J5 | real GEMINI_API_KEY (paid/limited account) + device smoke test | 30 min |

## 7. Risk register (top 10)
| # | risk | L×I | mitigation | owner |
|---|---|---|---|---|
| 1 | Unexecuted commits don't compile (AGP 9.1.1/Robolectric/KSP interplay unknown) | M×H | run T-004 CI immediately; fixes are small & isolated | human+agent |
| 2 | gemini-flash-latest alias behavior changes | L×M | pin explicit version after live verification (T-015) | dev |
| 3 | APK-embedded API key abuse | M×H | T-014 proxy; interim: restrict key by package+cert | owner |
| 4 | Robolectric SDK36 jars unavailable in CI | L×M | robolectric 4.16.1 declares SDK 36 support; fallback @Config sdk 35 | dev |
| 5 | Destructive migration wipes user data at v2 | M×H | F-DATA-002 in M4 before any schema change | dev |
| 6 | Legal exposure: no license/privacy | M×H | T-012/T-013 | owner |
| 7 | Simulated panels misread as real features | M×M | labels shipped (T-009); store listing copy review | owner |
| 8 | Per-keystroke DB writes on large files | H×L | M4 debounce | dev |
| 9 | Cursor/selection defects in editor (F-RELY-003, grade D) | M×M | instrumented UI test in M4 | dev |
| 10 | Single-contributor bus factor | M×M | docs+CI+tests this audit adds | owner |

## 8. Definition of Done (binary, machine-checkable)
- [ ] `./gradlew assembleDebug testDebugUnitTest` exits 0 in CI (T-004+T-011)
- [ ] CI badge green on default branch
- [ ] `grep -c gemini-1.5 app/src -r` == 0 ✅ done
- [ ] `grep -rc ari_eshghi app/src` == 0 ✅ done
- [ ] `test -x gradlew && test -f gradle/wrapper/gradle-wrapper.jar` ✅ done
- [ ] `test -f LICENSE` (T-012)
- [ ] Live 200 from Gemini endpoint with owner key (T-015)
- [ ] All 5 journey tests pass in CI (T-011 executed)
- [ ] P0 count == 0 **with executed verification**

## 9. Launch runbook (updated to reality)
1. Human: grant workflows permission → push audit/artifacts/android-ci.yml to .github/workflows/. 2. Watch first CI run; triage compile/test failures (agent can iterate). 3. Owner: add LICENSE + privacy policy; create restricted GEMINI_API_KEY; put it in CI/org secrets as needed for a device smoke test. 4. Smoke: install debug APK on emulator/device — launch (J1), edit+restart (J2), create file+restart (J3), preview toggle (J4), ask AI with real key (J5), replace-all (J6), terminal help/ls (J7). 5. Rollback trigger: any journey crash or AI 4xx/5xx — revert merge commit of this branch (single-PR rollback path; no DB migrations shipped, Room v1 unchanged → no data migration rollback needed). 6. Monitor: logcat tags `GeminiApi`, `CodeEditorVM`; add Crashlytics before public beta (M4).
