package io.github.bsidedevs.api_review.rs.metadata;

import java.util.Optional;

/**
 * Patch record for updating Review Session metadata.
 *
 * <p>Each field uses {@link FieldPatch} to distinguish between:
 * - {@link FieldPatch.Unchanged}: leave the field as-is
 * - {@link FieldPatch.Clear}: remove value (set to absent)
 * - {@link FieldPatch.Set}: replace with a new value
 *
 * @param name name patch instruction
 * @param description description patch instruction
 * @param targetUrl target URL patch instruction
 */
public record MetadataPatch(
        FieldPatch<String> name,
        FieldPatch<String> description,
        FieldPatch<String> targetUrl
) {

    /**
     * Creates a patch with all fields unchanged.
     *
     * @return patch with all fields set to UNCHANGED
     */
    public static MetadataPatch unchanged() {
        return new MetadataPatch(
                new FieldPatch.Unchanged<>(),
                new FieldPatch.Unchanged<>(),
                new FieldPatch.Unchanged<>()
        );
    }

    /**
     * Creates a patch with all fields cleared.
     *
     * @return patch with all fields set to CLEAR
     */
    public static MetadataPatch clear() {
        return new MetadataPatch(
                new FieldPatch.Clear<>(),
                new FieldPatch.Clear<>(),
                new FieldPatch.Clear<>()
        );
    }

    /**
     * Applies this patch to the current metadata values.
     *
     * @param currentName current name value
     * @param currentDescription current description value
     * @param currentTargetUrl current target URL value
     * @return tuple of (name, description, targetUrl) after applying patch
     */
    public PatchResult applyPatch(Optional<String> currentName, Optional<String> currentDescription, Optional<String> currentTargetUrl) {
        String newName = applyField(name, currentName);
        String newDescription = applyField(description, currentDescription);
        String newTargetUrl = applyField(targetUrl, currentTargetUrl);

        return new PatchResult(newName, newDescription, newTargetUrl);
    }

    private String applyField(FieldPatch<String> patch, Optional<String> currentValue) {
        return switch (patch) {
            case FieldPatch.Unchanged<?> ignored -> currentValue.orElse(null);
            case FieldPatch.Clear<?> ignored -> null;
            case FieldPatch.Set<String> set -> set.value();
        };
    }

    /**
     * Result of applying a metadata patch.
     *
     * @param name resulting name value (may be null)
     * @param description resulting description value (may be null)
     * @param targetUrl resulting target URL value (may be null)
     */
    public record PatchResult(
            String name,
            String description,
            String targetUrl
    ) {
    }
}
