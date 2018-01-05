/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.factory.processor;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.AbstractProcessor;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public abstract class AbstractFactoryProcessor extends AbstractProcessor {
    @Override
    public final SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    protected final List<ExecutableElement> getOverridableMethods(TypeElement typeElement) {
        List<ExecutableElement> list = new ArrayList<>();
        collectOverridableMethods(typeElement, list);
        return list;
    }

    private void collectOverridableMethods(TypeElement typeElement, List<ExecutableElement> list) {
        if (!(typeElement.getSuperclass() instanceof NoType || typeElement.getSuperclass().toString().equals("java.lang.Object"))) {
            collectOverridableMethods((TypeElement) processingEnv.getTypeUtils().asElement(typeElement.getSuperclass()), list);
        }
        typeElement.getInterfaces().forEach(intf -> collectOverridableMethods((TypeElement) processingEnv.getTypeUtils().asElement(intf), list));
        ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream().filter(m -> isOverridable(m)).forEach(list::add);
    }

    protected static boolean isInterface(TypeElement typeElement) {
        return (typeElement.getSuperclass() instanceof NoType && !typeElement.toString().equals("java.lang.Object"));
    }

    private static boolean isOverridable(ExecutableElement m) {
        return m.getModifiers().contains(Modifier.PUBLIC) && !m.getModifiers().contains(Modifier.STATIC) && !m.getModifiers().contains(Modifier.FINAL);
    }

    protected static String getClassLiteralString(TypeMirror type) {
        return getErasedTypeName(type) + ".class";
    }

    protected static String getErasedTypeName(TypeMirror type) {
        String qualifiedName;
        if (type.getKind() == TypeKind.DECLARED) {
            qualifiedName = ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
        } else if (type.getKind() == TypeKind.ARRAY) {
            qualifiedName = getErasedTypeName(((ArrayType) type).getComponentType()) + "[]";
        } else {
            qualifiedName = type.toString();
        }
        return qualifiedName;
    }

    protected final TypeMirror erasure(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED || type.getKind() == TypeKind.ARRAY) {
            return processingEnv.getTypeUtils().erasure(type);
        }
        return type;
    }

    protected final String getPackageName(TypeElement typeElement) {
        return processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
    }
}
