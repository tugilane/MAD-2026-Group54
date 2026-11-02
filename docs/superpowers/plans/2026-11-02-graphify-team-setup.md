# Graphify Team Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the repository ready to share Graphify's code graph through Git with automatic pre-commit refreshes and safe graph merges.

**Architecture:** Commit the assistant integration and a small shared pre-commit hook. Keep the merge driver's interpreter path local in Git config, while `.gitattributes` advertises the shared `graphify` driver name.

**Tech Stack:** Graphify CLI, POSIX shell, Git hooks, Git attributes, Gradle Android build.

**Spec:** `docs/superpowers/specs/2026-11-02-graphify-team-setup-design.md`

## Global Constraints

- Track only `graphify-out/graph.json`; ignore Graphify cache, reports, HTML, and machine-specific metadata.
- Rebuild with `PYTHONHASHSEED=0 graphify update .` before commits that change staged repository files.
- Do not commit or push from this task.

## Review Focus

- Graphify is not installed: the hook should fail clearly instead of silently committing a stale graph.
- Only the graph is staged by the hook: unrelated staged files remain untouched.
- A commit that changes only `graph.json` does not recursively rebuild the graph.
- A checkout on another machine needs local Git config for the merge driver and hooks path.
- The generated graph remains valid after the update and the Android build remains green.

### Task 1: Add shared Git automation

**Files:**
- Modify: `.gitattributes`
- Create: `.githooks/pre-commit`

**Interfaces:**
- Consumes: installed `graphify` CLI and staged Git changes.
- Produces: a refreshed and staged `graphify-out/graph.json` before commits.

- [ ] Add `merge=graphify` to the existing `graphify-out/graph.json` attribute rule.
- [ ] Create an executable POSIX pre-commit hook that skips when `GRAPHIFY_SKIP_HOOK=1`, when no graph exists, or when no non-graph files are staged; otherwise run deterministic `graphify update .` and `git add graphify-out/graph.json`.
- [ ] Run `sh -n .githooks/pre-commit` and verify it exits successfully.

### Task 2: Register local Git integration

**Files:**
- Modify: local `.git/config` only; do not commit it.

**Interfaces:**
- Consumes: `.githooks/pre-commit` and the interpreter path from `command -v graphify`.
- Produces: `core.hooksPath=.githooks` and `merge.graphify.driver` in local Git config.

- [ ] Register `core.hooksPath`.
- [ ] Register the Graphify merge driver using the installed Graphify interpreter.
- [ ] Verify both settings and `graphify hook status`.

### Task 3: Refresh and verify

**Files:**
- Modify: `graphify-out/graph.json` and local ignored Graphify artifacts.

- [ ] Run `graphify update .` and confirm the graph is rebuilt.
- [ ] Stage only the intended repository files, run the pre-commit hook, and confirm the graph is staged without staging `.DS_Store`.
- [ ] Run `bash gradlew assembleDebug`.
- [ ] Run `git diff --cached --check` and inspect the final staged file list.
