# Personas — SynKra

Four personas carry stories. The CISO and the Product Owner appear only as constraints (see the
end of this file). The SynKra agent is a system actor, not a persona: its behavior is specified
in the acceptance criteria of the human personas' stories.

Source: `entradas/prd.md` §3 and §7, `entradas/icp.md`, `entradas/pvb.md` §2.

---

## P-ANA — Compliance Analyst

| Field | Value |
|---|---|
| Role | Receives a regulatory circular, interprets it, and registers it as a structured change |
| Goal | Know exactly which plugin and extension point a change touches, and get a draft ready, without forgetting any country |
| Frustration | Nobody knows with certainty which countries a change applies to; the answer lives in two or three people who rotate |
| Trigger | A new norm with a deadline; an audit that finds a country missing a change |
| Interface | CLI (`synkra analyze`, `synkra propose`), YAML files, report files |
| Authority | Decides what changes; running `synkra propose` is the approval of the localization report. Cannot merge |
| Verifies | The report lists every plugin, affected and discarded, each with a reason |
| Success metric | Recall of affected plugins above 90 % on the evaluation dataset |

## P-OPS — DevOps Engineer

| Field | Value |
|---|---|
| Role | Brings up the cluster and deploys the product for each country |
| Goal | Opening a new country is adding a plugin and an overlay, not cloning a repository |
| Frustration | Per-country branches or images; differences between Windows and Linux environments |
| Trigger | The business opens operations in a new country |
| Interface | `make cluster`, `make deploy COUNTRY=xx`, `kubectl`, Argo CD |
| Authority | Owns the cluster, namespaces, network policies and secrets |
| Verifies | `kubectl kustomize` renders each overlay; only the country's plugin runs in its namespace |
| Success metric | A new country deploys in under 10 minutes on kind |

## P-LEAD — Tech Lead (owner of protected branches)

| Field | Value |
|---|---|
| Role | Reviews and merges pull requests; owns branch protection; holds one of the two trust vetoes |
| Goal | Know what the agent can write, and prove it from server configuration rather than a slide |
| Frustration | An agent that could merge, approve its own work, or touch `main` |
| Trigger | A draft PR from the agent; a request to install the agent |
| Interface | GitHub (PR review, branch protection API), `gh`, `kubectl auth can-i` |
| Authority | Final approval of every agent output. Only a human merges |
| Verifies | `main` requires review; the agent's token reaches `synkra/*` and drafts only; the agent's ServiceAccount cannot write to the cluster |
| Success metric | Zero agent writes outside `synkra/*`; approved PRs later reverted below 2 % |

## P-DEV — Plugin Developer

| Field | Value |
|---|---|
| Role | Writes and maintains the country plugins (`co`, `pe`) and the extension-point contracts |
| Goal | Add or change country behavior without touching the kernel or another country's plugin |
| Frustration | Country logic leaking into the core; rules copied by hand between countries |
| Trigger | A draft PR from the agent lands in a plugin; a new country is requested |
| Interface | Maven modules, plugin manifests, unit tests |
| Authority | Owns plugin code; reviews diffs the agent drafts in plugins |
| Verifies | The kernel has no country words; each plugin builds alone; each rule cites its source |
| Success metric | PRs approved without substantial rewriting above 50 % |

---

## Constraints (not personas)

- **CISO.** Holds the first trust veto: "does the source code leave my perimeter?" In `claude`
  mode it does (accepted trade-off, requirement NFR-09). Stories US-1.10 to US-1.12 and US-2.16
  give the CISO verifiable evidence of what the agent can reach.
- **Product Owner.** An indirect user. In the original vision the Product Owner received story
  drafts; that capability is outside the scope of this MVP (requirements, decision Q1-B).

## Persona-to-epic map

| Persona | E1 Analyst journey | E2 Operator journey | E3 Abandonment | E4 Agent cannot resolve |
|---|---|---|---|---|
| P-ANA | Primary | | Primary | Primary |
| P-OPS | | Primary | | Supporting |
| P-LEAD | Primary | Supporting | | Supporting |
| P-DEV | Supporting | Primary | | Supporting |
