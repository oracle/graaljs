/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.objects.Dead;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;

/**
 * Looks up global lexical bindings, with explicit operations for checking TDZ and const assignment.
 * Lookup and validation share the same shape cache.
 */
public abstract class GlobalScopeLookupNode extends JavaScriptBaseNode {
    final TruffleString varName;

    enum Checks {
        NONE,
        TDZ,
        TDZ_AND_CONST
    }

    GlobalScopeLookupNode(TruffleString varName) {
        this.varName = varName;
    }

    public static GlobalScopeLookupNode create(TruffleString varName) {
        return GlobalScopeLookupNodeGen.create(varName);
    }

    public final boolean hasBinding(Object scope) {
        return execute(scope, Checks.NONE);
    }

    public final void checkTDZ(Object scope) {
        execute(scope, Checks.TDZ);
    }

    public final void checkWrite(Object scope) {
        execute(scope, Checks.TDZ_AND_CONST);
    }

    protected abstract boolean execute(Object scope, Checks checks);

    @SuppressWarnings("unused")
    @Specialization(guards = {"scope.getShape() == cachedShape"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "cacheLimit")
    final boolean doCached(JSDynamicObject scope, Checks checks,
                    @Cached("scope.getShape()") Shape cachedShape,
                    @Cached("cachedShape.hasProperty(varName)") boolean exists,
                    @Cached("isDead(cachedShape)") boolean dead,
                    @Cached("isConstBinding(cachedShape)") boolean constBinding,
                    @Cached("getPropertyCacheLimit()") int cacheLimit) {
        assert !exists || dead == (JSDynamicObject.getOrNull(scope, varName) == Dead.instance());
        if (checks != Checks.NONE && dead) {
            throw Errors.createReferenceErrorNotDefined(varName, this);
        }
        if (checks == Checks.TDZ_AND_CONST && constBinding) {
            throw Errors.createTypeErrorConstReassignment(varName, this);
        }
        return exists;
    }

    protected int getPropertyCacheLimit() {
        return getLanguage().getJSContext().getPropertyCacheLimit();
    }

    @Specialization(replaces = "doCached")
    final boolean doUncached(JSDynamicObject scope, Checks checks,
                    @Cached InlinedBranchProfile errorBranch) {
        Property property = scope.getShape().getProperty(varName);
        if (property != null) {
            if (checks != Checks.NONE && JSDynamicObject.getOrNull(scope, varName) == Dead.instance()) {
                errorBranch.enter(this);
                throw Errors.createReferenceErrorNotDefined(varName, this);
            } else if (checks == Checks.TDZ_AND_CONST && JSProperty.isConst(property)) {
                errorBranch.enter(this);
                throw Errors.createTypeErrorConstReassignment(varName, this);
            }
            return true;
        }
        return false;
    }

    final boolean isDead(Shape shape) {
        Property property = shape.getProperty(varName);
        assert property == null || !property.getLocation().isConstant() || property.getLocation().getConstantValue() == Dead.instance();
        return property != null && property.getLocation().isConstant();
    }

    final boolean isConstBinding(Shape shape) {
        Property property = shape.getProperty(varName);
        return property != null && JSProperty.isConst(property);
    }
}
