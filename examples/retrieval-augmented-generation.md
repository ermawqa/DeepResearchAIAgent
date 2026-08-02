# Research: Retrieval-augmented generation

> **Illustrative sample.** This file shows the section structure the agent's prompt
> enforces. It is a fixture, not the transcript of a live API call — regenerate it
> against your own key with:
> `deep-research -q "Retrieval-augmented generation" -o examples/retrieval-augmented-generation.md`

## Summary
Retrieval-augmented generation (RAG) grounds a language model's output in documents fetched at query time, rather than relying solely on knowledge frozen into its weights. It is the default architecture for question answering over private or fast-moving corpora, because updating the index is far cheaper than retraining. The trade-off is that answer quality becomes bounded by retrieval quality.

## Key Concepts
- **Chunking** - splitting source documents into passages small enough to embed and rank, but large enough to stay self-contained.
- **Embedding** - mapping text to a dense vector so semantic similarity becomes geometric proximity.
- **Vector search** - approximate nearest-neighbour lookup over those vectors, usually HNSW or IVF-based.
- **Reranking** - a second-stage cross-encoder that rescores the top-k candidates with full query-document attention.
- **Grounding** - constraining generation to the retrieved context, and citing which passage supported each claim.

## Current Research Directions
- **Retrieval quality over generator size** - evidence continues to accumulate that better retrieval beats a larger generator for the same budget.
- **Hybrid retrieval** - combining lexical (BM25) and dense signals, since each fails on cases the other handles.
- **Long-context versus retrieval** - as context windows grow, an open question is which workloads still need an index at all.
- **Agentic and iterative retrieval** - letting the model issue several searches, read intermediate results, and refine its own query.

## Tools & Frameworks
- **Vector stores** - FAISS, pgvector, Qdrant, Weaviate, Milvus.
- **Orchestration** - LangChain and LlamaIndex for pipeline assembly.
- **Embedding models** - the E5 and BGE families, and hosted embedding endpoints.
- **Evaluation** - RAGAS and TruLens for faithfulness and context-relevance scoring.

## Challenges & Open Questions
- Chunking remains largely heuristic; there is no accepted method for choosing boundaries per corpus.
- Evaluation is unsettled - faithfulness metrics that themselves rely on an LLM judge inherit that judge's biases.
- Retrieval failures are silent: the generator will answer fluently from irrelevant context unless explicitly instructed to abstain.
- Multi-hop questions still defeat single-shot retrieval, and iterative approaches multiply latency and cost.

## Further Reading
- Search terms: "dense passage retrieval", "hybrid sparse-dense retrieval", "RAG faithfulness evaluation".
- Canonical work: the original RAG paper (Lewis et al., 2020), and the DPR and ColBERT retrieval lines.
- Projects worth reading: the FAISS and RAGAS repositories.

