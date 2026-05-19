# embedding-service

Python FastAPI wrapper around Google's SigLIP. Same model produces both image and text embeddings, which is what makes hybrid search work.

In prod this is owned by the **ML Platform** team and runs on a GPU EKS node group. Codefest variant runs on CPU — fine for 50–100 demo images and single-image queries.

## Why SigLIP over CLIP?

SigLIP (Sigmoid Loss for Language-Image Pre-training) improves on CLIP with:
- **~10% better zero-shot accuracy** on standard benchmarks
- **Sigmoid loss** instead of softmax — better calibrated similarity scores
- **Same inference speed** on GPU, slightly faster on CPU
- **768-dim vectors** (vs CLIP's 512) — more expressive representations

## Endpoints

See [`../openapi/embedding-service.yaml`](../openapi/embedding-service.yaml).

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/embed` | Image → 768-dim vector (fast path, used during ingestion if you don't need tags) |
| `POST` | `/embed-and-tag` | Image → vector + auto-caption + auto-tags (used by both index and query) |
| `POST` | `/embed-text` | Text → 768-dim vector in the **same space** as image embeddings |
| `GET` | `/health` | Model loaded? Which device? |

## How tagging works

There's no captioning model. Instead:

1. SigLIP encodes the image into a 768-dim vector.
2. We pre-encode a fixed vocabulary of property-relevant labels (`beach`, `infinity-pool`, `urban`, `sunset`, etc.) into the same space.
3. We pick the top-k labels by cosine similarity above a threshold.
4. The "caption" is a synthesized phrase from those labels (e.g., `"beachfront, overwater bungalow, with infinity pool"`).

This is good enough for hybrid retrieval BM25 against indexed property tags. In Phase 2, swap in BLIP-2 or a fine-tuned captioner for richer text.

## Vocabulary

Edit `TAG_VOCABULARY` in `main.py`. The model picks them up on next start.

## Run

```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8082
```

First start downloads ~400 MB of SigLIP weights to `~/.cache/huggingface/`. Cached for subsequent starts.

## Sanity check

```bash
# Image embedding
curl -X POST http://localhost:8082/embed-and-tag \
  -F "image=@/path/to/photo.jpg" | jq

# Text embedding (for hybrid query)
curl -X POST http://localhost:8082/embed-text \
  -H "Content-Type: application/json" \
  -d '{"text":"overwater villa at sunset"}' | jq

# Health
curl http://localhost:8082/health | jq
```

## Notes

- CPU inference is ~100–250 ms per image on Apple Silicon, 250–500 ms on x86. Acceptable for the demo.
- The model singleton is preloaded on startup but loaded lazily as a fallback so `/health` works during cold start.
- No PII handling — this service never sees identifiable data; the visual-search-api strips EXIF before forwarding the image bytes here.
- SigLIP uses sigmoid scoring, so raw similarity values are generally lower than CLIP's softmax-based scores. The tagging threshold is adjusted accordingly (0.15 vs 0.20).
