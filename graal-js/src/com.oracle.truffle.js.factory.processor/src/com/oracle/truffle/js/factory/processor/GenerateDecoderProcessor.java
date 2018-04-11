/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.CRC32;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.js.annotations.GenerateDecoder;
import com.oracle.truffle.js.codec.NodeDecoder;

public class GenerateDecoderProcessor extends AbstractFactoryProcessor {
    private TypeMirror objectTypeMirror;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateDecoder.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateDecoder.class)) {
            processElement((TypeElement) element);
        }

        return true;
    }

    private TypeMirror getObjectTypeMirror() {
        if (objectTypeMirror != null) {
            return objectTypeMirror;
        }
        return objectTypeMirror = processingEnv.getElementUtils().getTypeElement("java.lang.Object").asType();
    }

    private void processElement(TypeElement element) {
        generateDecoder(element);
    }

    private void generateClassesGetter(List<ExecutableElement> methods, PrintStream ps) {
        Stream<TypeMirror> enumTypes = methods.stream().flatMap(m -> m.getParameters().stream().map(p -> p.asType()).filter(pt -> !pt.getKind().isPrimitive())).distinct().filter(
                        pt -> isEnumClass(pt));
        Stream<TypeMirror> componentTypes = methods.stream().flatMap(m -> m.getParameters().stream().map(p -> p.asType()).filter(pt -> pt.getKind() == TypeKind.ARRAY)).map(
                        t -> ((ArrayType) t).getComponentType());
        List<TypeMirror> types = Stream.concat(enumTypes, componentTypes).map(t -> erasure(t)).distinct().sorted(Comparator.comparing(t -> !t.getKind().isPrimitive())).collect(Collectors.toList());
        if (!types.isEmpty()) {
            ps.println("private static final Class<?>[] CLASSES = {" + types.stream().map(t -> getClassLiteralString(t)).collect(Collectors.joining(", ")) + "};");
            ps.println();
            ps.println("@Override");
            ps.println("public Class<?>[] getClasses() {");
            ps.println("return CLASSES;");
            ps.println("}");
            ps.println();
        }
    }

    private boolean isEnumClass(TypeMirror pt) {
        return processingEnv.getTypeUtils().directSupertypes(pt).stream().anyMatch(st -> st.toString().startsWith("java.lang.Enum"));
    }

    public void generateDecoder(TypeElement typeElement) {
        String nodeFactoryClassName = typeElement.getQualifiedName().toString();
        String packageName = getPackageName(typeElement);
        String simpleClassName = nodeFactoryClassName.substring(nodeFactoryClassName.lastIndexOf('.') + 1, nodeFactoryClassName.length()) + "DecoderGen";
        String qualifiedClassName = packageName + "." + simpleClassName;
        String nodeDecoderClassName = NodeDecoder.class.getCanonicalName();
        String generatedByClassName = GeneratedBy.class.getCanonicalName();
        String decoderStateClassName = "DecoderState";
        String decoderStateName = "decoder";
        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(qualifiedClassName, typeElement);
            try (OutputStream outputStream = jfo.openOutputStream(); PrintStream ps = new PrintStream(outputStream)) {
                ps.println("package " + packageName + ";");
                ps.println();

                ps.println("@" + generatedByClassName + "(" + nodeFactoryClassName + ".class)");
                ps.println("public class " + simpleClassName + " " + "implements" + " " + nodeDecoderClassName + "<" + nodeFactoryClassName + ">" + " {");
                ps.println("private " + simpleClassName + "() {");
                ps.println("}");
                ps.println();
                ps.println("public static " + simpleClassName + " create() {");
                ps.println("return new " + simpleClassName + "();");
                ps.println("}");
                ps.println();

                List<ExecutableElement> publicMethods = getOverridableMethods(typeElement);

                generateClassesGetter(publicMethods, ps);

                ps.println("@Override");
                if (publicMethods.stream().anyMatch(method -> method.getParameters().stream().anyMatch(
                                param -> param.asType().getKind() == TypeKind.DECLARED && !((DeclaredType) param.asType()).getTypeArguments().isEmpty()))) {
                    ps.println("@SuppressWarnings(\"unchecked\")");
                }
                ps.println("public Object decodeNode(" + decoderStateClassName + " " + decoderStateName + ", " + nodeFactoryClassName + " nodeFactory" + ") {");
                ps.println("switch (decoder.getUInt()) {");
                for (int i = 0; i < publicMethods.size(); i++) {
                    ExecutableElement method = publicMethods.get(i);
                    int arity = method.getParameters().size();
                    ps.println("case " + i + ":");
                    String nodeExpr = "nodeFactory." + method.getSimpleName() +
                                    IntStream.range(0, arity).mapToObj(ai -> (isAssignable(method.getParameters().get(ai).asType(), getObjectTypeMirror()) ? ""
                                                    : "(" + method.getParameters().get(ai).asType() + ")") + getObjReg()).collect(Collectors.joining(", ", "(", ")"));
                    if (method.getReturnType().getKind() != TypeKind.VOID) {
                        ps.println("return " + nodeExpr + ";");
                    } else {
                        ps.println(nodeExpr + ";");
                        ps.println("return null;");
                    }
                }
                ps.println("default:");
                ps.println("throw new IllegalArgumentException(\"unknown node id\");");
                ps.println("}");
                ps.println("}");

                ps.println();
                ps.println("@Override");
                ps.println("public int getMethodIdFromSignature(String signature) {");
                ps.println("return EncoderSupport.getMethodIdFromSignature(signature);");
                ps.println("}");

                CRC32 crc32 = new CRC32();
                // In separate class to facilitate lazy loading.
                ps.println();
                ps.println("private static class EncoderSupport {");
                ps.println("static int getMethodIdFromSignature(String signature) {");
                ps.println("switch (signature) {");
                for (int i = 0; i < publicMethods.size(); i++) {
                    ExecutableElement method = publicMethods.get(i);
                    String methodSignature = getMethodSignature(method);
                    ps.println("case \"" + methodSignature + "\":");
                    ps.println("return " + i + ";");

                    crc32.update(methodSignature.getBytes(StandardCharsets.UTF_8));
                }
                ps.println("default:");
                ps.println("throw new IllegalArgumentException(\"unknown method: \" + signature);");
                ps.println("}");
                ps.println("}");
                ps.println("}");

                int checksum = (int) crc32.getValue();
                ps.println();
                ps.println("@Override");
                ps.println("public int getChecksum() {");
                ps.println("return " + checksum + ";");
                ps.println("}");

                ps.println("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getMethodSignature(ExecutableElement method) {
        return method.getSimpleName() + method.getParameters().stream().map(p -> p.asType()).map(t -> getTypeSignature(t)).collect(Collectors.joining(",", "(", ")")) +
                        getTypeSignature(method.getReturnType());
    }

    private static String getTypeSignature(TypeMirror type) {
        return getErasedTypeName(type);
    }

    private static String getObjReg() {
        return "decoder.getObject()";
    }

    private boolean isAssignable(TypeMirror toType, TypeMirror fromType) {
        return toType.equals(fromType) || (toType.equals(getObjectTypeMirror()) && fromType.getKind().isPrimitive()) || processingEnv.getTypeUtils().isAssignable(fromType, toType);
    }
}
