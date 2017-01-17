package us.shiroyama.android.vulture.processor.utils;

import com.squareup.javapoet.TypeName;

/**
 * @author Fumihiko Shiroyama
 */

public class TypeUtils {

    public static boolean isTypeBoolean(TypeName typeName) {
        return TypeName.BOOLEAN.equals(unbox(typeName));
    }

    public static boolean isTypeByte(TypeName typeName) {
        return TypeName.BYTE.equals(unbox(typeName));
    }

    public static boolean isTypeShort(TypeName typeName) {
        return TypeName.SHORT.equals(unbox(typeName));
    }

    public static boolean isTypeChar(TypeName typeName) {
        return TypeName.CHAR.equals(unbox(typeName));
    }

    public static boolean isTypeInt(TypeName typeName) {
        return TypeName.INT.equals(unbox(typeName));
    }

    public static boolean isTypeLong(TypeName typeName) {
        return TypeName.LONG.equals(unbox(typeName));
    }

    public static boolean isTypeFloat(TypeName typeName) {
        return TypeName.FLOAT.equals(unbox(typeName));
    }

    public static boolean isTypeDouble(TypeName typeName) {
        return TypeName.DOUBLE.equals(unbox(typeName));
    }

    public static boolean isTypeString(TypeName typeName) {
        return TypeName.get(String.class).equals(typeName);
    }

    private static TypeName unbox(TypeName typeName) {
        try {
            return typeName.unbox();
        } catch (UnsupportedOperationException e) {
            return typeName;
        }
    }
}
