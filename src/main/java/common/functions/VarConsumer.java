package common.functions;

@FunctionalInterface
public interface VarConsumer<T> {

    void accept(T... t);
}