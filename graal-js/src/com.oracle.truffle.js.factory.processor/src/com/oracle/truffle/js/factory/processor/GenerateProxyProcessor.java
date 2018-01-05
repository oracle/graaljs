/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.factory.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.js.annotations.GenerateProxy;

public class GenerateProxyProcessor extends AbstractFactoryProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateProxy.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateProxy.class)) {
            processElement((TypeElement) element);
        }

        return true;
    }

    private void processElement(TypeElement element) {
        generateProxy(element);
    }

    public void generateProxy(TypeElement typeElement) {
        String nodeFactoryClassName = typeElement.getQualifiedName().toString();
        String packageName = getPackageName(typeElement);
        String simpleClassName = nodeFactoryClassName.substring(nodeFactoryClassName.lastIndexOf('.') + 1, nodeFactoryClassName.length()) + "ProxyGen";
        String qualifiedClassName = packageName + "." + simpleClassName;
        String generatedByClassName = GeneratedBy.class.getCanonicalName();
        String proxyHandlerClassName = InvocationHandler.class.getCanonicalName();
        String methodClassName = Method.class.getCanonicalName();
        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(qualifiedClassName, typeElement);
            try (OutputStream outputStream = jfo.openOutputStream(); PrintStream ps = new PrintStream(outputStream)) {
                ps.println("package " + packageName + ";");
                ps.println();

                ps.println("@" + generatedByClassName + "(" + nodeFactoryClassName + ".class)");
                ps.println("public class " + simpleClassName + " " + (isInterface(typeElement) ? "implements" : "extends") + " " + nodeFactoryClassName + " {");
                ps.println("private final " + proxyHandlerClassName + " handler;");
                ps.println("private " + simpleClassName + "(" + proxyHandlerClassName + " handler) {");
                ps.println("this.handler = handler;");
                ps.println("}");
                ps.println();
                ps.println("public static " + nodeFactoryClassName + " create(" + proxyHandlerClassName + " handler) {");
                ps.println("return new " + simpleClassName + "(handler);");
                ps.println("}");
                ps.println();

                List<ExecutableElement> publicMethods = getOverridableMethods(typeElement);

                ps.println("private final " + methodClassName + " methods[] = new " + methodClassName + "[" + publicMethods.size() + "];");

                if (!publicMethods.isEmpty()) {
                    for (int i = 0; i < publicMethods.size(); i++) {
                        ExecutableElement method = publicMethods.get(i);
                        int arity = method.getParameters().size();
                        ps.println();

                        ps.println("@Override");
                        String paramList = IntStream.range(0, arity).mapToObj(ai -> {
                            TypeMirror type = method.getParameters().get(ai).asType();
                            if (method.isVarArgs() && ai == arity - 1) {
                                return ((ArrayType) type).getComponentType() + "... " + "arg" + ai;
                            }
                            return type + " " + "arg" + ai;
                        }).collect(Collectors.joining(", "));
                        ps.println("public " + method.getReturnType() + " " + method.getSimpleName() + " (" + paramList + ") {");
                        ps.println(methodClassName + " method = methods[" + i + "];");
                        String paramTypes = IntStream.range(0, arity).mapToObj(ai -> getClassLiteralString(method.getParameters().get(ai).asType())).collect(Collectors.joining(", "));
                        ps.println("if (method == null) {");
                        ps.println("try {");
                        ps.println("method = " + nodeFactoryClassName + ".class.getMethod(\"" + method.getSimpleName() + "\"" + (paramTypes.isEmpty() ? "" : ", " + paramTypes) + ");");
                        ps.println("methods[" + i + "] = method;");
                        ps.println("} catch (NoSuchMethodException e) {");
                        ps.println("throw new AssertionError(e);");
                        ps.println("}");
                        ps.println("}");
                        String args = IntStream.range(0, arity).mapToObj(ai -> "arg" + ai).collect(Collectors.joining(", "));
                        ps.println("Object[] args = new Object[]{" + args + "};");
                        ps.println("try {");
                        if (method.getReturnType().getKind() != TypeKind.VOID) {
                            ps.println("return " + cast(method.getReturnType()) + "handler.invoke(this, method, args);");
                        } else {
                            ps.println("handler.invoke(this, method, args);");
                        }
                        ps.println("} catch (RuntimeException | Error e) {");
                        ps.println("throw e;");
                        ps.println("} catch (Throwable e) {");
                        ps.println("throw new RuntimeException(e);");
                        ps.println("}");
                        ps.println("}");
                    }
                }

                ps.println("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String cast(TypeMirror returnType) {
        return returnType.toString().equals("java.lang.Object") ? "" : "(" + returnType.toString() + ") ";
    }
}
