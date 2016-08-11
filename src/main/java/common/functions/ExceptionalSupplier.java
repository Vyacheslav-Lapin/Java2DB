package common.functions;

import java.util.Optional;
import java.util.function.Supplier;

@FunctionalInterface
public interface ExceptionalSupplier<T, E extends Throwable> extends Supplier<Exceptional<T, E>> {

    T call() throws E;

    @SuppressWarnings("unchecked")
    @Override
    default Exceptional<T, E> get() {
        try {
            return Exceptional.withValue(call());
        } catch (Throwable e) {
            return Exceptional.withException((E) e);
        }
    }

    default T getOrThrowUnchecked() {
        return get().getOrThrowUnchecked();
    }

    default void executeOrThrowUnchecked() {
        get().getOrThrowUnchecked();
    }

    default Optional<T> getAsOptional() {
        return get().toOptional();
    }

    static <T, E extends Exception> Exceptional<T, E> call(ExceptionalSupplier<T, E> exceptionalSupplier) {
        return exceptionalSupplier.get();
    }

    static <T, E extends Exception> T getOrThrowUnchecked(ExceptionalSupplier<T, E> exceptionalSupplier) {
        return call(exceptionalSupplier).getOrThrowUnchecked();
    }

    static <T> T avoid(Object t) {
        return null;
    }
}
