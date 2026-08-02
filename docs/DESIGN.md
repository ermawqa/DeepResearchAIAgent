# Design notes

Why this project is built the way it is, and where it falls short.

---

## What separates a research agent from "search and summarise"

A summarising tool retrieves the top results for a query and compresses them. That
is a useful thing, but it inherits whatever the ranker put first and flattens
disagreement into a single confident voice.

The behaviour worth aiming at is closer to a junior researcher:

- **Context over keywords.** Understanding what the question is *for*, not just
  which documents contain its terms.
- **Synthesis across sources.** Surfacing patterns and contradictions between
  sources, rather than paraphrasing each in turn.
- **Structured insight.** Producing an artifact with a shape — concepts, open
  questions, next steps — instead of undifferentiated prose.
- **Multi-step reasoning.** Decomposing a topic, following up on what the first
  pass revealed, and revising accordingly.

This implementation delivers the third property and part of the first. It does not
yet do genuine cross-source synthesis or multi-step reasoning, because it has no
retrieval — see [Known limitations](#known-limitations). The section contract in
`ResearchPrompt` is what buys the structure: by fixing the output shape in a system
message, the report becomes comparable across topics and diffable in version
control, which is the precondition for evaluating it at all.

## How you would evaluate it

Output quality here is not a single number. A workable rubric mixes what can be
checked mechanically with what needs a human:

| Dimension | Question | How to measure |
|---|---|---|
| **Relevance** | Does it answer the question actually asked? | Human rating on a held-out topic set. |
| **Depth** | Does it go past what a first search result would say? | Blind comparison against a search-and-summarise baseline. |
| **Coherence** | Is it organised and internally consistent? | Structural checks plus human rating. |
| **Calibration** | Does it flag uncertainty rather than assert through it? | Count hedges on topics deliberately outside the model's knowledge cutoff. |
| **Faithfulness** | Are claims verifiable and unfabricated? | Fact-check a sample; count invented citations. |

What is cheap to test automatically — and is tested here — is the *scaffolding*:
that the prompt actually demands the six sections, that the request is built
deterministically from a query, and that failures never reach the renderer. Note
what this does not establish: nothing asserts that a real model's reply conforms
to the contract, because that needs a live call. Validating the response against
the contract is listed under [Known limitations](#known-limitations).

Judging the content itself needs human judgement or an LLM-as-judge — and a judge inherits
its own model's biases, which is why it is a supplement to human review rather
than a replacement for it.

The trap worth naming: it is easy to optimise for reports that *read* authoritative.
Fluency and accuracy are uncorrelated, and a rubric that only rewards the former
will happily select for confident fabrication.

## Where it will fail

Concretely, and in rough order of likelihood:

1. **Anything time-sensitive.** "What did the IMF announce yesterday?" has no good
   answer from parametric knowledge. The model may not even recognise that the
   question is outside what it can know.
2. **Specialised domains needing citations.** Legal and medical topics are exactly
   where fabricated references do damage, and exactly where a plausible-sounding
   answer is hardest for a non-expert to falsify.
3. **Ambiguous queries.** "Transformers" gets you machine learning or electrical
   engineering, and the agent cannot ask which you meant.
4. **Niche topics.** Where training data was thin, output degrades toward generic
   statements that are true of the whole field but say nothing about the topic.

The prompt mitigates (1) and (2) by requiring the model to declare thin knowledge
and forbidding invented citations. That reduces the failure rate; it does not
eliminate it.

## Engineering decisions

### Failures are data, not strings

The original implementation returned `"❌ OpenAI API error: $message"` as the
result. That collapses three distinct things into one type: a successful report, a
recoverable failure, and a permanent one. A caller cannot branch on it, and the
error text can end up written into an output file as though it were the report.

`LlmFailure` enumerates the cases instead. The retry policy keys off
`isRetryable`, `ExitCode.forFailure` maps each to a process exit code, and a
failure can never be mistaken for content.

### The agent depends on an interface

`ResearchAgent` takes an `LlmClient`, not an `OpenAIClient`. This costs one small
interface and buys two things: the agent's tests run against a recording fake with
no HTTP at all, and adding a second provider means adding a class rather than
editing the agent.

### Wire types are separate from domain types

`ChatCompletionResponseDto` mirrors OpenAI's JSON, annotated
`@JsonIgnoreProperties(ignoreUnknown = true)` so a new provider field cannot break
parsing. `CompletionResponse` is what the rest of the program sees. The mapping
between them is the only place that needs to change when the API evolves.

### Retries with backoff

5xx and 429 responses are retried with exponential backoff, capped at 30 seconds
and preferring the server's `Retry-After` header when present. Authentication
failures are not retried — a wrong key stays wrong, and retrying it just burns
time. The sleep function is injected, so retry timing is asserted in tests without
tests that actually sleep.

### Progress on stderr, output on stdout

`--format raw` is meant to be piped. Anything that is not the report itself goes
to stderr, so `deep-research -q "..." -f raw > notes.txt` produces a clean file.

### Prompt structure

The system message carries the role, the section contract, and the honesty rules;
the user message carries only the topic. Keeping the contract in the system turn
means it is not re-litigated per query, and the section headings stay stable —
which is what makes the Markdown diffable and the JSON worth archiving.

## Known limitations

- **No retrieval.** The single largest gap. Everything the agent knows is
  parametric, which caps its usefulness on anything recent or obscure and rules
  out real source attribution.
- **No memory.** Each run is independent; there are no follow-ups.
- **Single-shot generation.** No decomposition into sub-questions, no critique
  pass over its own draft.
- **Structure is enforced by prompt, not by parser.** The renderer trusts that the
  model followed the contract. A validating parser that rejected or repaired
  non-conforming output would be strictly better.

## Planned work

In the order that would add the most value:

1. **Retrieval-augmented mode.** A search-and-fetch tool loop before generation.
   This addresses the recency and obscurity failures together, and is the
   prerequisite for real citations.
2. **Citations.** With retrieval in place, attribute each claim to a fetched
   source with a URL — turning "trust the model" into "check the link".
3. **Sub-question decomposition.** Break a topic into sub-questions, research each,
   then synthesise. This is what would make the "deep" in the name earned.
4. **Session memory.** Persist prior reports so follow-ups can refine rather than
   restart.
5. **More providers.** Anthropic and a local Ollama `LlmClient`, which the existing
   interface already accommodates.
6. **Streaming.** Token-by-token output for a better wait on long reports.
