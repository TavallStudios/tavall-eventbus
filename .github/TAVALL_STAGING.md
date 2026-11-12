# Tavall Event Bus Staging Root

```text
<!-- tavall-staging:v1 -->
Type: REPOSITORY_INTEGRATION
State: ACTIVE
Branch: staging/platform
Parent: main
Promotion: MANUAL
ChildMergeTarget: staging/platform
ReactivatedAt: 31fab99 (origin/main)
```

This branch is the combined integration tree for Tavall Event Bus behavior, dependency/package changes, publication, and downstream compatibility. Child merges are integration for combined validation, not production promotion.

Reactivated on 2026-11-12: `staging/platform` had diverged from `main` (stale seed at 57595ae); it was reconciled with `origin/main` (31fab99) so child PRs target a current root. The current child candidate is the house-test style conversion PR (issue #13), which must target this branch.
