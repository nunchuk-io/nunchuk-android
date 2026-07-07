---
description: Fetch a Jira ticket, fix it, and (if small + confident) commit, push, and transition to Waiting To Test.
argument-hint: <NUN-XXXX or Jira URL>
---

# Resolve Jira Ticket

Investigate and fix the Jira ticket: **$ARGUMENTS**

## Instructions

### Step 1: Fetch the ticket

Use `mcp__jira__jira_get` on `/rest/api/3/issue/<KEY>` with this `jq` to pull only what you need:

```
{key: key, summary: fields.summary, status: fields.status.name, description: fields.description, attachments: fields.attachment[*].{filename: filename, url: content}}
```

Read the summary, Steps to Reproduce, Expected vs Actual sections, and the Tested Environment build (e.g. `2.5.2.PHASED_ROLL_OUT(329)`).

### Step 2: Locate the affected code

Grep the codebase for strings from the screen name / dialog title / error message in the bug. Read the screen file and its navigation/host activity. Identify the root cause before changing anything.

### Step 3: Decide the path

Apply the fix in-place. **Reuse, don't repeat (DRY)**: before adding a formatter, extension, mapper, string, or constant, grep for an existing one — shared utils live in `nunchuk-core/util/` (e.g. `WalletUtil.kt`, `NumberFormatter.kt`) and reusable UI in `nunchuk-core/.../compose/`. Call the shared method instead of inlining its body, and if you find the same logic in 2+ places, extract it to the nearest shared module rather than copy-pasting or redeclaring a local constant. Then judge:

- **Small + confident** = single-file or tightly localized change, follows existing conventions, you understand why it works, and a manual repro would clearly resolve the bug.
- Anything else (multi-module, behavior change beyond the reported bug, new dependencies, uncertainty about side-effects, failing build/tests, missing repro path) → stop and summarize the diff + questions for the user.

### Step 4: Commit (small + confident path only)

Stage **only** the files you touched for this ticket — never `git add -A` / `git add .`. Inspect `git status` for unrelated dirty files (e.g. `configs/dependencies.gradle`, `.mcp.json`) and leave them alone.

Commit message — follow the repo template exactly (see `git log --oneline -10`):

```
[<KEY>] <ticket summary verbatim, minus any "AND - " / "[Phased Rollout]" prefixes>
```

Use a HEREDOC:

```bash
git commit -m "$(cat <<'EOF'
[NUN-XXXX] Summary line here
EOF
)"
```

Do NOT add Claude/co-author trailers.

### Step 5: Push

```bash
git push
```

If the branch has no upstream, use `git push -u origin <branch>`. Never force-push.

### Step 6: Publish a Firebase App Distribution build

After pushing, build the development debug APK and upload it to Firebase App Distribution so QA can test the fix:

```bash
./gradlew assembleDevelopmentDebug appDistributionUploadDevelopmentDebug
```

This takes several minutes. Wait for `BUILD SUCCESSFUL` and capture the Firebase console URL printed near the end (e.g. `https://console.firebase.google.com/project/.../appdistribution/.../releases/<id>`). Include that link in the final report.

### Step 7: Transition Jira to "Waiting To Test"

The transition ID for "Waiting To Test" on this project is **`41`** (verify once per project with `GET /rest/api/3/issue/<KEY>/transitions` if unsure).

```
mcp__jira__jira_post
  path: /rest/api/3/issue/<KEY>/transitions
  body: {"transition": {"id": "41"}}
```

Then confirm with a `jira_get` filtered to `{key, status: fields.status.name}`.

### Step 8: Report

One short paragraph:
- What was wrong (1 sentence root cause)
- File(s) changed + line range
- Commit hash + that it was pushed
- Firebase App Distribution release URL
- Jira status = Waiting To Test

If you stopped at Step 3, instead report the diff, why you're not confident, and what you need from the user.
