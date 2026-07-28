package io.github.bsidedevs.api_review.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for shared enums.
 * 
 * <p>Validates enum values match design specifications.
 */
class EnumsTest {

    @Test
    @DisplayName("UserRole has exactly three values")
    void userRole_hasThreeValues() {
        UserRole[] roles = UserRole.values();
        assertEquals(3, roles.length);
        assertNotNull(UserRole.PROJECT_ADMIN);
        assertNotNull(UserRole.CLIENT);
        assertNotNull(UserRole.PLATFORM_ADMIN);
    }

    @Test
    @DisplayName("ReviewSessionState has exactly six values")
    void reviewSessionState_hasSixValues() {
        ReviewSessionState[] states = ReviewSessionState.values();
        assertEquals(6, states.length);
        assertNotNull(ReviewSessionState.DRAFT);
        assertNotNull(ReviewSessionState.RECORDING);
        assertNotNull(ReviewSessionState.COMPLETED);
        assertNotNull(ReviewSessionState.REOPENED);
        assertNotNull(ReviewSessionState.ARCHIVED);
        assertNotNull(ReviewSessionState.DELETED);
    }

    @Test
    @DisplayName("InvitationStatus has exactly four values")
    void invitationStatus_hasFourValues() {
        InvitationStatus[] statuses = InvitationStatus.values();
        assertEquals(4, statuses.length);
        assertNotNull(InvitationStatus.PENDING);
        assertNotNull(InvitationStatus.ACCEPTED);
        assertNotNull(InvitationStatus.REVOKED);
        assertNotNull(InvitationStatus.EXPIRED);
    }

    @Test
    @DisplayName("ArtifactKind has exactly eleven values")
    void artifactKind_hasElevenValues() {
        ArtifactKind[] kinds = ArtifactKind.values();
        assertEquals(11, kinds.length);
        // Capture artifacts
        assertNotNull(ArtifactKind.RRWEB_RECORDING);
        assertNotNull(ArtifactKind.DOM_SNAPSHOT);
        assertNotNull(ArtifactKind.TIMELINE_EVENT);
        assertNotNull(ArtifactKind.BROWSER_METADATA);
        // Annotation artifacts
        assertNotNull(ArtifactKind.EXCALIDRAW_SCENE);
        assertNotNull(ArtifactKind.COMMENT);
        assertNotNull(ArtifactKind.TEXT_NOTE);
        assertNotNull(ArtifactKind.VOICE_NOTE);
        assertNotNull(ArtifactKind.TRANSCRIPTION);
        assertNotNull(ArtifactKind.AI_SUMMARY);
        assertNotNull(ArtifactKind.AI_RESULT);
    }

    @Test
    @DisplayName("NotificationKind has exactly four values")
    void notificationKind_hasFourValues() {
        NotificationKind[] kinds = NotificationKind.values();
        assertEquals(4, kinds.length);
        assertNotNull(NotificationKind.COMMENT_ADDED);
        assertNotNull(NotificationKind.COMMENT_REPLIED);
        assertNotNull(NotificationKind.SESSION_COMPLETED);
        assertNotNull(NotificationKind.AI_PROCESSING_COMPLETED);
    }

    @Test
    @DisplayName("DeliveryStatus has exactly three values")
    void deliveryStatus_hasThreeValues() {
        DeliveryStatus[] statuses = DeliveryStatus.values();
        assertEquals(3, statuses.length);
        assertNotNull(DeliveryStatus.PENDING_RECORD);
        assertNotNull(DeliveryStatus.RECORDED);
        assertNotNull(DeliveryStatus.FAILED);
    }

    @Test
    @DisplayName("PersistenceStatus has exactly three values")
    void persistenceStatus_hasThreeValues() {
        PersistenceStatus[] statuses = PersistenceStatus.values();
        assertEquals(3, statuses.length);
        assertNotNull(PersistenceStatus.OK);
        assertNotNull(PersistenceStatus.FAILED);
        assertNotNull(PersistenceStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("AccessError has exactly three values")
    void accessError_hasThreeValues() {
        AccessError[] errors = AccessError.values();
        assertEquals(3, errors.length);
        assertNotNull(AccessError.UNAUTHENTICATED);
        assertNotNull(AccessError.FORBIDDEN);
        assertNotNull(AccessError.NOT_FOUND);
    }

    @Test
    @DisplayName("AccessReason has exactly nine values")
    void accessReason_hasNineValues() {
        AccessReason[] reasons = AccessReason.values();
        assertEquals(9, reasons.length);
        // Grant reasons
        assertNotNull(AccessReason.OWNER);
        assertNotNull(AccessReason.ACCEPTED_INVITATION);
        assertNotNull(AccessReason.ROLE_ADMIN);
        // Denial reasons
        assertNotNull(AccessReason.DENIED_NO_RELATION);
        assertNotNull(AccessReason.DENIED_NO_ROLE);
        assertNotNull(AccessReason.DENIED_PENDING_INVITATION);
        assertNotNull(AccessReason.DENIED_REVOKED);
        assertNotNull(AccessReason.DENIED_PROJECT_BLOCKED);
        assertNotNull(AccessReason.DENIED_UNKNOWN);
    }
}
