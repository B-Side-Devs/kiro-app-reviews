package io.github.bsidedevs.api_review.shared;

/**
 * Delivery status of a notification's history recording. On failure, up to
 * 2 retries are attempted (3 total) before marking {@link #FAILED}
 * (Requirement 12.6).
 */
public enum DeliveryStatus {

    /** Notification generated but not yet persisted to history. */
    PENDING_RECORD,

    /** Persisted and visible in the recipient's notification list. */
    RECORDED,

    /** All 3 recording attempts failed. */
    FAILED
}
