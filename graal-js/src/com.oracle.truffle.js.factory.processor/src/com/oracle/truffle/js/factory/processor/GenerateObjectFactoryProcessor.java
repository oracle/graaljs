/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.factory.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.js.annotations.GenerateObjectFactory;

public class GenerateObjectFactoryProcessor extends AbstractFactoryProcessor {

    static final int SPACES = 4;

    private static final String Shape = "com.oracle.truffle.api.object.Shape";
    private static final String JSDynamicObject = "com.oracle.truffle.js.runtime.objects.JSDynamicObject";
    private static final String JSObject = "com.oracle.truffle.js.runtime.objects.JSObject";
    private static final String JSObjectFactory = "com.oracle.truffle.js.runtime.builtins.JSObjectFactory";
    private static final String JSRealm = "com.oracle.truffle.js.runtime.JSRealm";

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateObjectFactory.class.getCanonicalName());
    }

    protected final boolean typeNameEquals(TypeMirror type, String name) {
        if (type.getKind() == TypeKind.DECLARED) {
            return typeNameEquals(asTypeElement(type), name);
        }
        return false;
    }

    protected static boolean typeNameEquals(TypeElement type, String name) {
        return type.getQualifiedName().contentEquals(name);
    }

    protected final DeclaredType getDeclaredType(String element) {
        TypeElement type = Objects.requireNonNull(processingEnv.getElementUtils().getTypeElement(element), element);
        return (DeclaredType) type.asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateObjectFactory.class)) {
            assert element.getKind() == ElementKind.CONSTRUCTOR : element;
            processElement((TypeElement) element.getEnclosingElement());
        }

        return true;
    }

    private void processElement(TypeElement element) {
        var superclasses = Stream.iterate(element, e -> e.getSuperclass().getKind() != TypeKind.NONE, e -> asTypeElement(e.getSuperclass()));
        if (superclasses.anyMatch(e -> typeNameEquals(e, JSObject))) {
            generateFactoryClass(element);
        } else {
            printError("Not a JSObject subclass", element);
        }
    }

    private void printError(String msg, TypeElement typeElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, typeElement);
    }

    public void generateFactoryClass(TypeElement typeElement) {
        String objectClassName = typeElement.getQualifiedName().toString();
        String simpleClassName = objectClassName.substring(objectClassName.lastIndexOf('.') + 1, objectClassName.length()) + "Factory";
        String packageName = getPackageName(typeElement);
        String qualifiedClassName = packageName + "." + simpleClassName;
        String generatedByClassName = GeneratedBy.class.getCanonicalName();

        boolean constructorFound = false;
        List<String> executables = new ArrayList<>();

        String privateConstructor = "private %s() {\n}".formatted(simpleClassName);
        executables.add(privateConstructor);

        for (var constructor : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            // Ignore private constructors
            if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                continue;
            }

            // Only consider annotated constructors
            if (constructor.getAnnotation(GenerateObjectFactory.class) == null) {
                continue;
            }

            // Requires a leading Shape parameter
            var parameters = constructor.getParameters();
            if (parameters.size() == 0 || !typeNameEquals(parameters.get(0).asType(), Shape)) {
                continue;
            }
            constructorFound = true;

            List<VariableElement> declaredParams = new ArrayList<>();
            List<VariableElement> forwardedParams = new ArrayList<>();
            Optional<String> protoParam = Optional.empty();
            for (int i = 1; i < parameters.size(); i++) {
                var parameter = parameters.get(i);
                if (typeNameEquals(parameter.asType(), JSDynamicObject) &&
                                parameter.getSimpleName().contentEquals("prototype") || parameter.getSimpleName().contentEquals("proto")) {
                    protoParam = Optional.of(parameter.getSimpleName().toString());
                } else {
                    declaredParams.add(parameter);
                }
                forwardedParams.add(parameter);
            }

            String shapeName = "shape";
            String protoName = protoParam.orElse("proto");
            String factoryName = "factory";
            String realmName = "realm";
            String objName = "obj";
            for (boolean withProto : List.of(false, true)) {
                var factoryMethod = new StringBuilder();
                String parameterPairsIn = Stream.concat(Stream.concat(Stream.of(JSObjectFactory + " " + factoryName, JSRealm + " " + realmName),
                                withProto ? Stream.of(JSDynamicObject + " " + protoName) : Stream.empty()),
                                declaredParams.stream().map(p -> getErasedTypeName(p.asType()) + " " + p.getSimpleName().toString())). //
                                collect(Collectors.joining(",\n", "\n", "")).indent(SPACES * 2).stripTrailing();
                String parameterNamesOut = Stream.concat(Stream.of(shapeName),
                                forwardedParams.stream().map(p -> p.getSimpleName().toString())).collect(Collectors.joining(", "));
                factoryMethod.append("public static %s create(%s) {".formatted(objectClassName, parameterPairsIn).indent(0));
                StringJoiner body = new StringJoiner("\n");
                if (!withProto) {
                    body.add("var %s = %s.getPrototype(%s);".formatted(protoName, factoryName, realmName));
                }
                body.add("var %s = %s.getShape(%s, %s);".formatted(shapeName, factoryName, realmName, protoName));
                body.add("var %s = new %s(%s);".formatted(objName, objectClassName, parameterNamesOut));
                body.add("%s.initProto(%s, %s, %s);".formatted(factoryName, objName, realmName, protoName));
                body.add("%s.trackAllocation(%s);".formatted(factoryName, objName));
                body.add("return %s;".formatted(objName));
                factoryMethod.append(body.toString().indent(SPACES));
                factoryMethod.append("}");
                executables.add(factoryMethod.toString());
            }
        }

        if (!constructorFound) {
            printError("No suitable constructors found. At least one accessible constructor with a leading Shape parameter is required.", typeElement);
        }

        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(qualifiedClassName, typeElement);
            try (OutputStream outputStream = jfo.openOutputStream(); PrintStream ps = new PrintStream(outputStream, false, StandardCharsets.UTF_8)) {
                ps.println("package " + packageName + ";");
                ps.println();

                var cls = new StringBuilder();
                cls.append("@%s(%s.class)".formatted(generatedByClassName, objectClassName).indent(0));
                cls.append("public final class %s {".formatted(simpleClassName).indent(0));
                cls.append('\n');

                cls.append(executables.stream().collect(Collectors.joining("\n\n")).indent(SPACES));

                cls.append("}");

                ps.println(cls.toString().stripIndent());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
