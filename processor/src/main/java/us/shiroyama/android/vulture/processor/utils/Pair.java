package us.shiroyama.android.vulture.processor.utils;

/**
 * @author Fumihiko Shiroyama
 */

public class Pair<A, B> {
    public A first;
    public B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Pair<A, B> of(A first, B second) {
        return new Pair<>(first, second);
    }
}
