package gg.pufferfish.pufferfish.util;

import java.util.Iterator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record IterableWrapper<T>(Iterator<T> iterator) implements Iterable<T> {

    @Override
    public Iterator<T> iterator() {
        return iterator;
    }

}
