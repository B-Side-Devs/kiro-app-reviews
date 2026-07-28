# Product

**kiro-app-reviews** is a collaborative app-review platform that lets teams record, annotate, and share UX sessions on live URLs. Users (PROJECT_ADMIN, CLIENT, PLATFORM_ADMIN) work within a Workspace → Project → Review Session hierarchy.

A **Review Session** is the central aggregate. It goes through a defined lifecycle:

```
DRAFT → RECORDING → COMPLETED ↔ REOPENED → ARCHIVED → DELETED (soft, terminal)
```

Sessions can carry metadata (name, description, targetUrl) and will eventually hold artifacts (rrweb recordings, DOM snapshots, comments, voice notes, AI results). Notifications fire on key domain events.

## Domain vocabulary

| Abbreviation | Full name |
|---|---|
| `iam` | Identity & Access Management |
| `wspr` | Workspaces, Projects & Invitations |
| `rs` | Review Session (domain core) |
| `art` | Artifacts |
| `notif` | Notifications |
| `boff` | Backoffice |

## Current status

The MVP exposes the Review Session core API (create, get, list, state transitions). `art`, `notif`, and `boff` packages are scaffolded but not implemented. The frontend (Next.js) is not yet in scope.
