# CLAUDE.md

This repository is a fork of [cashapp/paparazzi](https://github.com/cashapp/paparazzi). It publishes under the `com.eygraber` group. The plugin id is `com.eygraber.paparazzi`.

## Branches

| Branch     | Purpose                                                       |
|:-----------|:--------------------------------------------------------------|
| `master`   | Upstream syncs only. It mirrors `cashapp/paparazzi` `master`. |
| `eygraber` | The default branch. Target all changes here.                  |

Do not commit fork changes to `master`. Sync it from upstream only.

## Pull upstream work

1. Sync upstream `master` into `master`.
2. Rebase `eygraber` onto `master`.

## Publish

- When a PR merges into `eygraber`, the `build` workflow publishes a snapshot. The snapshot version comes from `VERSION_NAME` in [gradle.properties](/gradle.properties).
- To release a version, run the `release` workflow on `eygraber` with `workflow_dispatch`. Its required `number` input selects the release in the current series. The series prefix is hardcoded in [release.yml](/.github/workflows/release.yml).
