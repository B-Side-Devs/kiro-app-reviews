package io.github.bsidedevs.api_review.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Result}.
 */
class ResultTest {

    @Nested
    @DisplayName("Ok variant")
    class OkTests {

        @Test
        @DisplayName("isOk returns true")
        void isOk_returnsTrue() {
            Result<String, String> result = Result.ok("value");
            assertTrue(result.isOk());
        }

        @Test
        @DisplayName("isErr returns false")
        void isErr_returnsFalse() {
            Result<String, String> result = Result.ok("value");
            assertFalse(result.isErr());
        }

        @Test
        @DisplayName("unwrap returns the value")
        void unwrap_returnsValue() {
            Result<String, String> result = Result.ok("value");
            assertEquals("value", result.unwrap());
        }

        @Test
        @DisplayName("unwrapErr throws IllegalStateException")
        void unwrapErr_throws() {
            Result<String, String> result = Result.ok("value");
            assertThrows(IllegalStateException.class, result::unwrapErr);
        }

        @Test
        @DisplayName("unwrapOrElse returns the value")
        void unwrapOrElse_returnsValue() {
            Result<String, String> result = Result.ok("value");
            assertEquals("value", result.unwrapOrElse("default"));
        }

        @Test
        @DisplayName("unwrapOrRecover returns the value without calling recovery")
        void unwrapOrRecover_returnsValue() {
            Result<String, String> result = Result.ok("value");
            assertEquals("value", result.unwrapOrRecover(err -> "recovered"));
        }

        @Test
        @DisplayName("map transforms the value")
        void map_transformsValue() {
            Result<String, String> result = Result.ok("value");
            Result<Integer, String> mapped = result.map(String::length);
            assertTrue(mapped.isOk());
            assertEquals(5, mapped.unwrap());
        }

        @Test
        @DisplayName("mapErr returns same Ok with different error type")
        void mapErr_returnsSameOk() {
            Result<String, String> result = Result.ok("value");
            Result<String, Integer> mapped = result.mapErr(String::length);
            assertTrue(mapped.isOk());
            assertEquals("value", mapped.unwrap());
        }

        @Test
        @DisplayName("flatMap chains operations")
        void flatMap_chainsOperations() {
            Result<String, String> result = Result.ok("value");
            Result<Integer, String> chained = result.flatMap(v -> Result.ok(v.length()));
            assertTrue(chained.isOk());
            assertEquals(5, chained.unwrap());
        }

        @Test
        @DisplayName("flatMap can propagate error from chained operation")
        void flatMap_canPropagateError() {
            Result<String, String> result = Result.ok("value");
            Result<Integer, String> chained = result.flatMap(v -> Result.err("error"));
            assertTrue(chained.isErr());
            assertEquals("error", chained.unwrapErr());
        }

        @Test
        @DisplayName("filter passes when predicate matches")
        void filter_passesWhenMatches() {
            Result<String, String> result = Result.ok("value");
            Result<String, String> filtered = result.filter(s -> s.startsWith("val"), () -> "error");
            assertTrue(filtered.isOk());
            assertEquals("value", filtered.unwrap());
        }

        @Test
        @DisplayName("filter returns error when predicate fails")
        void filter_returnsErrorWhenFails() {
            Result<String, String> result = Result.ok("value");
            Result<String, String> filtered = result.filter(s -> s.startsWith("x"), () -> "error");
            assertTrue(filtered.isErr());
            assertEquals("error", filtered.unwrapErr());
        }

        @Test
        @DisplayName("ifOk consumes the value")
        void ifOk_consumesValue() {
            Result<String, String> result = Result.ok("value");
            AtomicReference<String> captured = new AtomicReference<>();
            result.ifOk(captured::set);
            assertEquals("value", captured.get());
        }

        @Test
        @DisplayName("ifErr does nothing")
        void ifErr_doesNothing() {
            Result<String, String> result = Result.ok("value");
            AtomicReference<String> captured = new AtomicReference<>();
            result.ifErr(captured::set);
            assertNull(captured.get());
        }

        @Test
        @DisplayName("toOptional returns Optional with value")
        void toOptional_returnsPresent() {
            Result<String, String> result = Result.ok("value");
            Optional<String> opt = result.toOptional();
            assertTrue(opt.isPresent());
            assertEquals("value", opt.get());
        }

        @Test
        @DisplayName("null value throws NullPointerException")
        void nullValue_throws() {
            assertThrows(NullPointerException.class, () -> Result.ok(null));
        }

        @Test
        @DisplayName("toString shows Ok(value)")
        void toString_showsOk() {
            Result<String, String> result = Result.ok("value");
            assertEquals("Ok(value)", result.toString());
        }
    }

    @Nested
    @DisplayName("Err variant")
    class ErrTests {

        @Test
        @DisplayName("isOk returns false")
        void isOk_returnsFalse() {
            Result<String, String> result = Result.err("error");
            assertFalse(result.isOk());
        }

        @Test
        @DisplayName("isErr returns true")
        void isErr_returnsTrue() {
            Result<String, String> result = Result.err("error");
            assertTrue(result.isErr());
        }

        @Test
        @DisplayName("unwrap throws IllegalStateException")
        void unwrap_throws() {
            Result<String, String> result = Result.err("error");
            assertThrows(IllegalStateException.class, result::unwrap);
        }

        @Test
        @DisplayName("unwrapErr returns the error")
        void unwrapErr_returnsError() {
            Result<String, String> result = Result.err("error");
            assertEquals("error", result.unwrapErr());
        }

        @Test
        @DisplayName("unwrapOrElse returns the default")
        void unwrapOrElse_returnsDefault() {
            Result<String, String> result = Result.err("error");
            assertEquals("default", result.unwrapOrElse("default"));
        }

        @Test
        @DisplayName("unwrapOrRecover calls recovery function")
        void unwrapOrRecover_callsRecovery() {
            Result<String, String> result = Result.err("error");
            assertEquals("recovered: error", result.unwrapOrRecover(err -> "recovered: " + err));
        }

        @Test
        @DisplayName("map returns same Err with different value type")
        void map_returnsSameErr() {
            Result<String, String> result = Result.err("error");
            Result<Integer, String> mapped = result.map(String::length);
            assertTrue(mapped.isErr());
            assertEquals("error", mapped.unwrapErr());
        }

        @Test
        @DisplayName("mapErr transforms the error")
        void mapErr_transformsError() {
            Result<String, String> result = Result.err("error");
            Result<String, Integer> mapped = result.mapErr(String::length);
            assertTrue(mapped.isErr());
            assertEquals(5, mapped.unwrapErr());
        }

        @Test
        @DisplayName("flatMap returns same Err")
        void flatMap_returnsSameErr() {
            Result<String, String> result = Result.err("error");
            Result<Integer, String> chained = result.flatMap(v -> Result.ok(v.length()));
            assertTrue(chained.isErr());
            assertEquals("error", chained.unwrapErr());
        }

        @Test
        @DisplayName("filter returns same Err")
        void filter_returnsSameErr() {
            Result<String, String> result = Result.err("error");
            Result<String, String> filtered = result.filter(s -> true, () -> "other error");
            assertTrue(filtered.isErr());
            assertEquals("error", filtered.unwrapErr());
        }

        @Test
        @DisplayName("ifOk does nothing")
        void ifOk_doesNothing() {
            Result<String, String> result = Result.err("error");
            AtomicReference<String> captured = new AtomicReference<>();
            result.ifOk(captured::set);
            assertNull(captured.get());
        }

        @Test
        @DisplayName("ifErr consumes the error")
        void ifErr_consumesError() {
            Result<String, String> result = Result.err("error");
            AtomicReference<String> captured = new AtomicReference<>();
            result.ifErr(captured::set);
            assertEquals("error", captured.get());
        }

        @Test
        @DisplayName("toOptional returns empty Optional")
        void toOptional_returnsEmpty() {
            Result<String, String> result = Result.err("error");
            Optional<String> opt = result.toOptional();
            assertTrue(opt.isEmpty());
        }

        @Test
        @DisplayName("null error throws NullPointerException")
        void nullError_throws() {
            assertThrows(NullPointerException.class, () -> Result.err(null));
        }

        @Test
        @DisplayName("toString shows Err(error)")
        void toString_showsErr() {
            Result<String, String> result = Result.err("error");
            assertEquals("Err(error)", result.toString());
        }
    }

    @Nested
    @DisplayName("With AccessError")
    class AccessErrorTests {

        @Test
        @DisplayName("Result with AccessError.UNAUTHENTICATED")
        void withUnauthenticated() {
            Result<String, AccessError> result = Result.err(AccessError.UNAUTHENTICATED);
            assertTrue(result.isErr());
            assertEquals(AccessError.UNAUTHENTICATED, result.unwrapErr());
        }

        @Test
        @DisplayName("Result with AccessError.FORBIDDEN")
        void withForbidden() {
            Result<String, AccessError> result = Result.err(AccessError.FORBIDDEN);
            assertTrue(result.isErr());
            assertEquals(AccessError.FORBIDDEN, result.unwrapErr());
        }

        @Test
        @DisplayName("Result with AccessError.NOT_FOUND")
        void withNotFound() {
            Result<String, AccessError> result = Result.err(AccessError.NOT_FOUND);
            assertTrue(result.isErr());
            assertEquals(AccessError.NOT_FOUND, result.unwrapErr());
        }

        @Test
        @DisplayName("Chaining with access errors")
        void chainingWithAccessErrors() {
            Result<String, AccessError> result = Result.err(AccessError.FORBIDDEN);
            Result<Integer, AccessError> mapped = result
                .map(String::length)
                .flatMap(s -> Result.ok(s * 2));
            
            assertTrue(mapped.isErr());
            assertEquals(AccessError.FORBIDDEN, mapped.unwrapErr());
        }
    }
}
