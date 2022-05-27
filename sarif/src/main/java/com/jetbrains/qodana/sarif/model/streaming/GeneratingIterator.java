package com.jetbrains.qodana.sarif.model.streaming;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class GeneratingIterator<T> implements Iterator<T> {
    private T next = null;
    private State state = State.INITIAL_UNINITIALIZED;

    protected abstract T makeInitial();
    protected abstract T makeNext(T prev);

    @Override
    public boolean hasNext() {
        if (state == State.DONE) {
            return false;
        } else if (state == State.INITIAL_UNINITIALIZED) {
            next = makeInitial();
            state = State.NEXT_KNOWN;
        } else if (state == State.NEXT_UNKNOWN) {
            next = makeNext(next);
            state = State.NEXT_KNOWN;
        }
        if (next == null) {
            state = State.DONE;
            return false;
        }
        return true;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        } else {
            state = State.NEXT_UNKNOWN;
            return next;
        }
    }

    private enum State {
        INITIAL_UNINITIALIZED, // initial value, nothing calculated yet
        NEXT_UNKNOWN, // initial calculated, but next value is yet unknown
        NEXT_KNOWN, // initial calculated, next known
        DONE // there is no more elements
    }
}
