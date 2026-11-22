# Implementation Plan — Phases 4 and 5

## 1. Team workflow backend

- Add V4 tables for team runs, role steps, decisions, persisted events and cursor indexes.
- Add bounded Ollama role port, six fixed roles, context/version validation, failure/timeout/cancel/retry lifecycle, immutable hashed final artifact.
- Add project-scoped authenticated API for create/list/detail/approve/reject/retry/cancel/history.
- Reuse TaskService for atomic task creation; approval only after completed reviewer step.
- Recover interrupted RUNNING runs at startup.

Status: implemented; backend compilation succeeds.

## 2. Realtime event delivery

- Add project-scoped one-use WebSocket tickets and native WebSocket endpoint.
- Publish committed persisted events; provide REST cursor recovery and cap event batch.
- Authenticate ticket issuance using existing Basic auth; bind ticket to project and expiry.

Status: implemented; backend compilation succeeds.

## 3. Angular team workspace

- Add team API service and panel for demand, runs, artifacts, decisions, retry/cancel.
- Consume cursor history on reconnect and authenticated one-use websocket ticket; show connection state.
- Add server-backed activity list and accessible canvas office view, no synthetic transitions.

Status: implemented; Angular production build succeeds.

## 4. Documentation and validation

- Update roadmap, architecture, README, ADR and learning/validation notes.
- Compile backend and build frontend. Tests will follow repository instructions if authorized by the user.

## Boundaries

- Preserve uncommitted Phase 2/3 work. Do not alter V1-V3. Do not add code execution, filesystem tools, auto-approval or unbounded fan-out.
