package io.github.bsidedevs.api_review.shared;

import java.io.Serializable;
import java.util.Objects;

/**
 * Sealed reference to an artifact's payload: either inline JSON or an
 * external blob URL. Sealing enables exhaustive pattern matching.
 */
public sealed interface PayloadRef extends Serializable
    permits PayloadRef.InlineJson, PayloadRef.BlobUrl {

    /** JSON content stored inline (comments, text notes, small metadata). */
    record InlineJson(String json) implements PayloadRef {

        public InlineJson {
            Objects.requireNonNull(json, "JSON content must not be null");
        }
    }

    /** Reference to a blob in object storage (recordings, snapshots, voice notes). */
    record BlobUrl(String url) implements PayloadRef {

        public BlobUrl {
            Objects.requireNonNull(url, "Blob URL must not be null");
            if (url.isBlank()) {
                throw new IllegalArgumentException("Blob URL must not be blank");
            }
        }
    }
}
