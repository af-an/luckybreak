package com.luckybreak.api.event;

public interface Event<T> {
    void register(T callback);
    T invoker();
}