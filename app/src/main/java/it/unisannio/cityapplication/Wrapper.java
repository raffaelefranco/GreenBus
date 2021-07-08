package it.unisannio.cityapplication;

import java.io.Serializable;

// TODO remove

public class Wrapper<T> implements Serializable {
    private T wrapped;

    public Wrapper(T wrapped) {
        this.wrapped = wrapped;
    }

    public T get() {
        return wrapped;
    }
}
