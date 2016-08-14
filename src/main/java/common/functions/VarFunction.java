package common.functions;

import java.util.Arrays;
import java.util.function.Supplier;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface VarFunction<T, R> {

    @SuppressWarnings("unchecked")
    R apply(T... t);

    static <T, R> Supplier<R> carryFull(VarFunction<T, R> varFunction, T... params) {
        return () -> varFunction.apply(params);
    }

    static <T, R> VarFunction<T, R> carryPartial(VarFunction<T, R> varFunction, T... params) {
        return additionalParams -> {
            T[] result = Arrays.copyOf(params, params.length + additionalParams.length);
            System.arraycopy(additionalParams, 0, result, params.length, additionalParams.length);
            return varFunction.apply(result);
        };
    }
}