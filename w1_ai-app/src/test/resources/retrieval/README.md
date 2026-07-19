# Retrieval evaluation dataset

`retrieval-eval-v1.jsonl` is the first versioned retrieval set for ScholarBrain.
It labels each case with the stable paper ID in the current evaluation database and an
expected heading. Section and chunk UUIDs are deliberately avoided because they can
change after a structure or persistence rerun.

The default unit test validates only the dataset schema and never calls PostgreSQL,
Redis, an embedding API, or an LLM. The environment-backed evaluation runner should:

1. verify that `paperId` exists in the current evaluation database;
2. run the same case against `vector`, `hybrid`, and later `hybrid-rerank` profiles;
3. treat an evidence hit as relevant when its heading path contains `expectedHeading`;
4. report Recall@5/10, MRR@10, nDCG@10, and P50/P95 latency;
5. save the retrieval version, embedding model, chunker version, and dataset version.

Add exact evidence quotes and paper fingerprints as the set grows. Never label cases
with a transient section or chunk UUID.

Run the environment-backed benchmark after PostgreSQL and the embedding provider are
available:

```powershell
mvn test -pl w1_ai-app -am `
  -Dtest=RetrievalEvaluationIT `
  -Dsurefire.failIfNoSpecifiedTests=false `
  -Dpaper.retrieval.mode=hybrid
```

Change the last property to `vector` or `fulltext` to compare the same cases without
changing code or labels. This integration test is excluded from the default `*UnitTest`
suite.
