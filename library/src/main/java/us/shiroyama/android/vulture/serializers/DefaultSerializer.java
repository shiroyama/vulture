package us.shiroyama.android.vulture.serializers;

/**
 * @author Fumihiko Shiroyama
 */

public class DefaultSerializer implements CustomSerializer<Object, Object> {
    @Override
    public Object serialize(Object o) {
        return o;
    }

    @Override
    public Object deserialize(Object o) {
        return o;
    }
}
