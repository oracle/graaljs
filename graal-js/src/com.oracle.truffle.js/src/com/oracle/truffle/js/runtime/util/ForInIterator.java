/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.util;

import java.util.Arrays;
import java.util.List;

import org.graalvm.collections.EconomicSet;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.runtime.Boundaries;

public class ForInIterator {
    public DynamicObject object;
    public Shape objectShape;
    public boolean objectWasVisited;
    public EconomicSet<Object> visitedKeys;
    public List<?> remainingKeys;
    public int remainingKeysSize;
    public int remainingKeysIndex;
    public Shape[] visitedShapes;
    public int visitedShapesSize;
    public boolean fastOwnKeys;
    public int protoDepth;
    public final boolean iterateValues;

    public ForInIterator(DynamicObject obj, boolean iterateValues) {
        this.object = obj;
        this.iterateValues = iterateValues;
        this.visitedShapes = new Shape[4];
    }

    public void addVisitedShape(Shape shape, BranchProfile growBranch) {
        if (visitedShapesSize >= visitedShapes.length) {
            growBranch.enter();
            visitedShapes = Arrays.copyOf(visitedShapes, visitedShapes.length * 2);
        }
        visitedShapes[visitedShapesSize++] = shape;
    }

    // no boundary to allow EA to escape foreign objects
    public boolean addVisitedKey(final Object key) {
        if (visitedKeys == null) {
            visitedKeys = Boundaries.economicSetCreate();
        }
        return Boundaries.economicSetAdd(visitedKeys, key);
    }

    public boolean isVisitedKey(final Object key) {
        return (visitedShapesSize > 0 && visitedShapeSetContainsKey(visitedShapes, visitedShapesSize, key)) ||
                        (visitedKeys != null && Boundaries.economicSetContains(visitedKeys, key));
    }

    @TruffleBoundary
    private static boolean visitedShapeSetContainsKey(Shape[] visitedShapes, int size, Object key) {
        for (int i = 0; i < size; i++) {
            Shape visitedShape = visitedShapes[i];
            if (visitedShape.hasProperty(key)) {
                return true;
            }
        }
        return false;
    }
}
