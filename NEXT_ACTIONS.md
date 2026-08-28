# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 3/3 (100.0%)
- **Function parity:** 52/76 matched (target 142) — 68.4%
- **Class/type parity:** 12/19 matched (target 41) — 63.2%
- **Combined symbol parity:** 64/95 matched (target 183) — 67.4%
- **Average inline-code cosine:** 0.39 (function body across 3 matched files)
- **Average documentation cosine:** 0.23 (doc text across 3 matched files)
- **Cheat-zeroed Files:** 0
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
- **Lint issues:** 4

### 2. unix

- **Target:** `filedescriptor.Unix`
- **Similarity:** 0.35
- **Dependents:** 0
- **Priority Score:** 63906.5
- **Functions:** 32/35 matched (target 75)
- **Missing functions:** `drop`, `probe_handle_type`, `new`
- **Types:** 1/4 matched (target 7)
- **Missing types:** `HandleType`, `RawFileDescriptor`, `SocketDescriptor`
- **Lint issues:** 8

### 3. lib

- **Target:** `filedescriptor.Lib`
- **Similarity:** 0.63
- **Dependents:** 0
- **Priority Score:** 12103.7
- **Functions:** 9/9 matched (target 32)
- **Missing functions:** _none_
- **Types:** 11/12 matched (target 33)
- **Missing types:** `Result`

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

