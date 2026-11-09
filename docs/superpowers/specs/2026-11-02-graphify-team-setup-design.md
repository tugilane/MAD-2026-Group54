# Graphify Team Setup Design

> **Superseded (2026-11-09):** decided not to commit `graphify-out/graph.json`. The pre-commit hook, the merge driver and the tracked graph were removed. Each developer runs `graphify update .` locally when needed. This document describes the old setup and is kept for history.


## Intent

Keep the committed Graphify knowledge graph synchronized with the code that produced it, and make graph merges safe across branches.

## Goals

- Rebuild and stage `graphify-out/graph.json` before commits that change the repository.
- Track only the shareable graph; keep Graphify cache and generated views local.
- Register Graphify's union merge driver in each local checkout.

## Non-goals

- Automatically running Graphify on every file save.
- Committing Graphify caches, reports, HTML, or machine-specific metadata.
- Pushing or committing changes from this setup task.

## Chosen approach

Use a committed `.githooks/pre-commit` script and `core.hooksPath=.githooks`. Add `merge=graphify` to `.gitattributes`, and register the driver locally with the Python interpreter used by the installed `graphify` command. This keeps the team behavior reviewable in Git while leaving machine-specific paths in `.git/config`.

## Acceptance

- A staged code change causes the pre-commit hook to run `graphify update .` and stage the refreshed graph.
- Graphify's merge driver is configured locally.
- Only `graphify-out/graph.json` is eligible for version control.
- The Android debug build still succeeds.
