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
package com.oracle.truffle.js.nodes.access;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.utilities.NeverValidAssumption;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.JSShape;

/**
 * Checks if a scope binding is present and guards against TDZ and const assignment.
 */
@ImportStatic(JSTruffleOptions.class)
public abstract class GlobalScopeLookupNode extends JavaScriptBaseNode {
    final JSContext context;
    final String varName;
    final boolean write;

    GlobalScopeLookupNode(JSContext context, String varName, boolean write) {
        this.context = context;
        this.varName = varName;
        this.write = write;
    }

    public static GlobalScopeLookupNode create(JSContext context, String varName, boolean write) {
        return GlobalScopeLookupNodeGen.create(context, varName, write);
    }

    public abstract boolean execute(Object scope);

    @SuppressWarnings("unused")
    @Specialization(assumptions = {"assumption"})
    static boolean doAbsent(DynamicObject scope,
                    @Cached("getAbsentPropertyAssumption(scope.getShape())") Assumption assumption) {
        return false;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"scope.getShape() == cachedShape"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "PropertyCacheLimit", replaces = "doAbsent")
    final boolean doCached(DynamicObject scope,
                    @Cached("scope.getShape()") Shape cachedShape,
                    @Cached("cachedShape.hasProperty(varName)") boolean exists,
                    @Cached("isDead(cachedShape)") boolean dead,
                    @Cached("isConstAssignment(cachedShape)") boolean constAssignment) {
        assert !exists || dead == (scope.get(varName) == Dead.instance());
        if (dead) {
            throw Errors.createReferenceErrorNotDefined(varName, this);
        }
        if (constAssignment) {
            throw Errors.createTypeErrorConstReassignment(varName, scope, this);
        }
        return exists;
    }

    @Specialization(replaces = "doCached")
    final boolean doUncached(DynamicObject scope,
                    @Cached("create()") BranchProfile errorBranch) {
        Property property = scope.getShape().getProperty(varName);
        if (property != null) {
            if (scope.get(varName) == Dead.instance()) {
                errorBranch.enter();
                throw Errors.createReferenceErrorNotDefined(varName, this);
            } else if (write && JSProperty.isConst(property)) {
                errorBranch.enter();
                throw Errors.createTypeErrorConstReassignment(varName, scope, this);
            }
            return true;
        }
        return false;
    }

    final boolean isDead(Shape shape) {
        Property property = shape.getProperty(varName);
        return property != null && property.getLocation().isDeclared();
    }

    final boolean isConstAssignment(Shape shape) {
        if (write) {
            Property property = shape.getProperty(varName);
            return property != null && JSProperty.isConst(property);
        }
        return false;
    }

    final Assumption getAbsentPropertyAssumption(Shape shape) {
        Property property = shape.getProperty(varName);
        if (property == null) {
            return JSShape.getPropertyAssumption(shape, varName);
        }
        return NeverValidAssumption.INSTANCE;
    }
}
