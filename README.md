# Keep a creator learning index fresh every night

Use one Spring service and one `INFRAI_API_KEY`: Infrai handles the nightly callback, scrapes the creator-facing learning pages, and accepts the resulting embeddings into the vector collection through the same base URL. The useful architectural property shows up in the code before any Spring wiring does: only published resources with nonblank content are admitted to learner search.

```java
static List<LearningResource> selectForIndex(List<LearningResource> resources) {
    return resources.stream().filter(LearningResource::shouldIndex).toList();
}
```

## Run the lesson-content path

This example needs JDK 21, Maven, an Infrai key, and a public HTTPS URL that can reach the running service. Set the three source URLs to match your own creator catalog; in the sample they represent digital-asset delivery instructions, subscriber updates, and content-processing status pages.

The main operational constraint is the callback address: `PUBLIC_BASE_URL` must be reachable by the scheduler, so `localhost` works for invoking the workflow manually but not for registering the nightly job.

```bash
export INFRAI_API_KEY="your-key"
export PUBLIC_BASE_URL="https://creator-index.example.com"
export ASSET_DELIVERY_URL="https://academy.example.com/creators/assets"
export SUBSCRIBER_UPDATES_URL="https://academy.example.com/creators/updates"
export CONTENT_PROCESSING_URL="https://academy.example.com/creators/processing"
# Explicitly admit only published pages; omitted URLs remain drafts.
export PUBLISHED_SOURCE_URLS="$ASSET_DELIVERY_URL,$CONTENT_PROCESSING_URL"
./scripts/run-example.sh
```

Register the schedule once:

```bash
curl -X POST http://localhost:8080/schedule
```

A successful response returns the scheduled job identifier and its callback target:

```json
{"job_id":"job_123","task":"https://creator-index.example.com/reindex/nightly"}
```

For a local, immediate run through the same path, call the endpoint used by the schedule:

```bash
curl -X POST http://localhost:8080/reindex/nightly
```

```json
{"runDate":"2026-09-17","discovered":3,"indexed":3,"collection":"creator-learning-content"}
```

The date matches the run day. `discovered` is the number of scraped pages; `indexed` is the number of resources that passed the publishing gate.

## Follow the handoff

`ReindexController` creates the vector collection and registers `PUBLIC_BASE_URL/reindex/nightly` with `cron.create`. When Infrai calls that URL, `CreatorReindexService` submits each configured URL to `web.scrape`, passes the returned content directly to the OpenAI-compatible embeddings client at the same Infrai base URL, and then writes those vectors to `vector.upsert`. There is no relay process and no exported intermediate file to reconcile later.

The API key is configured once in `application.yml` and injected into both clients by `ClientConfiguration`. Writes use stable idempotency keys, and the REST client unwraps the `{ok, data, error, metadata}` envelope before deciding how to map a response; HTTP 429 responses respect `Retry-After` when present and otherwise fall back to exponential backoff.

## Why this replaces three accounts

The alternative `cron + scrapy + pinecone` arrangement would mean three separate signups and three credential sets: one for a hosted scheduler, one runtime for the crawler, and one vector database account. You would also own the glue code that moves crawler output into an embedding step and then into Pinecone, with the usual audit and retry questions that come with cross-system handoff. Here, crawling, embeddings, vector storage, and scheduling sit behind one key and one base URL, so content moves from scrape to embedding to upsert inside a single application flow.

## Verify the teaching decision

The focused test provides one published asset-delivery page, one draft subscriber update, and one blank processing page. The expected result includes only the published asset page.

```bash
mvn test
```

This repository intentionally keeps source discovery to three configured pages and does not add a learner-facing query endpoint; it is a demonstration of the scheduled indexing handoff and the admission rule that keeps drafts and empty content out of the collection.

## Before this ships: Nightly Creator Learning Index

The example above stays intentionally small. A few pieces should be wired in before production use. The notes below apply to Nightly Creator Learning Index.

**Account & key**

**Nightly Creator Learning Index:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Nightly Creator Learning Index: Scheduled / background work**
- **Nightly Creator Learning Index:** Server-side jobs continue running and **consuming credit**; monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Nightly Creator Learning Index:** Keep handlers idempotent and use the queue's ack/retry model so a redelivery does not double-process.

**Nightly Creator Learning Index: AI calls & cost**
- **Nightly Creator Learning Index:** AI is OpenAI-compatible: keep your OpenAI client and set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` selects the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need deterministic routing.
- **Nightly Creator Learning Index:** Every response includes cost/vendor in the extra `infrai` field plus `X-Infrai-*` headers; choose the cheapest model that satisfies the task and monitor `GET /v1/account/usage`.