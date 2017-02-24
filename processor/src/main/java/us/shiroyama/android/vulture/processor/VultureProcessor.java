package us.shiroyama.android.vulture.processor;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.os.Message;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
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

import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeBoolean;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeBundle;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeByte;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeChar;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeDouble;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeFloat;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeInt;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeLong;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeParcelable;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeShort;
import static us.shiroyama.android.vulture.processor.utils.TypeUtils.isTypeString;

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
        if (annotations.isEmpty()) {
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
                                        return MethodParam.of(paramName, typeName, varElm);
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
                                String receiverName = "data";
                                methodBuilder.addStatement("$T $L = message.getData()", Bundle.class, receiverName);
                                methodParams.forEach(methodParam -> buildGetBundleStatement(receiverName, methodBuilder, methodParam));
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
                            .map(param -> {
                                List<AnnotationSpec> annotationSpecs = param.element.getAnnotationMirrors()
                                        .stream()
                                        .map(AnnotationSpec::get)
                                        .collect(Collectors.toList());
                                return ParameterSpec
                                        .builder(param.type, param.name)
                                        .addAnnotations(annotationSpecs).build();
                            })
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
                        String receiverName = "data";
                        methodBuilder.addStatement("$T $L = new $T()", typeBundle, receiverName, typeBundle);
                        parameterSpecs.forEach(parameterSpec -> buildPutBundleStatement(receiverName, parameterSpec, methodBuilder));
                        methodBuilder.addStatement("handlerMessage.setData(data)");
                    }

                    methodBuilder.addStatement("$N.sendMessage(handlerMessage)", handlerFieldSpec);
                    return methodBuilder.build();
                })
                .collect(Collectors.toList());
    }

    private boolean isSupportedType(TypeName typeName) {
        return isTypeBoolean(typeName) || isTypeByte(typeName) || isTypeShort(typeName) || isTypeChar(typeName) || isTypeInt(typeName) || isTypeLong(typeName) || isTypeFloat(typeName) || isTypeDouble(typeName) || isTypeString(typeName) || isTypeBundle(typeName) || isTypeParcelable(typeName);
    }

    private void buildGetBundleStatement(String receiverName, MethodSpec.Builder methodBuilder, MethodParam methodParam) {
        TypeName typeName = methodParam.type;

        if (!isSupportedType(typeName)) {
            throw new IllegalArgumentException(String.format("TypeName %s is not supported.", typeName.toString()));
        }

        String getMethod = String.format("get%s", ((ClassName) typeName.box()).simpleName());
        methodBuilder.addStatement("$T $L = $L.$L($S)", typeName, methodParam.name, receiverName, getMethod, methodParam.name);
    }

    private void buildPutBundleStatement(String receiverName, ParameterSpec parameterSpec, MethodSpec.Builder methodBuilder) {
        TypeName typeName = parameterSpec.type;

        if (!isSupportedType(typeName)) {
            throw new IllegalArgumentException(String.format("TypeName %s is not supported.", typeName.toString()));
        }

        String putMethod = String.format("put%s", ((ClassName) typeName.box()).simpleName());
        methodBuilder.addStatement("$L.$L($S, $L)", receiverName, putMethod, parameterSpec.name, parameterSpec.name);
    }

    private String getTargetClassName(TypeElement originalClass) {
        return "Safe" + originalClass.getSimpleName();
    }

}
