package us.shiroyama.android.vulture.serializers;

/**
 * @author Fumihiko Shiroyama
 */

public interface CustomSerializer<FROM, TO> {
    TO serialize(FROM from);

    FROM deserialize(TO to);
}
