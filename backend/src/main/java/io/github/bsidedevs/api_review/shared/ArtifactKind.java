package io.github.bsidedevs.api_review.shared;

/**
 * Taxonomy of artifact types captured or created inside a Review Session.
 *
 * <p>Capture artifacts (raw browser-extension data) are only created while the
 * session is {@code RECORDING}. Annotation artifacts (user-created content) can
 * be created during {@code RECORDING}, {@code COMPLETED} or {@code REOPENED}.
 * Authorship and creation timestamp are immutable for every artifact
 * (Requirements 9.1, 9.2); only the author may edit or delete their own
 * {@code COMMENT}, {@code TEXT_NOTE} or {@code VOICE_NOTE} (Requirements 9.3, 9.4).
 */
public enum ArtifactKind {

    // Capture artifacts — RECORDING state only (Requirement 8.8)
    RRWEB_RECORDING,
    DOM_SNAPSHOT,
    TIMELINE_EVENT,
    BROWSER_METADATA,

    // Annotation artifacts — RECORDING, COMPLETED or REOPENED (Requirement 8.9)
    EXCALIDRAW_SCENE,
    COMMENT,
    TEXT_NOTE,
    VOICE_NOTE,
    TRANSCRIPTION,
    AI_SUMMARY,
    AI_RESULT
}
