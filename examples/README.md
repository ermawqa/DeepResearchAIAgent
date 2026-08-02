# Examples

Sample output showing the structure the agent produces.

- [`retrieval-augmented-generation.md`](retrieval-augmented-generation.md) — a
  Markdown report with the six-section contract from `ResearchPrompt`.

These files are **illustrative fixtures**, not transcripts of live API calls, so
they can be committed without spending credits and without pinning the repo to
one model's phrasing on one day. Regenerate them against your own key with:

```bash
export OPENAI_API_KEY="sk-..."
./gradlew installDist
./build/install/deep-research/bin/deep-research \
  -q "Retrieval-augmented generation" \
  -o examples/retrieval-augmented-generation.md
```

A real run appends a provenance footer recording the model that served the request
and the token cost. The JSON format carries the same information as fields:

```json
{
  "query": "Retrieval-augmented generation",
  "model": "gpt-4o-mini-2024-07-18",
  "generated_at": "2026-08-02T10:15:30Z",
  "content": "## Summary\n…",
  "usage": {
    "prompt_tokens": 289,
    "completion_tokens": 612,
    "total_tokens": 901
  }
}
```
