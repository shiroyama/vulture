package us.shiroyama.android.vulture.processor.utils;

import android.os.Bundle;
import android.os.Parcelable;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.io.Serializable;
import java.util.ArrayList;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * @author Fumihiko Shiroyama
 */

public class TypeUtils {
    public static boolean isPrimitive(TypeName typeName) {
        return typeName.isPrimitive() || typeName.isBoxedPrimitive();
    }

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

    public static boolean isTypeBundle(TypeName typeName) {
        return TypeName.get(Bundle.class).equals(typeName);
    }

    private static TypeName unbox(TypeName typeName) {
        try {
            return typeName.unbox();
        } catch (UnsupportedOperationException e) {
            return typeName;
        }
    }

    public static boolean isPrimitiveArray(Element element) {
        if (element.asType().getKind() != TypeKind.ARRAY) {
            return false;
        }

        ArrayType arrayType = (ArrayType) element.asType();
        TypeMirror componentType = arrayType.getComponentType();

        TypeName typeName = TypeName.get(componentType);
        return typeName.isPrimitive() || typeName.isBoxedPrimitive();
    }

    public static boolean isTypeParcelable(Types typeUtils, Elements elementUtils, Element element) {
        TypeElement typeSerializable = elementUtils.getTypeElement(Parcelable.class.getName());
        return typeUtils.isSubtype(element.asType(), typeSerializable.asType());
    }

    public static boolean isParcelableArrayList(Types typeUtils, Elements elementUtils, Element element) {
        TypeElement typeArrayList = elementUtils.getTypeElement(ArrayList.class.getName());
        TypeElement typeParcelable = elementUtils.getTypeElement(Parcelable.class.getName());
        WildcardType wildcardType = typeUtils.getWildcardType(typeParcelable.asType(), null);
        DeclaredType declaredType = typeUtils.getDeclaredType(typeArrayList, wildcardType);

        return typeUtils.isSubtype(element.asType(), declaredType);
    }

    public static boolean isParcelableArray(Types typeUtils, Elements elementUtils, Element element) {
        if (element.asType().getKind() != TypeKind.ARRAY) {
            return false;
        }

        ArrayType arrayType = (ArrayType) element.asType();
        TypeMirror componentType = arrayType.getComponentType();

        TypeElement typeParcelable = elementUtils.getTypeElement(Parcelable.class.getName());
        return typeUtils.isSubtype(componentType, typeParcelable.asType());
    }

    public static boolean isSerializable(Types typeUtils, Elements elementUtils, Element element) {
        TypeElement typeSerializable = elementUtils.getTypeElement(Serializable.class.getName());
        return typeUtils.isSubtype(element.asType(), typeSerializable.asType());
    }

    public static String getSimpleClassName(TypeName typeName) {
        return ((ClassName) typeName).simpleName();
    }

    public static String getPrimitiveClassName(TypeName typeName) {
        return ((ClassName) typeName.box()).simpleName();
    }

    public static String getPrimitiveArrayClassName(Element element) {
        if (element.asType().getKind() != TypeKind.ARRAY) {
            throw new IllegalArgumentException("element is not array: " + element.toString());
        }

        ArrayType arrayType = (ArrayType) element.asType();
        TypeMirror componentType = arrayType.getComponentType();
        ClassName className = (ClassName) TypeName.get(componentType).box();
        return className.simpleName() + "Array";
    }

}
