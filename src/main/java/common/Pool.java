package common;

import common.functions.ExceptionalConsumer;
import common.functions.ExceptionalSupplier;
import common.reflect.InvocationHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static common.functions.ExceptionalSupplier.avoid;

public class Pool<T extends AutoCloseable> implements Supplier<T>, AutoCloseable {

    private Function<InvocationHandler<T>, T> proxyMaker;
    private BlockingQueue<T> freeObjectsQueue;
    private volatile boolean isClosing;

    public Pool(Class<T> anInterface, Supplier<T> generator, int size) {
        proxyMaker = InvocationHandler.getProxyMakerFor(anInterface);
        freeObjectsQueue = new ArrayBlockingQueue<>(size);

        IntStream.range(0, size)
                .mapToObj(i -> generator.get())
                .map(this::proxy)
                .forEach(freeObjectsQueue::add);
    }

    private T proxy(T t) {
        return proxyMaker.apply(
                (proxy, method, chain, args) ->
                        method.getName().equals("close") && !isClosing ?
                                avoid(freeObjectsQueue.offer(proxy)):
                                chain.apply(t));
    }

    @Override
    public T get() {
        if (isClosing) throw new RuntimeException("Trying to get object from closed pool!");
        return ExceptionalSupplier.getOrThrowUnchecked(freeObjectsQueue::take);
    }

    @Override
    public void close() throws Exception {
        isClosing = true;
        freeObjectsQueue.forEach(ExceptionalConsumer.toUncheckedConsumer(T::close));
    }
}