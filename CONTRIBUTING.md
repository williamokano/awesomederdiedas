# Contributing

## Commit messages

This project uses [Conventional Commits](https://www.conventionalcommits.org). Release versions
and `CHANGELOG.md` are generated from commit messages, so every commit that lands on `main`
must follow the format:

```
<type>(<optional scope>): <description>

<optional body>

<optional footer, e.g. BREAKING CHANGE: ...>
```

| Type | Use for | Release |
| --- | --- | --- |
| `feat` | A new feature | minor |
| `fix` | A bug fix | patch |
| `perf` | A performance improvement | patch |
| `refactor` | Code change that is neither a fix nor a feature | none |
| `docs` | Documentation only | none |
| `test` | Adding or fixing tests | none |
| `build` | Build system or dependencies | none |
| `ci` | GitHub Actions workflows | none |
| `chore` / `style` | Maintenance, formatting | none |
| `revert` | Reverting a previous commit | none |

Add `!` after the type (`feat!: ...`) or a `BREAKING CHANGE:` footer for a major release.

Commits with "none" don't trigger a release on their own. They are included in the changelog
of the next release made by a `feat`, `fix` or `perf` commit.

Examples:

```
feat: add review mode for wrong answers
fix(timer): stop counting while the app is paused
feat(data)!: replace the noun list format
```

### Local check

A `commit-msg` hook rejects messages that don't follow the format. Enable it once per clone:

```bash
git config core.hooksPath .githooks
```

## Pull requests

The **Commit Messages** workflow checks both the PR title and every commit in the PR.

- **Prefer "Rebase and merge" or "Create a merge commit"** so each conventional commit is kept
  in the history and the changelog.
- **If you squash-merge**, GitHub uses the PR title as the commit message, so the PR title must
  follow the same format (e.g. `feat: add review mode`). The check enforces this.

Recommended repository settings (Settings > General > Pull Requests):

- Allow rebase merging and merge commits
- If squash merging stays enabled, set its default commit message to **Pull request title**
