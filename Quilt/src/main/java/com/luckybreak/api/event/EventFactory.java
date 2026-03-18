package com.luckybreak.api.event;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public final class EventFactory {

    private EventFactory() {
    }

    public static <T> Event<T> createArrayBacked(Class<T> callbackType, Function<T[], T> invokerFactory) {
        return new ArrayBackedEvent<>(callbackType, invokerFactory);
    }

    private static final class ArrayBackedEvent<T> implements Event<T> {
        private final Class<T> callbackType;
        private final Function<T[], T> invokerFactory;
        private volatile T[] callbacks;
        private volatile T invoker;

        @SuppressWarnings("unchecked")
        private ArrayBackedEvent(Class<T> callbackType, Function<T[], T> invokerFactory) {
            this.callbackType = callbackType;
            this.invokerFactory = invokerFactory;
            this.callbacks = (T[]) Array.newInstance(callbackType, 0);
            this.invoker = invokerFactory.apply(this.callbacks);
        }

        @Override
        public synchronized void register(T callback) {
            Objects.requireNonNull(callback, "callback");
            T[] newCallbacks = Arrays.copyOf(callbacks, callbacks.length + 1);
            newCallbacks[newCallbacks.length - 1] = callback;
            callbacks = newCallbacks;
            invoker = invokerFactory.apply(newCallbacks);
        }

        @Override
        public T invoker() {
            return invoker;
        }
    }
}