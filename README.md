# DeepResearchAiAgent

[![CI](https://github.com/ermawqa/DeepResearchAIAgent/actions/workflows/ci.yml/badge.svg)](https://github.com/ermawqa/DeepResearchAIAgent/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![JDK](https://img.shields.io/badge/JDK-17%2B-437291?logo=openjdk&logoColor=white)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A command-line research agent in Kotlin. Give it a topic; it returns a structured
briefing — key concepts, active research directions, tooling, and open questions —
as Markdown, JSON, or plain text.

```console
$ deep-research --query "Retrieval-augmented generation" --format markdown
Researching "Retrieval-augmented generation" with gpt-4o-mini…
Saved markdown report to research_output.md
Used 901 tokens.
```

See [`examples/`](examples/) for a full sample report.

---

## Why this exists

A chat interface gives you a wall of prose that reads differently every time you
ask. This tool constrains the model to a fixed section contract, so output is
comparable across topics, diffable in git, and archivable as JSON with the model
and token cost recorded alongside it.

## Features

| | |
|---|---|
| **Structured output** | A system prompt pins six sections, so every report has the same shape. |
| **Three formats** | `markdown` for reading, `json` for tooling, `raw` for piping. |
| **Typed error handling** | Auth, rate-limit, network, and API failures are distinct types with distinct exit codes. |
| **Automatic retries** | Exponential backoff on 5xx and 429, honouring the server's `Retry-After`. |
| **Provider-agnostic core** | The agent depends on an `LlmClient` interface, not on OpenAI. |
| **Environment-driven config** | Model, base URL, and timeout are all overridable without a rebuild. |
| **Tested without a network** | 57 tests run against a local `MockWebServer` — no API key, no cost. |

## Getting started

**Requirements:** JDK 17 or newer. Gradle comes with the wrapper.

```bash
git clone https://github.com/ermawqa/DeepResearchAIAgent.git
cd DeepResearchAIAgent
export OPENAI_API_KEY="sk-..."
./gradlew run --args="--query 'Retrieval-augmented generation'"
```

To get a real `deep-research` command instead of going through Gradle:

```bash
./gradlew installDist
./build/install/deep-research/bin/deep-research --query "Vector databases"
```

Run with no `--query` and it prompts for one.

## Usage

```
deep-research [OPTIONS]

  -q, --query <text>          Topic to research. Prompted for if omitted.
  -f, --format <format>       markdown | json | raw (default: markdown)
  -o, --output <path>         Write here instead of the format default.
  -m, --model <name>          Override the model for this run.
  -t, --temperature <0.0-2.0> Sampling temperature (default: 0.3).
  -h, --help                  Show usage.
  -v, --version               Show the version.
```

```bash
# Archive a report as JSON, in a directory that doesn't exist yet
deep-research -q "Kotlin coroutines internals" -f json -o reports/coroutines.json

# Pipe plain text into another tool — progress messages go to stderr
deep-research -q "Vector databases" -f raw > notes.txt
```

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `OPENAI_API_KEY` | *(required)* | API credential. |
| `OPENAI_MODEL` | `gpt-4o-mini` | Default model. |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` | Point at a proxy or an OpenAI-compatible provider. |
| `OPENAI_TIMEOUT_SECONDS` | `60` | Per-request timeout. |

### Exit codes

Failures are distinguishable from a shell script — a rate limit is worth retrying,
a bad key is not.

| Code | Meaning | Code | Meaning |
|---|---|---|---|
| `0` | Success | `5` | Rate limited |
| `2` | Bad usage | `6` | Network failure |
| `3` | Missing configuration | `7` | API / malformed response |
| `4` | Authentication failed | `8` | Could not write output |

## Architecture

The dependency arrow points one way: the agent knows about an interface, and the
OpenAI client is plugged in at the entry point. Swapping providers or testing the
agent means substituting one implementation — no agent code changes.

```
                      ┌──────────────┐
   args ─────────────▶│    Main      │  wiring + exit codes
                      └──────┬───────┘
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
      ┌───────────┐   ┌────────────┐  ┌───────────┐
      │ ArgParser │   │ AppConfig  │  │ Report    │
      │  (cli)    │   │  (config)  │  │ Writer    │
      └───────────┘   └────────────┘  └─────┬─────┘
                                            │ renders via
                             ┌──────────────┴──────────────┐
                             ▼                             ▼
                    ┌─────────────────┐          ┌──────────────────┐
                    │ ResearchAgent   │          │ Markdown / Json  │
                    │    (agent)      │          │ / Raw Renderer   │
                    └────────┬────────┘          └──────────────────┘
                             │ depends on the interface
                             ▼
                    ┌─────────────────┐
                    │   LlmClient     │  ◀── fake impls in tests
                    └────────┬────────┘
                             │ implemented by
                             ▼
                    ┌─────────────────┐
                    │  OpenAIClient   │  HTTP, retries, error mapping
                    └─────────────────┘
```

| Package | Responsibility |
|---|---|
| `cli` | Argument parsing, usage text, exit codes. |
| `config` | Environment-driven configuration with validation. |
| `llm` | Provider port, typed failures, OpenAI implementation, wire DTOs. |
| `agent` | Prompt construction and research orchestration. |
| `output` | Rendering to each format and routing to file or stdout. |

Design decisions and their trade-offs are written up in [`docs/DESIGN.md`](docs/DESIGN.md).

## Development

```bash
./gradlew build          # compile, test, and check
./gradlew test           # tests only
./gradlew installDist    # build the launcher script
```

Warnings are errors (`allWarningsAsErrors`), and CI runs the suite on JDK 17 and 21.

The tests cover argument parsing edge cases, configuration precedence, HTTP error
mapping and retry behaviour, prompt construction, and rendering — all against a
local mock server, so `./gradlew build` needs no API key.

## Limitations

Worth being direct about what this does and does not do:

- **No retrieval.** The agent queries a language model's parametric knowledge. It
  has no web access, so it cannot answer questions about recent events, and its
  "Further Reading" section suggests search terms rather than verified sources.
- **No citations.** The prompt forbids inventing them, which is a mitigation, not
  a fix. Claims should be verified before being relied on.
- **Single turn.** There is no memory between runs and no follow-up refinement.

## Roadmap

- [ ] Retrieval-augmented mode: a search/fetch tool loop before generation
- [ ] Real citations with source URLs
- [ ] Conversational follow-ups over a persisted session
- [ ] Anthropic and local (Ollama) `LlmClient` implementations
- [ ] Response streaming for faster time-to-first-token

## License

[MIT](LICENSE) © Yermukhamed Shakhman
