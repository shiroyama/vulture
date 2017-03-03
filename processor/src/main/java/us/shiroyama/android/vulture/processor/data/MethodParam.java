package us.shiroyama.android.vulture.processor.data;

import android.annotation.TargetApi;

import com.squareup.javapoet.TypeName;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import us.shiroyama.android.vulture.annotations.Serializable;
import us.shiroyama.android.vulture.processor.utils.Pair;
import us.shiroyama.android.vulture.processor.utils.TypeUtils;
import us.shiroyama.android.vulture.serializers.DefaultSerializer;

/**
 * @author Fumihiko Shiroyama
 */

@TargetApi(24)
public class MethodParam {
    private final Types typeUtils;

    private final Elements elementUtils;

    public final String name;

    public final VariableElement element;

    public final TypeName type;

    public boolean isPrimitive;

    public boolean isPrimitiveArray;

    public boolean isString;

    public boolean isBundle;

    public boolean isParcelable;

    public boolean isParcelableArray;

    public boolean isParcelableArrayList;

    public boolean isSerializable;

    public TypeName serializerType;

    public TypeName serializeFrom;

    public TypeName serializeTo;

    public MethodParam(Types typeUtils, Elements elementUtils, String name, VariableElement element) {
        this.typeUtils = typeUtils;
        this.elementUtils = elementUtils;

        this.name = name;
        this.element = element;
        this.type = TypeName.get(element.asType());

        detectType();

        Serializable serializable = element.getAnnotation(Serializable.class);
        if (serializable != null) {
            try {
                serializerType = TypeName.get(serializable.serializer());

                Pair<TypeName, TypeName> serializeDeserializeTypes = TypeUtils.getSerializeTypes(serializable.serializer());
                serializeFrom = serializeDeserializeTypes.first;
                serializeTo = serializeDeserializeTypes.second;
            } catch (MirroredTypeException e) {
                DeclaredType declaredType = (DeclaredType) e.getTypeMirror();
                serializerType = TypeName.get(declaredType);

                TypeElement typeElement = (TypeElement) declaredType.asElement();
                Pair<TypeName, TypeName> serializeDeserializeTypes = TypeUtils.getSerializeTypes(typeElement);
                serializeFrom = serializeDeserializeTypes.first;
                serializeTo = serializeDeserializeTypes.second;
            }
        }
    }

    private void detectType() {
        if (TypeUtils.isPrimitive(type)) {
            isPrimitive = true;
        } else if (TypeUtils.isPrimitiveArray(element)) {
            isPrimitiveArray = true;
        } else if (TypeUtils.isTypeString(type)) {
            isString = true;
        } else if (TypeUtils.isTypeBundle(type)) {
            isBundle = true;
        } else if (TypeUtils.isParcelableArrayList(typeUtils, elementUtils, element)) {
            isParcelableArrayList = true;
        } else if (TypeUtils.isParcelableArray(typeUtils, elementUtils, element)) {
            isParcelableArray = true;
        } else if (TypeUtils.isTypeParcelable(typeUtils, elementUtils, element)) {
            isParcelable = true;
        } else if (TypeUtils.isSerializable(typeUtils, elementUtils, element)) {
            isSerializable = true;
        }
    }

    public boolean hasSerializer() {
        return serializerType != null && !TypeName.get(DefaultSerializer.class).equals(serializerType);
    }

    public boolean isSupportedType() {
        return isPrimitive || isPrimitiveArray || isString || isBundle || isParcelable || isParcelableArray || isParcelableArrayList || isSerializable || hasSerializer();
    }

    public boolean needCast() {
        return isSerializable || isParcelable || isParcelableArray;
    }

}
