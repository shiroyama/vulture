package us.shiroyama.android.vulture.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods to be callbacked safely
 *
 * @author Fumihiko Shiroyama
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface SafeCallback {
}
