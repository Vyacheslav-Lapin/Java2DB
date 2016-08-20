package common.functions;

import java.util.function.Function;

@FunctionalInterface
public interface ExceptionalVarConsumer<T, E extends Throwable> extends VarConsumer<T> {

    @SuppressWarnings("unchecked")
    void call(T... params) throws E;

    @SuppressWarnings("unchecked")
    @Override
    default void accept(T... params) {
        try {
            call(params);
        } catch (Throwable e) {
            //noinspection unchecked
            ifThrowable((E) e);
        }
    }

    default void ifThrowable(E e) {
        throw new RuntimeException(e);
    }

    static <T, E extends Throwable> VarConsumer<T> toUnchecked(ExceptionalVarConsumer<T, E> exceptionalConsumer) {
        return exceptionalConsumer;
    }

    @SafeVarargs
    @SuppressWarnings("unused")
    static <T, E extends Throwable> void call(ExceptionalVarConsumer<T, E> exceptionalConsumer, T... params) {
        call(exceptionalConsumer, RuntimeException::new, params);
    }

    @SafeVarargs
    static <T, E extends Throwable, E1 extends Throwable> void call(ExceptionalVarConsumer<T, E> teExceptionalVarConsumer,
                                                                    Function<E, E1> exceptionTransformer,
                                                                    T... params) throws E1 {
        try {
            teExceptionalVarConsumer.call(params);
        } catch (Throwable e) {
            //noinspection unchecked
            throw exceptionTransformer.apply((E) e);
        }
    }

    @SafeVarargs
    static <T, E extends Throwable> ExceptionalRunnable<E> supply(ExceptionalVarConsumer<T, E> exceptionalVarConsumer,
                                                                  T... params) {
        return () -> exceptionalVarConsumer.accept(params);
    }

    static <T, E extends Throwable> Runnable supplyUnchacked(ExceptionalVarConsumer<T, E> exceptionalVarConsumer,
                                                             T... params) {
        return supply(exceptionalVarConsumer, params);
    }
}
