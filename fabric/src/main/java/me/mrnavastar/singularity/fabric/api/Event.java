package me.mrnavastar.singularity.fabric.api;

import java.util.ArrayList;
import java.util.function.Function;

public class Event<T> {
    private final ArrayList<T> handlers = new ArrayList<>();
    private final Function<ArrayList<T>, T> invokerFactory;

    public Event(Function<ArrayList<T>, T> invokerFactory) {
        this.invokerFactory = invokerFactory;
    }

    public void register(T handler) {
        handlers.add(handler);
    }

    public T getInvoker() {
        return invokerFactory.apply(handlers);
    }
}