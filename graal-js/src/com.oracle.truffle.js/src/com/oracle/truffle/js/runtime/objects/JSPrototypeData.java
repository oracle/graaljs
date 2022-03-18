/*
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.builtins.JSClass;

/**
 * Extra data associated with prototype objects.
 */
public final class JSPrototypeData {
    private static final Shape[] EMPTY_SHAPE_ARRAY = new Shape[0];

    /** Copy-on-write array. Not volatile to prevent SpotBugs warning. */
    @SuppressWarnings("unused") private Shape[] protoChildTrees = EMPTY_SHAPE_ARRAY;

    private static final VarHandle PROTO_CHILD_TREES_VAR_HANDLE;
    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            PROTO_CHILD_TREES_VAR_HANDLE = lookup.findVarHandle(JSPrototypeData.class, "protoChildTrees", Shape[].class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw Errors.shouldNotReachHere(e);
        }
    }

    public JSPrototypeData() {
    }

    private static Shape lookupShapeByType(Shape[] shapes, JSClass jsclass) {
        for (Shape shape : shapes) {
            if (JSShape.getJSClassNoCast(shape) == jsclass) {
                return shape;
            }
        }
        return null;
    }

    public Shape getProtoChildTree(JSClass jsclass) {
        return lookupShapeByType(getProtoChildTrees(), jsclass);
    }

    public Shape getOrAddProtoChildTree(JSClass jsclass, Shape newRootShape) {
        CompilerAsserts.neverPartOfCompilation();
        while (true) {
            Shape[] oldArray = getProtoChildTrees();
            Shape existingRootShape = lookupShapeByType(oldArray, jsclass);
            if (existingRootShape != null) {
                return existingRootShape;
            }
            Shape[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
            newArray[oldArray.length] = newRootShape;
            if (PROTO_CHILD_TREES_VAR_HANDLE.compareAndSet(this, oldArray, newArray)) {
                return newRootShape;
            }
        }
    }

    private Shape[] getProtoChildTrees() {
        return (Shape[]) PROTO_CHILD_TREES_VAR_HANDLE.getVolatile(this);
    }
}
