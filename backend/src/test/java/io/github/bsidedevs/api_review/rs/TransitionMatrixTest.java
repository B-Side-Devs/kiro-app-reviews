package io.github.bsidedevs.api_review.rs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bsidedevs.api_review.shared.PersistenceStatus;
import io.github.bsidedevs.api_review.shared.ReviewSessionState;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Property;
import net.jqwik.api.providers.ArbitraryProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TransitionMatrix}.
 * <p>
 * Property-based tests validate:
 * <ul>
 *   <li>DELETED state is absorbing (all transitions denied)</li>
 *   <li>UPDATE_METADATA only from DRAFT</li>
 *   <li>START only from DRAFT or REOPENED</li>
 *   <li>COMPLETE only from RECORDING with OK persistence</li>
 *   <li>REOPEN/ARCHIVE/DELETE only from COMPLETED/DRAFT/REOPENED/ARCHIVED</li>
 * </ul>
 */
class TransitionMatrixTest {

    @Nested
    @DisplayName("Command enum")
    class CommandEnumTests {

        @Test
        @DisplayName("Command enum has all expected values")
        void commandEnumHasAllValues() {
            var commands = Stream.of(TransitionMatrix.Command.values())
                    .map(TransitionMatrix.Command::name)
                    .toList();
            
            assertThat(commands).containsExactlyInAnyOrder(
                    "CREATE",
                    "UPDATE_METADATA",
                    "START",
                    "COMPLETE",
                    "REOPEN",
                    "ARCHIVE",
                    "DELETE"
            );
        }

        @Test
        @DisplayName("CREATE command cannot be evaluated by TransitionMatrix")
        void createCommandCannotBeEvaluated() {
            assertThatThrownBy(() -> TransitionMatrix.evaluate(null, TransitionMatrix.Command.CREATE, PersistenceStatus.OK))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("CREATE command cannot be evaluated by TransitionMatrix");
        }
    }

    @Nested
    @DisplayName("Verdict record")
    class VerdictRecordTests {

        @Test
        @DisplayName("allowed creates verdict with target state")
        void allowedCreatesVerdictWithTargetState() {
            var verdict = TransitionMatrix.Verdict.allowed(ReviewSessionState.RECORDING);
            
            assertThat(verdict).isNotNull();
            assertThat(verdict.allowed()).isTrue();
            assertThat(verdict.targetState()).isPresent();
            assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.RECORDING);
            assertThat(verdict.code()).isEmpty();
        }

        @Test
        @DisplayName("denied creates verdict with code")
        void deniedCreatesVerdictWithCode() {
            var verdict = TransitionMatrix.Verdict.denied("INVALID_TRANSITION");
            
            assertThat(verdict).isNotNull();
            assertThat(verdict.allowed()).isFalse();
            assertThat(verdict.targetState()).isEmpty();
            assertThat(verdict.code()).isPresent();
            assertThat(verdict.code().get()).isEqualTo("INVALID_TRANSITION");
        }

        @Test
        @DisplayName("denied without code creates verdict with empty code")
        void deniedWithoutCodeCreatesVerdictWithEmptyCode() {
            var verdict = TransitionMatrix.Verdict.denied();
            
            assertThat(verdict).isNotNull();
            assertThat(verdict.allowed()).isFalse();
            assertThat(verdict.targetState()).isEmpty();
            assertThat(verdict.code()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DELETED state is absorbing")
    class DeletedStateTests {

        @Test
        @DisplayName("All commands denied from DELETED state")
        void allCommandsDeniedFromDeleted() {
            for (var command : TransitionMatrix.Command.values()) {
                if (command == TransitionMatrix.Command.CREATE) {
                    continue;
                }
                var v = TransitionMatrix.evaluate(ReviewSessionState.DELETED, command, PersistenceStatus.OK);
                
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
                assertThat(v.targetState()).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("UPDATE_METADATA")
    class UpdateMetadataTests {

        @Test
        @DisplayName("Allowed only from DRAFT state")
        void allowedOnlyFromDraft() {
            // From DRAFT - allowed
            var verdictDraft = TransitionMatrix.evaluate(
                    ReviewSessionState.DRAFT,
                    TransitionMatrix.Command.UPDATE_METADATA,
                    PersistenceStatus.OK
            );
            assertThat(verdictDraft.allowed()).isTrue();
            assertThat(verdictDraft.targetState()).isEmpty();
            assertThat(verdictDraft.code()).isEmpty();

            // From other states - denied (DELETED is absorbing, returns INVALID_TRANSITION)
            var otherStates = Stream.of(ReviewSessionState.values())
                    .filter(s -> s != ReviewSessionState.DRAFT && s != ReviewSessionState.DELETED)
                    .toList();
            
            for (var state : otherStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.UPDATE_METADATA,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("METADATA_NOT_EDITABLE");
            }
            
            // DELETED is absorbing - all transitions return INVALID_TRANSITION
            var vDeleted = TransitionMatrix.evaluate(
                    ReviewSessionState.DELETED,
                    TransitionMatrix.Command.UPDATE_METADATA,
                    PersistenceStatus.OK
            );
            assertThat(vDeleted.allowed()).isFalse();
            assertThat(vDeleted.code()).isPresent();
            assertThat(vDeleted.code().get()).isEqualTo("INVALID_TRANSITION");
        }
    }

    @Nested
    @DisplayName("START")
    class StartTests {

        @Test
        @DisplayName("Allowed from DRAFT and REOPENED")
        void allowedFromDraftAndReopened() {
            for (var state : new ReviewSessionState[]{ReviewSessionState.DRAFT, ReviewSessionState.REOPENED}) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.START,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isTrue();
                assertThat(v.targetState()).isPresent();
                assertThat(v.targetState().get()).isEqualTo(ReviewSessionState.RECORDING);
                assertThat(v.code()).isEmpty();
            }
        }

        @Test
        @DisplayName("Denied from other states")
        void deniedFromOtherStates() {
            var otherStates = Stream.of(ReviewSessionState.values())
                    .filter(s -> s != ReviewSessionState.DRAFT && s != ReviewSessionState.REOPENED)
                    .toList();
            
            for (var state : otherStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.START,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }
    }

    @Nested
    @DisplayName("COMPLETE")
    class CompleteTests {

        @Test
        @DisplayName("Allowed only from RECORDING state with OK persistence")
        void allowedOnlyFromRecordingWithOkPersistence() {
            // From RECORDING with OK - allowed
            var verdict = TransitionMatrix.evaluate(
                    ReviewSessionState.RECORDING,
                    TransitionMatrix.Command.COMPLETE,
                    PersistenceStatus.OK
            );
            assertThat(verdict.allowed()).isTrue();
            assertThat(verdict.targetState()).isPresent();
            assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.COMPLETED);
            assertThat(verdict.code()).isEmpty();
        }

        @Test
        @DisplayName("Denied from non-RECORDING states")
        void deniedFromNonRecordingStates() {
            var otherStates = Stream.of(ReviewSessionState.values())
                    .filter(s -> s != ReviewSessionState.RECORDING)
                    .toList();
            
            for (var state : otherStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.COMPLETE,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }

        @Test
        @DisplayName("Denied from RECORDING with non-OK persistence")
        void deniedFromRecordingWithNonOkPersistence() {
            for (var status : new PersistenceStatus[]{PersistenceStatus.IN_PROGRESS, PersistenceStatus.FAILED}) {
                var v = TransitionMatrix.evaluate(
                        ReviewSessionState.RECORDING,
                        TransitionMatrix.Command.COMPLETE,
                        status
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("PERSISTENCE_PENDING");
            }
        }
    }

    @Nested
    @DisplayName("REOPEN")
    class ReopenTests {

        @Test
        @DisplayName("Allowed only from COMPLETED state")
        void allowedOnlyFromCompleted() {
            // From COMPLETED - allowed
            var verdict = TransitionMatrix.evaluate(
                    ReviewSessionState.COMPLETED,
                    TransitionMatrix.Command.REOPEN,
                    PersistenceStatus.OK
            );
            assertThat(verdict.allowed()).isTrue();
            assertThat(verdict.targetState()).isPresent();
            assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.REOPENED);
            assertThat(verdict.code()).isEmpty();

            // From other states - denied
            var otherStates = Stream.of(ReviewSessionState.values())
                    .filter(s -> s != ReviewSessionState.COMPLETED)
                    .toList();
            
            for (var state : otherStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.REOPEN,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }
    }

    @Nested
    @DisplayName("ARCHIVE")
    class ArchiveTests {

        @Test
        @DisplayName("Allowed only from COMPLETED state")
        void allowedOnlyFromCompleted() {
            // From COMPLETED - allowed
            var verdict = TransitionMatrix.evaluate(
                    ReviewSessionState.COMPLETED,
                    TransitionMatrix.Command.ARCHIVE,
                    PersistenceStatus.OK
            );
            assertThat(verdict.allowed()).isTrue();
            assertThat(verdict.targetState()).isPresent();
            assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.ARCHIVED);
            assertThat(verdict.code()).isEmpty();

            // From other states - denied
            var otherStates = Stream.of(ReviewSessionState.values())
                    .filter(s -> s != ReviewSessionState.COMPLETED)
                    .toList();
            
            for (var state : otherStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.ARCHIVE,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }
    }

    @Nested
    @DisplayName("DELETE")
    class DeleteTests {

        @Test
        @DisplayName("Allowed from DRAFT, COMPLETED, REOPENED, ARCHIVED")
        void allowedFromNonRecordingStates() {
            var allowedStates = new ReviewSessionState[]{
                    ReviewSessionState.DRAFT,
                    ReviewSessionState.COMPLETED,
                    ReviewSessionState.REOPENED,
                    ReviewSessionState.ARCHIVED
            };
            
            for (var state : allowedStates) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.DELETE,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isTrue();
                assertThat(v.targetState()).isPresent();
                assertThat(v.targetState().get()).isEqualTo(ReviewSessionState.DELETED);
                assertThat(v.code()).isEmpty();
            }
        }

        @Test
        @DisplayName("Denied from RECORDING and DELETED states")
        void deniedFromRecordingAndDeleted() {
            for (var state : new ReviewSessionState[]{ReviewSessionState.RECORDING, ReviewSessionState.DELETED}) {
                var v = TransitionMatrix.evaluate(
                        state,
                        TransitionMatrix.Command.DELETE,
                        PersistenceStatus.OK
                );
                assertThat(v.allowed()).isFalse();
                assertThat(v.code()).isPresent();
                assertThat(v.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }
    }

    @Nested
    @DisplayName("Property-based tests")
    class PropertyTests {

        @Property
        @DisplayName("Property 1: DELETED state blocks all transitions")
        void deletedStateIsAbsorbing(
                @net.jqwik.api.ForAll("command") TransitionMatrix.Command command,
                @net.jqwik.api.ForAll("persistenceStatus") PersistenceStatus status
        ) {
            // Skip CREATE as it cannot be evaluated
            if (command == TransitionMatrix.Command.CREATE) {
                return;
            }
            
            var verdict = TransitionMatrix.evaluate(ReviewSessionState.DELETED, command, status);
            
            assertThat(verdict.allowed()).isFalse();
            assertThat(verdict.code()).isPresent();
            assertThat(verdict.code().get()).isEqualTo("INVALID_TRANSITION");
            assertThat(verdict.targetState()).isEmpty();
        }

        @Property
        @DisplayName("Property 2: UPDATE_METADATA only from DRAFT")
        void updateMetadataOnlyFromDraft(
                @net.jqwik.api.ForAll("state") ReviewSessionState state,
                @net.jqwik.api.ForAll("persistenceStatus") PersistenceStatus status
        ) {
            var verdict = TransitionMatrix.evaluate(state, TransitionMatrix.Command.UPDATE_METADATA, status);
            
            if (state == ReviewSessionState.DRAFT) {
                assertThat(verdict.allowed()).isTrue();
                assertThat(verdict.targetState()).isEmpty();
            } else {
                assertThat(verdict.allowed()).isFalse();
                assertThat(verdict.code()).isPresent();
                assertThat(verdict.code().get()).isEqualTo("METADATA_NOT_EDITABLE");
            }
        }

        @Property
        @DisplayName("Property 3: START only from DRAFT or REOPENED")
        void startOnlyFromDraftOrReopened(
                @net.jqwik.api.ForAll("state") ReviewSessionState state,
                @net.jqwik.api.ForAll("persistenceStatus") PersistenceStatus status
        ) {
            var verdict = TransitionMatrix.evaluate(state, TransitionMatrix.Command.START, status);
            
            if (state == ReviewSessionState.DRAFT || state == ReviewSessionState.REOPENED) {
                assertThat(verdict.allowed()).isTrue();
                assertThat(verdict.targetState()).isPresent();
                assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.RECORDING);
            } else {
                assertThat(verdict.allowed()).isFalse();
                assertThat(verdict.code()).isPresent();
                assertThat(verdict.code().get()).isEqualTo("INVALID_TRANSITION");
            }
        }

        @Property
        @DisplayName("Property 4: COMPLETE requires RECORDING and OK persistence")
        void completeRequiresRecordingAndOkPersistence(
                @net.jqwik.api.ForAll("state") ReviewSessionState state,
                @net.jqwik.api.ForAll("persistenceStatus") PersistenceStatus status
        ) {
            var verdict = TransitionMatrix.evaluate(state, TransitionMatrix.Command.COMPLETE, status);
            
            if (state == ReviewSessionState.RECORDING && status == PersistenceStatus.OK) {
                assertThat(verdict.allowed()).isTrue();
                assertThat(verdict.targetState()).isPresent();
                assertThat(verdict.targetState().get()).isEqualTo(ReviewSessionState.COMPLETED);
            } else {
                assertThat(verdict.allowed()).isFalse();
                assertThat(verdict.code()).isPresent();
                // Either INVALID_TRANSITION or PERSISTENCE_PENDING
                assertThat(verdict.code().get()).isIn("INVALID_TRANSITION", "PERSISTENCE_PENDING");
            }
        }

        @Property
        @DisplayName("Property 5: REOPEN/ARCHIVE/DELETE only from COMPLETED or DELETED states")
        void reopenArchiveDeleteFromCorrectStates(
                @net.jqwik.api.ForAll("state") ReviewSessionState state,
                @net.jqwik.api.ForAll("command") TransitionMatrix.Command command,
                @net.jqwik.api.ForAll("persistenceStatus") PersistenceStatus status
        ) {
            // Skip commands not in this category
            if (command != TransitionMatrix.Command.REOPEN
                    && command != TransitionMatrix.Command.ARCHIVE
                    && command != TransitionMatrix.Command.DELETE) {
                return;
            }
            
            var verdict = TransitionMatrix.evaluate(state, command, status);
            
            // REOPEN/ARCHIVE only from COMPLETED
            if (command == TransitionMatrix.Command.REOPEN || command == TransitionMatrix.Command.ARCHIVE) {
                if (state == ReviewSessionState.COMPLETED) {
                    assertThat(verdict.allowed()).isTrue();
                    assertThat(verdict.targetState()).isPresent();
                } else {
                    assertThat(verdict.allowed()).isFalse();
                    assertThat(verdict.code()).isPresent();
                    assertThat(verdict.code().get()).isEqualTo("INVALID_TRANSITION");
                }
            }
            // DELETE from DRAFT, COMPLETED, REOPENED, ARCHIVED
            else if (command == TransitionMatrix.Command.DELETE) {
                if (state == ReviewSessionState.DRAFT
                        || state == ReviewSessionState.COMPLETED
                        || state == ReviewSessionState.REOPENED
                        || state == ReviewSessionState.ARCHIVED) {
                    assertThat(verdict.allowed()).isTrue();
                    assertThat(verdict.targetState()).isPresent();
                } else {
                    assertThat(verdict.allowed()).isFalse();
                    assertThat(verdict.code()).isPresent();
                    assertThat(verdict.code().get()).isEqualTo("INVALID_TRANSITION");
                }
            }
        }
    }

    /**
     * Arbitrary providers for property-based testing.
     */
    public static class Arbitraries {

        @net.jqwik.api.Provide
        net.jqwik.api.Arbitrary<TransitionMatrix.Command> command() {
            return net.jqwik.api.Arbitraries.of(TransitionMatrix.Command.values())
                    .filter(c -> c != TransitionMatrix.Command.CREATE);
        }

        @net.jqwik.api.Provide
        net.jqwik.api.Arbitrary<ReviewSessionState> state() {
            return net.jqwik.api.Arbitraries.of(ReviewSessionState.values());
        }

        @net.jqwik.api.Provide
        net.jqwik.api.Arbitrary<PersistenceStatus> persistenceStatus() {
            return net.jqwik.api.Arbitraries.of(PersistenceStatus.values());
        }
    }
}
