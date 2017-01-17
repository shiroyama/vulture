package us.shiroyama.android.vulture.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for classes that have {@link SafeCallback} methods
 *
 * @author Fumihiko Shiroyama
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ObserveLifecycle {
}
