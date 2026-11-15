#!/usr/bin/env bash
# Checks that the agent workflow files agree with each other: every agent and skill parses,
# every skill an agent preloads exists, every agent or skill a doc names exists, and the model
# tables in the docs match the agent files. Run it after changing anything under .claude/,
# AGENTS.md or docs/agents/. It checks the files, not behaviour: see README, "Troubleshooting".
set -u
cd "$(dirname "$0")/.."
python3 - <<'PY'
import glob, os, re, sys

errors = []
MODELS = {"haiku", "sonnet", "opus", "fable", "inherit"}

def frontmatter(path):
    text = open(path).read()
    if not text.startswith("---\n"):
        errors.append(f"{path}: frontmatter must start on line 1"); return {}, text
    head, _, body = text[4:].partition("\n---\n")
    fields, key = {}, None
    for line in head.splitlines():
        if re.match(r"^\s+- ", line) and key:
            fields[key] = fields.get(key) or []
            fields[key].append(line.split("- ", 1)[1].strip())
        elif ":" in line:
            key, _, value = line.partition(":")
            key = key.strip(); fields[key] = value.strip()
            if key == "description" and ": " in value and not value.strip().startswith(('"', "'")):
                errors.append(f"{path}: description has an unquoted ': ', which strict YAML rejects")
    return fields, body

skills = {}
for path in sorted(glob.glob(".claude/skills/*/SKILL.md")):
    f, body = frontmatter(path)
    folder = os.path.basename(os.path.dirname(path))
    if f.get("name") != folder: errors.append(f"{path}: name '{f.get('name')}' must match folder '{folder}'")
    if not f.get("description"): errors.append(f"{path}: no description")
    if len(f.get("description", "")) > 1536: errors.append(f"{path}: description over 1536 characters")
    if body.count("\n") > 500: errors.append(f"{path}: over 500 lines")
    skills[folder] = f

agents = {}
for path in sorted(glob.glob(".claude/agents/*.md")):
    f, body = frontmatter(path)
    name = os.path.basename(path)[:-3]
    if f.get("name") != name: errors.append(f"{path}: name '{f.get('name')}' must match file name")
    if not f.get("description"): errors.append(f"{path}: no description")
    model = f.get("model", "")
    if not model: errors.append(f"{path}: no model set, so it would inherit the session's")
    elif model not in MODELS and not model.startswith("claude-"): errors.append(f"{path}: unknown model '{model}'")
    tools = [t.strip() for t in f.get("tools", "").split(",")]
    preload = f.get("skills") or []
    for s in preload:
        if s not in skills: errors.append(f"{path}: preloads missing skill '{s}'")
        elif skills[s].get("disable-model-invocation") == "true": errors.append(f"{path}: '{s}' cannot be preloaded")
    named = set(re.findall(r"`([a-z]+(?:-[a-z]+)+)`", body)) & (set(skills) | {"run"})
    if (named - set(preload)) and "Skill" not in tools:
        errors.append(f"{path}: names skills {sorted(named - set(preload))} but has no Skill tool")
    agents[name] = f

docs = ["README.md", "AGENTS.md", *glob.glob("*/AGENTS.md"), *glob.glob("docs/agents/*.md"),
        *glob.glob(".claude/commands/*.md"), *glob.glob(".claude/agents/*.md"), *glob.glob(".claude/skills/*/SKILL.md")]
known = set(agents) | set(skills)
suffixes = ("-implementer", "-reviewer", "-architect", "-verifier")
prefixes = ("mobile-", "backend-", "admin-")
for path in docs:
    text = open(path).read()
    for token in set(re.findall(r"`([a-z]+(?:-[a-z]+)+)`", text)):
        looks_like = token.endswith(suffixes) or (token.startswith(prefixes) and "/" not in token)
        if looks_like and token not in known:
            errors.append(f"{path}: names `{token}`, which is neither an agent nor a skill")
    # A table row that starts with an agent name and carries a model must match the agent file.
    for row in re.findall(r"^\| `([a-z-]+)` \|(.*)$", text, re.M):
        name, rest = row
        if name in agents:
            found = [m for m in re.findall(r"`([a-z]+)`", rest) if m in MODELS]
            if found and found[0] != agents[name].get("model"):
                errors.append(f"{path}: says {name} runs on {found[0]}, the agent file says {agents[name].get('model')}")

for path in glob.glob(".claude/commands/*.md"):
    frontmatter(path)

if errors:
    print("check-agents: FAILED"); [print("  " + e) for e in errors]; sys.exit(1)
print(f"check-agents: {len(agents)} agents, {len(skills)} skills, {len(docs)} docs agree")
PY
