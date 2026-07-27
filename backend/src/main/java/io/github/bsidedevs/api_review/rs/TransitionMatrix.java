package io.github.bsidedevs.api_review.rs;

import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import java.util.Optional;

/**
 * State transition matrix for Review Sessions.
 *
 * <p>Pure function that determines if a transition from a given state with a
 * command and persistence status is allowed. Returns a {@link Verdict} indicating
 * whether the transition is allowed and, if so, the target state or error code.
 *
 * <p>Key rules:
 * <ul>
 *   <li>DELETED is an absorbing state: all transitions are denied</li>
 *   <li>UPDATE_METADATA only allowed from DRAFT state</li>
 *   <li>START only allowed from DRAFT or REOPENED, transitions to RECORDING</li>
 *   <li>COMPLETE only allowed from RECORDING with OK persistence, transitions to COMPLETED</li>
 *   <li>REOPEN only allowed from COMPLETED, transitions to REOPENED</li>
 *   <li>ARCHIVE only allowed from COMPLETED, transitions to ARCHIVED</li>
 *   <li>DELETE allowed from DRAFT, COMPLETED, REOPENED, ARCHIVED, transitions to DELETED</li>
 * </ul>
 *
 * <p>Note: Authorization (OWNER reason for REOPEN/ARCHIVE/DELETE) is validated in the
 * service layer, not in this matrix. This matrix only defines state transition validity.
 */
public enum TransitionMatrix {

    INSTANCE;

    /**
     * Commands available for state transitions.
     *
     * <p>CREATE is handled by the service layer (creates new entity, no state transition).
     * UPDATE_METADATA, START, COMPLETE, REOPEN, ARCHIVE, DELETE are state transitions.
     */
    public enum Command {
        CREATE,
        UPDATE_METADATA,
        START,
        COMPLETE,
        REOPEN,
        ARCHIVE,
        DELETE
    }

    /**
     * Result of evaluating a state transition.
     *
     * @param allowed whether the transition is allowed
     * @param targetState the target state if allowed, empty otherwise
     * @param code error code if transition is denied, empty otherwise
     */
    public record Verdict(
            boolean allowed,
            Optional<ReviewSessionState> targetState,
            Optional<String> code
    ) {

        /**
         * Creates a verdict for an allowed transition.
         *
         * @param targetState the target state after the transition (can be null for commands that don't change state)
         * @return the verdict
         */
        public static Verdict allowed(ReviewSessionState targetState) {
            return new Verdict(true, Optional.ofNullable(targetState), Optional.empty());
        }

        /**
         * Creates a verdict for a denied transition with a code.
         *
         * @param code error code explaining why the transition was denied
         * @return the verdict
         */
        public static Verdict denied(String code) {
            return new Verdict(false, Optional.empty(), Optional.of(code));
        }

        /**
         * Creates a verdict for a denied transition without a code.
         *
         * @return the verdict
         */
        public static Verdict denied() {
            return new Verdict(false, Optional.empty(), Optional.empty());
        }
    }

    /**
     * Evaluates whether a state transition is allowed.
     *
     * @param state current state of the Review Session
     * @param command requested transition command
     * @param persistenceStatus current persistence status
     * @return verdict indicating if transition is allowed
     * @throws IllegalArgumentException if command is CREATE (handled by service layer)
     */
    public static Verdict evaluate(ReviewSessionState state, Command command, PersistenceStatus persistenceStatus) {
        // DELETED is an absorbing state: all transitions denied
        if (state == ReviewSessionState.DELETED) {
            return Verdict.denied("INVALID_TRANSITION");
        }
        
        return switch (command) {
            case CREATE -> throw new IllegalArgumentException("CREATE command cannot be evaluated by TransitionMatrix");
            case UPDATE_METADATA -> evaluateUpdateMetadata(state);
            case START -> evaluateStart(state);
            case COMPLETE -> evaluateComplete(state, persistenceStatus);
            case REOPEN -> evaluateReopen(state);
            case ARCHIVE -> evaluateArchive(state);
            case DELETE -> evaluateDelete(state);
        };
    }

    private static Verdict evaluateUpdateMetadata(ReviewSessionState state) {
        if (state == ReviewSessionState.DRAFT) {
            return Verdict.allowed(null);
        }
        return Verdict.denied("METADATA_NOT_EDITABLE");
    }

    private static Verdict evaluateStart(ReviewSessionState state) {
        if (state == ReviewSessionState.DRAFT || state == ReviewSessionState.REOPENED) {
            return Verdict.allowed(ReviewSessionState.RECORDING);
        }
        return Verdict.denied("INVALID_TRANSITION");
    }

    private static Verdict evaluateComplete(ReviewSessionState state, PersistenceStatus persistenceStatus) {
        if (state != ReviewSessionState.RECORDING) {
            return Verdict.denied("INVALID_TRANSITION");
        }
        if (persistenceStatus != PersistenceStatus.OK) {
            return Verdict.denied("PERSISTENCE_PENDING");
        }
        return Verdict.allowed(ReviewSessionState.COMPLETED);
    }

    private static Verdict evaluateReopen(ReviewSessionState state) {
        if (state == ReviewSessionState.COMPLETED) {
            return Verdict.allowed(ReviewSessionState.REOPENED);
        }
        return Verdict.denied("INVALID_TRANSITION");
    }

    private static Verdict evaluateArchive(ReviewSessionState state) {
        if (state == ReviewSessionState.COMPLETED) {
            return Verdict.allowed(ReviewSessionState.ARCHIVED);
        }
        return Verdict.denied("INVALID_TRANSITION");
    }

    private static Verdict evaluateDelete(ReviewSessionState state) {
        if (state == ReviewSessionState.DELETED) {
            return Verdict.denied("INVALID_TRANSITION");
        }
        if (state == ReviewSessionState.DRAFT
                || state == ReviewSessionState.COMPLETED
                || state == ReviewSessionState.REOPENED
                || state == ReviewSessionState.ARCHIVED) {
            return Verdict.allowed(ReviewSessionState.DELETED);
        }
        return Verdict.denied("INVALID_TRANSITION");
    }
}
