package io.github.bsidedevs.api_review.shared;

/**
 * Notification types, each with a deterministic recipient mapping
 * (Requirements 12.1–12.4):
 * <ul>
 *   <li>{@link #COMMENT_ADDED} → Project Admin owner.</li>
 *   <li>{@link #COMMENT_REPLIED} → comment author + Project Admin owner
 *       (consolidated into one notification if they are the same user).</li>
 *   <li>{@link #SESSION_COMPLETED} → all authorized participants.</li>
 *   <li>{@link #AI_PROCESSING_COMPLETED} → Project Admin owner.</li>
 * </ul>
 *
 * @see DeliveryStatus
 */
public enum NotificationKind {
    COMMENT_ADDED,
    COMMENT_REPLIED,
    SESSION_COMPLETED,
    AI_PROCESSING_COMPLETED
}
