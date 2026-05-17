package com.marriott.codefest.snap2stay.visualsearchapi.embedding;

import java.util.List;

public final class EmbeddingDtos {

    private EmbeddingDtos() {}

    public record EmbedAndTagResponse(
            List<Float> vector,
            String caption,
            List<String> tags,
            String modelName,
            int vectorDim
    ) {
        /** Convert to a primitive float[] for performance-sensitive paths. */
        public float[] vectorArray() {
            float[] out = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                out[i] = vector.get(i);
            }
            return out;
        }
    }

    public record EmbedTextRequest(String text) {}

    public record EmbedResponse(
            List<Float> vector,
            String modelName,
            int vectorDim
    ) {
        public float[] vectorArray() {
            float[] out = new float[vector.size()];
            for (int i = 0; i < vector.size(); i++) {
                out[i] = vector.get(i);
            }
            return out;
        }
    }
}
