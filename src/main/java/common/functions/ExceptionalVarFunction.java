package common.functions;

import java.util.function.Supplier;

@FunctionalInterface
public interface ExceptionalVarFunction<T, R, E extends Throwable> extends VarFunction<T, Exceptional<R, E>> {

    @SuppressWarnings("unchecked")
    R get(T... t) throws E;

    @SuppressWarnings("unchecked")
    @Override
    default Exceptional<R, E> apply(T... t) {
        try {
            return Exceptional.withValue(get(t));
        } catch (Throwable e) {
            return Exceptional.withException((E) e);
        }
    }

    @SuppressWarnings("unchecked")
    default R getOrThrowUnchecked(T... params) {
        return apply(params).getOrThrowUnchecked();
    }

    @SuppressWarnings("unchecked")
    default void executeOrThrowUnchecked(T... params) {
        getOrThrowUnchecked(params);
    }

    @SafeVarargs
    static <T, R, E extends Throwable> ExceptionalSupplier<R, E> supply(ExceptionalVarFunction<T, R, E> exceptionalVarFunction, T... params) {
        return () -> exceptionalVarFunction.get(params);
    }

    static <T, R, E extends Throwable> Supplier<R> supplyUnchecked(ExceptionalVarFunction<T, R, E> exceptionalVarFunction, T... params) {
        return supply(exceptionalVarFunction, params)::getOrThrowUnchecked;
    }
}
