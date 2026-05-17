"""
Snap2Stay Embedding Service.

Wraps OpenAI's CLIP (ViT-B/32) to produce:
  - vector embeddings (image or text) — 512-dim
  - auto-tags via zero-shot classification against a property vocabulary
  - auto-captions synthesized from the top tags

Endpoints:
  POST /embed             — image -> vector
  POST /embed-and-tag     — image -> {vector, caption, tags}
  POST /embed-text        — text  -> vector (for hybrid search)
  GET  /health            — model loaded?

Same vector space for image and text, which is the whole point of CLIP and is
what makes hybrid search work in prod.
"""

from __future__ import annotations

import io
import logging
import os
from typing import List

import torch
from fastapi import FastAPI, File, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
from pydantic import BaseModel
from transformers import CLIPModel, CLIPProcessor

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("snap2stay.embedding")

MODEL_NAME = os.getenv("SNAP2STAY_MODEL_NAME", "openai/clip-vit-base-patch32")
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# Vocabulary used for zero-shot tagging. Property-relevant concepts only.
TAG_VOCABULARY: List[str] = [
    # Settings
    "beach", "overwater", "tropical", "ocean", "lagoon",
    "mountain", "alpine", "ski", "snow",
    "desert", "safari", "savanna",
    "urban", "skyline", "metropolitan", "rooftop",
    "countryside", "vineyard", "forest",
    # Architecture
    "infinity-pool", "swimming-pool", "spa", "sauna",
    "modern", "luxury", "boutique", "historic", "moroccan", "courtyard",
    "suite", "penthouse", "villa", "bungalow",
    # Mood
    "sunset", "night", "daylight",
    "romantic", "family-friendly", "business",
]

# Map verbose-friendly captions for top tags so the synthesized caption reads naturally.
CAPTION_PHRASE = {
    "beach": "beachfront",
    "overwater": "overwater bungalow",
    "tropical": "tropical",
    "ocean": "oceanfront",
    "lagoon": "lagoon-side",
    "mountain": "mountain-view",
    "alpine": "alpine",
    "ski": "ski-in/ski-out",
    "snow": "snow-covered",
    "desert": "desert",
    "safari": "safari lodge",
    "savanna": "savanna",
    "urban": "urban",
    "skyline": "skyline-view",
    "metropolitan": "metropolitan",
    "rooftop": "rooftop",
    "countryside": "countryside",
    "vineyard": "vineyard",
    "forest": "forested",
    "infinity-pool": "infinity pool",
    "swimming-pool": "pool",
    "spa": "spa",
    "sauna": "sauna",
    "modern": "modern",
    "luxury": "luxury",
    "boutique": "boutique",
    "historic": "historic",
    "moroccan": "Moroccan-style",
    "courtyard": "courtyard",
    "suite": "suite",
    "penthouse": "penthouse",
    "villa": "villa",
    "bungalow": "bungalow",
    "sunset": "sunset",
    "night": "evening",
    "daylight": "daylight",
    "romantic": "romantic",
    "family-friendly": "family-friendly",
    "business": "business",
}

app = FastAPI(title="Snap2Stay Embedding Service", version="0.1.0")

# Permissive CORS for local dev; fronted by visual-search-api in prod, never directly exposed.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class EmbedResponse(BaseModel):
    vector: List[float]
    modelName: str
    vectorDim: int


class EmbedAndTagResponse(BaseModel):
    vector: List[float]
    caption: str
    tags: List[str]
    modelName: str
    vectorDim: int


class EmbedTextRequest(BaseModel):
    text: str


# Lazy-loaded singletons. Loaded on first request so /health can return UP=false
# during cold start instead of blocking the entire process.
_model: CLIPModel | None = None
_processor: CLIPProcessor | None = None
_tag_features: torch.Tensor | None = None


def _load_model() -> tuple[CLIPModel, CLIPProcessor]:
    global _model, _processor, _tag_features
    if _model is None or _processor is None:
        log.info("Loading CLIP model %s onto %s ...", MODEL_NAME, DEVICE)
        _model = CLIPModel.from_pretrained(MODEL_NAME).to(DEVICE)
        _model.eval()
        _processor = CLIPProcessor.from_pretrained(MODEL_NAME)

        # Pre-encode the tag vocabulary once so /embed-and-tag is fast.
        with torch.no_grad():
            tag_inputs = _processor(text=TAG_VOCABULARY, return_tensors="pt", padding=True).to(DEVICE)
            features = _model.get_text_features(**tag_inputs)
            features = features / features.norm(dim=-1, keepdim=True)
            _tag_features = features
        log.info("Model + tag vocabulary ready (%d tags)", len(TAG_VOCABULARY))
    return _model, _processor


def _read_image(upload: UploadFile) -> Image.Image:
    try:
        data = upload.file.read()
        if not data:
            raise HTTPException(status_code=400, detail="Empty image payload")
        return Image.open(io.BytesIO(data)).convert("RGB")
    except HTTPException:
        raise
    except Exception as exc:  # noqa: BLE001 - intentionally broad for upload diagnostics
        raise HTTPException(status_code=400, detail=f"Could not decode image: {exc}") from exc


def _encode_image(img: Image.Image) -> torch.Tensor:
    model, processor = _load_model()
    with torch.no_grad():
        inputs = processor(images=img, return_tensors="pt").to(DEVICE)
        feats = model.get_image_features(**inputs)
        feats = feats / feats.norm(dim=-1, keepdim=True)
    return feats


def _top_tags(image_features: torch.Tensor, k: int = 5, threshold: float = 0.20) -> List[str]:
    """Pick top-k tags whose CLIP similarity to the image exceeds threshold."""
    assert _tag_features is not None
    sims = (image_features @ _tag_features.T).squeeze(0)  # cosine since both are L2-normalized
    top = torch.topk(sims, min(k, sims.shape[0]))
    out: List[str] = []
    for score, idx in zip(top.values.tolist(), top.indices.tolist()):
        if score >= threshold:
            out.append(TAG_VOCABULARY[idx])
    return out


def _synthesize_caption(tags: List[str]) -> str:
    if not tags:
        return "interior space"
    phrases = [CAPTION_PHRASE.get(t, t) for t in tags]
    if len(phrases) == 1:
        return phrases[0]
    if len(phrases) == 2:
        return f"{phrases[0]} {phrases[1]}"
    return f"{', '.join(phrases[:-1])}, with {phrases[-1]}"


@app.get("/health")
def health():
    loaded = _model is not None
    return {
        "status": "UP" if loaded else "STARTING",
        "modelName": MODEL_NAME,
        "vectorDim": 512,
        "device": DEVICE,
        "tagVocabularySize": len(TAG_VOCABULARY),
    }


@app.post("/embed", response_model=EmbedResponse)
def embed(image: UploadFile = File(...)):
    img = _read_image(image)
    feats = _encode_image(img)
    return EmbedResponse(
        vector=feats.squeeze(0).tolist(),
        modelName=MODEL_NAME,
        vectorDim=feats.shape[-1],
    )


@app.post("/embed-and-tag", response_model=EmbedAndTagResponse)
def embed_and_tag(image: UploadFile = File(...)):
    img = _read_image(image)
    feats = _encode_image(img)
    tags = _top_tags(feats)
    caption = _synthesize_caption(tags)
    return EmbedAndTagResponse(
        vector=feats.squeeze(0).tolist(),
        caption=caption,
        tags=tags,
        modelName=MODEL_NAME,
        vectorDim=feats.shape[-1],
    )


@app.post("/embed-text", response_model=EmbedResponse)
def embed_text(req: EmbedTextRequest):
    if not req.text.strip():
        raise HTTPException(status_code=400, detail="Text is empty")
    model, processor = _load_model()
    with torch.no_grad():
        inputs = processor(text=[req.text], return_tensors="pt", padding=True).to(DEVICE)
        feats = model.get_text_features(**inputs)
        feats = feats / feats.norm(dim=-1, keepdim=True)
    return EmbedResponse(
        vector=feats.squeeze(0).tolist(),
        modelName=MODEL_NAME,
        vectorDim=feats.shape[-1],
    )


@app.on_event("startup")
def warmup():
    """Pre-load the model so the first real request doesn't pay the cold-start cost."""
    try:
        _load_model()
    except Exception:  # noqa: BLE001
        log.exception("Model preload failed; will retry on first request")
