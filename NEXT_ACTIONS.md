# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/3 (66.7%)
- **Function parity:** 43/67 matched (target 142) — 64.2%
- **Class/type parity:** 1/7 matched (target 9) — 14.3%
- **Combined symbol parity:** 44/74 matched (target 151) — 59.5%
- **Average inline-code cosine:** 0.10 (function body across 2 matched files)
- **Average documentation cosine:** 0.00 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 2 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. windows

- **Target:** `filedescriptor.Windows`
- **Similarity:** 0.20
- **Dependents:** 0
- **Priority Score:** 243508.0
- **Functions:** 11/32 matched (target 35)
- **Missing functions:** `default`, `as_raw_file_descriptor`, `into_raw_file_descriptor`, `from_raw_file_descriptor`, `as_socket_descriptor`, `into_socket_descriptor`, `from_socket_descriptor`, `probe_handle_type`, `drop`, `dup_impl`, `as_stdio_impl`, `as_file_impl`, `set_non_blocking_impl`, `redirect_stdio_impl`, `read`, `write`, `flush`, `new`, `socketpair_impl`, `poll_impl`, `socketpair`
- **Types:** 0/3 matched (target 1)
- **Missing types:** `RawFileDescriptor`, `SocketDescriptor`, `HandleType`
- **Tests:** 0/1 matched

### 2. unix

- **Target:** `filedescriptor.Unix [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 63910.0
- **Functions:** 32/35 matched (target 107)
- **Missing functions:** `drop`, `probe_handle_type`, `new`
- **Types:** 1/4 matched (target 8)
- **Missing types:** `HandleType`, `RawFileDescriptor`, `SocketDescriptor`
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

