package common.functions;

import java.util.function.BiFunction;
import java.util.function.Supplier;

@FunctionalInterface
public interface ExceptionalBiFunction<T, U, R, E extends Throwable> extends BiFunction<T, U, Exceptional<R, E>> {

    R get(T t, U u) throws E;

    @Override
    default Exceptional<R, E> apply(T t, U u) {
        try {
            return Exceptional.withValue(get(t, u));
        } catch (Throwable e) {
            //noinspection unchecked
            return Exceptional.withException((E) e);
        }
    }

    static <T, U, R, E extends Throwable> R getOrThrowUnchecked(ExceptionalBiFunction<T, U, R, E> exceptionalBiFunction,
                                                             T param1, U param2) {
        return exceptionalBiFunction.apply(param1, param2).getOrThrowUnchecked();
    }

    static <T, U, R, E extends Throwable> BiFunction<T, U, R> toUncheckedFunction(ExceptionalBiFunction<T, U, R, E> exceptionalBiFunction) {
        return (t, u) -> getOrThrowUnchecked(exceptionalBiFunction, t, u);
    }

    static <T, U, R, E extends Throwable> ExceptionalSupplier<R, E> carry(ExceptionalBiFunction<T, U, R, E> exceptionalBiFunction,
                                                                       T param1, U param2) {
        return () -> exceptionalBiFunction.get(param1, param2);
    }

    static <T, U, R, E extends Throwable> Supplier<R> carryUnchacked(ExceptionalBiFunction<T, U, R, E> exceptionalBiFunction,
                                                                  T param1, U param2) {
        return carry(exceptionalBiFunction, param1, param2)::getOrThrowUnchecked;
    }
}