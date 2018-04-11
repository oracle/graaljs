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
