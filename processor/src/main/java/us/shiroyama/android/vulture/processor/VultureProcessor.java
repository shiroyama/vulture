package us.shiroyama.android.vulture.processor;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Message;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import us.shiroyama.android.vulture.PauseHandler;
import us.shiroyama.android.vulture.annotations.ObserveLifecycle;
import us.shiroyama.android.vulture.annotations.SafeCallback;
import us.shiroyama.android.vulture.processor.data.MethodParam;
import us.shiroyama.android.vulture.processor.utils.StringUtils;
import us.shiroyama.android.vulture.processor.utils.TypeUtils;

/**
 * Annotation Processor
 *
 * @author Fumihiko Shiroyama
 */

@AutoService(Processor.class)
public class VultureProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(ObserveLifecycle.class.getName(), SafeCallback.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        elementUtils = processingEnvironment.getElementUtils();
        typeUtils = processingEnvironment.getTypeUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
    }

    @TargetApi(24)
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (annotations.size() == 0) {
            return true;
        }

        roundEnvironment.getElementsAnnotatedWith(ObserveLifecycle.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .forEach(typeElement -> {
                    TypeElement originalClass = (TypeElement) typeElement;
                    String targetPackage = elementUtils.getPackageOf(originalClass).getQualifiedName().toString();

                    List<ExecutableElement> safeCallbackMethods = getSafeCallbackMethods(originalClass);
                    Map<ExecutableElement, String> methodToIdMap = getMethodToIdMap(safeCallbackMethods);
                    FieldSpec targetRefSpec = getTargetRefSpec(originalClass);
                    Map<ExecutableElement, List<MethodParam>> methodToParams = getMethodToParams(safeCallbackMethods);
                    TypeSpec pauseHandlerSpec = getConcretePauseHandler(methodToIdMap, methodToParams, targetRefSpec);
                    FieldSpec handlerFieldSpec = getHandlerFieldSpec(pauseHandlerSpec);

                    TypeSpec targetTypeSpec = TypeSpec
                            .classBuilder(getTargetClassName(originalClass))
                            .addModifiers(Modifier.FINAL)
                            .addFields(getIdFieldSpecs(methodToIdMap))
                            .addField(handlerFieldSpec)
                            .addField(targetRefSpec)
                            .addType(pauseHandlerSpec)
                            .addMethod(getRegisterMethodSpecs(originalClass, targetRefSpec, handlerFieldSpec))
                            .addMethod(getUnregisterMethodSpecs(handlerFieldSpec))
                            .addMethods(getMethodSpecs(handlerFieldSpec, methodToParams, methodToIdMap))
                            .build();
                    JavaFile javaFile = JavaFile
                            .builder(targetPackage, targetTypeSpec)
                            .addFileComment("This is auto-generated code. Do not modify this directly.")
                            .build();

                    try {
                        javaFile.writeTo(filer);
                    } catch (IOException e) {
                        messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                });

        return true;
    }

    @TargetApi(24)
    private List<ExecutableElement> getSafeCallbackMethods(TypeElement originalClass) {
        return originalClass.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .filter(method -> method.getAnnotation(SafeCallback.class) != null)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());
    }

    @TargetApi(24)
    private Map<ExecutableElement, String> getMethodToIdMap(List<ExecutableElement> safeCallbackMethods) {
        return safeCallbackMethods
                .stream()
                .collect(Collectors.toMap(
                        method -> method,
                        method -> StringUtils.camelToSnake(method.getSimpleName().toString()).toUpperCase()
                ));
    }

    @TargetApi(24)
    private List<FieldSpec> getIdFieldSpecs(Map<ExecutableElement, String> methodToIdMap) {
        AtomicInteger index = new AtomicInteger(0);
        return methodToIdMap.values()
                .stream()
                .map(value -> FieldSpec
                        .builder(int.class, value)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$L", index.getAndIncrement())
                        .build())
                .collect(Collectors.toList());
    }

    private FieldSpec getTargetRefSpec(TypeElement originalClass) {
        ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(ClassName.get(WeakReference.class), TypeName.get(originalClass.asType()));
        return FieldSpec
                .builder(parameterizedTypeName, "targetRef")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .build();
    }

    private FieldSpec getHandlerFieldSpec(TypeSpec handlerSpec) {
        return FieldSpec
                .builder(PauseHandler.class, "handler")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $N()", handlerSpec)
                .build();
    }

    @TargetApi(24)
    private Map<ExecutableElement, List<MethodParam>> getMethodToParams(List<ExecutableElement> methods) {
        return methods.stream()
                .collect(Collectors.toMap(
                        method -> method,
                        method -> {
                            AtomicInteger index = new AtomicInteger(0);
                            return method.getParameters()
                                    .stream()
                                    .map(varElm -> {
                                        String paramName = String.format(Locale.US, "arg%d", index.incrementAndGet());
                                        TypeName typeName = TypeName.get(varElm.asType());
                                        return MethodParam.of(paramName, typeName);
                                    })
                                    .collect(Collectors.toList());
                        }
                ));
    }

    private TypeSpec getConcretePauseHandler(Map<ExecutableElement, String> methodToIdMap, Map<ExecutableElement, List<MethodParam>> methodToParams, FieldSpec targetRef) {
        return TypeSpec
                .classBuilder(ClassName.get(PauseHandler.class).simpleName() + "Impl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(PauseHandler.class)
                .addMethod(getProcessMethodSpec(methodToIdMap, methodToParams, targetRef))
                .build();
    }

    @TargetApi(24)
    private MethodSpec getProcessMethodSpec(Map<ExecutableElement, String> methodToIdMap, Map<ExecutableElement, List<MethodParam>> methodToParams, FieldSpec targetRef) {
        ParameterSpec message = ParameterSpec.builder(Message.class, "message").build();
        MethodSpec.Builder methodBuilder = MethodSpec
                .methodBuilder("processMessage")
                .addModifiers(Modifier.PROTECTED)
                .addAnnotation(Override.class)
                .addParameter(message)
                .beginControlFlow("switch ($N.what)", message);

        methodToIdMap.entrySet()
                .stream()
                .forEach(entry -> {
                            ExecutableElement method = entry.getKey();
                            List<MethodParam> methodParams = methodToParams.get(method);
                            methodBuilder
                                    .addCode("case $L: ", entry.getValue())
                                    .addCode("{\n$>");
                            if (methodParams.size() > 0) {
                                methodBuilder.addStatement("$T data = message.getData()", Bundle.class);
                                methodParams.forEach(methodParam -> buildGetBundleStatement(methodBuilder, methodParam));
                            }
                            methodBuilder
                                    .beginControlFlow("if ($N.get() != null)", targetRef)
                                    .addStatement("$N.get().$L($L)", targetRef, method.getSimpleName(), String.join(",", methodParams.stream().map(p -> p.name).collect(Collectors.toList())))
                                    .endControlFlow()
                                    .addStatement("break")
                                    .addCode("$<}\n");
                        }
                );

        return methodBuilder.endControlFlow().build();
    }

    private MethodSpec getRegisterMethodSpecs(TypeElement originalClass, FieldSpec targetFieldSpec, FieldSpec handlerFieldSpec) {
        return MethodSpec
                .methodBuilder("register")
                .addModifiers(Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(originalClass.asType()), "target").build())
                .addStatement("$N = new $T<>($L)", targetFieldSpec, WeakReference.class, "target")
                .addStatement("$N.resume()", handlerFieldSpec)
                .build();
    }

    private MethodSpec getUnregisterMethodSpecs(FieldSpec handlerFieldSpec) {
        return MethodSpec
                .methodBuilder("unregister")
                .addModifiers(Modifier.STATIC)
                .addStatement("$N.pause()", handlerFieldSpec)
                .build();
    }

    @TargetApi(24)
    private List<MethodSpec> getMethodSpecs(FieldSpec handlerFieldSpec, Map<ExecutableElement, List<MethodParam>> methodToParams, Map<ExecutableElement, String> methodToIdMap) {
        return methodToParams.entrySet()
                .stream()
                .map(entry -> {
                    ExecutableElement method = entry.getKey();
                    List<MethodParam> methodParams = entry.getValue();
                    List<ParameterSpec> parameterSpecs = methodParams
                            .stream()
                            .map(signature -> ParameterSpec.builder(signature.type, signature.name).build())
                            .collect(Collectors.toList());

                    MethodSpec.Builder methodBuilder = MethodSpec
                            .methodBuilder(method.getSimpleName().toString() + "Safely")
                            .addModifiers(Modifier.STATIC)
                            .addParameters(parameterSpecs);

                    String id = methodToIdMap.get(method);
                    TypeName typeMessage = TypeName.get(Message.class);
                    methodBuilder.addStatement("$T handlerMessage = $N.obtainMessage($L)", typeMessage, handlerFieldSpec, id);

                    if (parameterSpecs.size() > 0) {
                        TypeName typeBundle = TypeName.get(Bundle.class);
                        methodBuilder.addStatement("$T data = new $T()", typeBundle, typeBundle);
                        parameterSpecs.forEach(parameterSpec -> buildPutBundleStatement(methodBuilder, parameterSpec));
                        methodBuilder.addStatement("handlerMessage.setData(data)");
                    }

                    methodBuilder.addStatement("$N.sendMessage(handlerMessage)", handlerFieldSpec);
                    return methodBuilder.build();
                })
                .collect(Collectors.toList());
    }

    private void buildGetBundleStatement(MethodSpec.Builder methodBuilder, MethodParam methodParam) {
        TypeName typeName = methodParam.type;
        if (TypeUtils.isTypeBoolean(typeName)) {
            methodBuilder.addStatement("$T $L = data.getBoolean($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeByte(typeName)) {
            methodBuilder.addStatement("$T $L = data.getByte($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeShort(typeName)) {
            methodBuilder.addStatement("$T $L = data.getShort($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeChar(typeName)) {
            methodBuilder.addStatement("$T $L = data.getCar($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeInt(typeName)) {
            methodBuilder.addStatement("$T $L = data.getInt($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeLong(typeName)) {
            methodBuilder.addStatement("$T $L = data.getLong($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeFloat(typeName)) {
            methodBuilder.addStatement("$T $L = data.getFloat($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeDouble(typeName)) {
            methodBuilder.addStatement("$T $L = data.getDouble($S)", typeName, methodParam.name, methodParam.name);
        } else if (TypeUtils.isTypeString(typeName)) {
            methodBuilder.addStatement("$T $L = data.getString($S)", typeName, methodParam.name, methodParam.name);
        } else {
            throw new IllegalArgumentException(String.format("TypeName %s is not supported.", typeName.toString()));
        }
    }

    private void buildPutBundleStatement(MethodSpec.Builder methodBuilder, ParameterSpec parameterSpec) {
        TypeName typeName = parameterSpec.type;
        if (TypeUtils.isTypeBoolean(typeName)) {
            methodBuilder.addStatement("data.putBoolean($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeByte(typeName)) {
            methodBuilder.addStatement("data.putByte($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeShort(typeName)) {
            methodBuilder.addStatement("data.putShort($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeChar(typeName)) {
            methodBuilder.addStatement("data.putChar($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeInt(typeName)) {
            methodBuilder.addStatement("data.putInt($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeLong(typeName)) {
            methodBuilder.addStatement("data.putLong($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeFloat(typeName)) {
            methodBuilder.addStatement("data.putFloat($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeDouble(typeName)) {
            methodBuilder.addStatement("data.putDouble($S, $L)", parameterSpec.name, parameterSpec.name);
        } else if (TypeUtils.isTypeString(typeName)) {
            methodBuilder.addStatement("data.putString($S, $L)", parameterSpec.name, parameterSpec.name);
        } else {
            throw new IllegalArgumentException(String.format("TypeName %s is not supported.", typeName.toString()));
        }
    }

    private String getTargetClassName(TypeElement originalClass) {
        return "Safe" + originalClass.getSimpleName();
    }

}
