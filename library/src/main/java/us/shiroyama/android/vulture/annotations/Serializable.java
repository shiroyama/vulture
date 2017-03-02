package us.shiroyama.android.vulture.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import us.shiroyama.android.vulture.serializers.CustomSerializer;
import us.shiroyama.android.vulture.serializers.DefaultSerializer;

/**
 * Annotation for parameters that have {@link CustomSerializer}
 *
 * @author Fumihiko Shiroyama
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Serializable {
    Class<? extends CustomSerializer> serializer() default DefaultSerializer.class;
}
