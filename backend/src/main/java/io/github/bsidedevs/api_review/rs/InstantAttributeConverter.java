package io.github.bsidedevs.api_review.rs;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JPA AttributeConverter that maps {@link Instant} to/from {@link Timestamp}
 * for TIMESTAMPTZ(3) columns.
 *
 * <p>PostgreSQL TIMESTAMPTZ stores microseconds; this converter preserves
 * millisecond precision as required by the schema (TIMESTAMPTZ(3)).
 */
@Converter(autoApply = false)
public class InstantAttributeConverter implements AttributeConverter<Instant, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(Instant attribute) {
        if (attribute == null) {
            return null;
        }
        return Timestamp.from(attribute);
    }

    @Override
    public Instant convertToEntityAttribute(Timestamp dbData) {
        if (dbData == null) {
            return null;
        }
        return dbData.toInstant();
    }
}
