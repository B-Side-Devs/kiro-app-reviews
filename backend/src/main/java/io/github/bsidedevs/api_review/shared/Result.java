package io.github.bsidedevs.api_review.shared;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional result type for explicit error handling without exceptions.
 *
 * <p>A {@code Result<T, E>} is either a success ({@link Ok}) or a failure
 * ({@link Err}). Domain operations return this type instead of throwing, so
 * error handling stays explicit and composable across facade boundaries.
 *
 * @param <T> the success value type
 * @param <E> the error type
 */
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    boolean isOk();

    boolean isErr();

    /** @throws IllegalStateException if this is an {@link Err} */
    T unwrap();

    /** @throws IllegalStateException if this is an {@link Ok} */
    E unwrapErr();

    /** Returns the success value, or {@code defaultValue} if this is an error. */
    T unwrapOrElse(T defaultValue);

    /** Returns the success value, or a value computed from the error. */
    T unwrapOrRecover(Function<E, T> recovery);

    /** Maps the success value, leaving an error untouched. */
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    /** Maps the error value, leaving a success untouched. */
    <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper);

    /** Chains another {@code Result}-producing operation on success. */
    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);

    /** Turns a success failing {@code predicate} into an {@link Err}. */
    Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier);

    void ifOk(Consumer<? super T> consumer);

    void ifErr(Consumer<? super E> consumer);

    /** Converts to {@link Optional}, discarding the error on failure. */
    Optional<T> toOptional();

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /** Success variant holding the value. */
    record Ok<T, E>(T value) implements Result<T, E> {

        public Ok {
            Objects.requireNonNull(value, "Ok value must not be null");
        }

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public boolean isErr() {
            return false;
        }

        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public E unwrapErr() {
            throw new IllegalStateException("Cannot unwrap error from Ok: " + value);
        }

        @Override
        public T unwrapOrElse(T defaultValue) {
            return value;
        }

        @Override
        public T unwrapOrRecover(Function<E, T> recovery) {
            return value;
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return new Ok<>(mapper.apply(value));
        }

        @Override
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            @SuppressWarnings("unchecked")
            Result<T, F> result = (Result<T, F>) this;
            return result;
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier) {
            if (predicate.test(value)) {
                return this;
            }
            return new Err<>(errorSupplier.get());
        }

        @Override
        public void ifOk(Consumer<? super T> consumer) {
            consumer.accept(value);
        }

        @Override
        public void ifErr(Consumer<? super E> consumer) {
            // No-op for Ok
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.of(value);
        }

        @Override
        public String toString() {
            return "Ok(" + value + ")";
        }
    }

    /** Failure variant holding the error. */
    record Err<T, E>(E error) implements Result<T, E> {

        public Err {
            Objects.requireNonNull(error, "Err error must not be null");
        }

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public boolean isErr() {
            return true;
        }

        @Override
        public T unwrap() {
            throw new IllegalStateException("Cannot unwrap value from Err: " + error);
        }

        @Override
        public E unwrapErr() {
            return error;
        }

        @Override
        public T unwrapOrElse(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T unwrapOrRecover(Function<E, T> recovery) {
            return recovery.apply(error);
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) this;
            return result;
        }

        @Override
        public <F> Result<T, F> mapErr(Function<? super E, ? extends F> mapper) {
            return new Err<>(mapper.apply(error));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            @SuppressWarnings("unchecked")
            Result<U, E> result = (Result<U, E>) this;
            return result;
        }

        @Override
        public Result<T, E> filter(Predicate<? super T> predicate, Supplier<E> errorSupplier) {
            return this;
        }

        @Override
        public void ifOk(Consumer<? super T> consumer) {
            // No-op for Err
        }

        @Override
        public void ifErr(Consumer<? super E> consumer) {
            consumer.accept(error);
        }

        @Override
        public Optional<T> toOptional() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return "Err(" + error + ")";
        }
    }
}
