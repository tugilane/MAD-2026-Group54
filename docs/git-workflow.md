# StrideSet Git Workflow

Our workflow uses short-lived feature branches, pull requests, and merge commits. Everyone contributes through their own Git identity.

## Branches and daily work

- Keep `main` buildable. Submit changes through pull requests.
- Create a branch for each task rather than keeping a permanent personal branch.
- Use descriptive names: `feat/exercise-picker`, `fix/run-pause-distance`, or `docs/setup-guide`.
- Keep branches focused and aim for reviewable changes within 1–2 days.
- Split larger features into smaller working increments.
- Push regularly so work is backed up and visible to teammates.

Start a task from updated `main`:

```bash
git switch main
git pull --ff-only origin main
git switch -c feat/exercise-picker
```

Commit related changes together. Use messages that explain what changed, such as `Persist completed workout sets`. Check the staged diff before committing and avoid including unrelated files.

## Pull requests and review

Open a draft PR early. Include:

- The linked issue and purpose of the change.
- What works and what remains unfinished.
- How the change was tested.
- Screenshots when the UI changes.

Before merging, obtain approval from at least one teammate and pass the relevant build and tests. Resolve review feedback and conflicts on the feature branch.

Rotate reviewers so knowledge spreads across the team. Coordinate shared database, dependency, and build changes with their owner before editing them.

When a feature depends on another change, merge the smallest useful foundation first, then build against updated `main`.

## Updating branches and preserving history

Bring changes from `main` into an active feature branch regularly. With your current work committed, run:

```bash
git fetch origin
git merge origin/main
```

Resolve conflicts carefully, check the resulting changes, and rerun the relevant checks before pushing. Ask the affected teammate when the intended behavior is unclear.

Merge approved PRs using GitHub’s **Create a merge commit** option. This preserves individual commits and records the PR merge point.

After merging:

- Delete the completed feature branch.
- Update local `main`.
- Create a fresh branch for the next task.

Do not force-push shared branches or rewrite existing `main` history. Correct merged mistakes through a new fix or revert PR.

## Team etiquette

- Use your own name and Git email so contributions are attributed correctly.
- Keep issue ownership and PR descriptions current.
- Communicate overlapping work before changing the same files.
- Review teammates’ PRs promptly and explain requested changes.
- Keep secrets, signing keys, local configuration, and generated build files out of commits.
- Don't commit `graphify-out/`. If you use Graphify, build the graph locally with `graphify update .` when you need it.
- Judge contributions through completed work, reviews, and collaboration—not commit counts.

Configure `main` protection to require pull requests, one approving review, and passing automated checks once those checks are available. These guidelines alone do not configure GitHub settings.
