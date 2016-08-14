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

    static <T, R, E extends Throwable> ExceptionalSupplier<R, E> carry(ExceptionalVarFunction<T, R, E> exceptionalVarFunction, T... params) {
        return () -> exceptionalVarFunction.get(params);
    }

    static <T, R, E extends Throwable> Supplier<R> carryUnchacked(ExceptionalVarFunction<T, R, E> exceptionalVarFunction, T... params) {
        return carry(exceptionalVarFunction, params)::getOrThrowUnchecked;
    }
}
