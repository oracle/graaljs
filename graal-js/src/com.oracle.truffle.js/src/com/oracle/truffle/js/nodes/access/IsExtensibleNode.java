/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSShape;

/**
 * Implements abstract operation IsExtensible.
 */
@GenerateUncached
@ImportStatic({JSShape.class})
public abstract class IsExtensibleNode extends JavaScriptBaseNode {

    protected IsExtensibleNode() {
    }

    public abstract boolean executeBoolean(DynamicObject obj);

    @SuppressWarnings("unused")
    @Specialization(guards = {"getJSClass(cachedShape).usesOrdinaryIsExtensible()", "cachedShape.check(object)"}, limit = "1")
    protected static boolean doCachedShape(DynamicObject object,
                    @Cached("object.getShape()") Shape cachedShape,
                    @Cached("isExtensible(cachedShape)") boolean result) {
        return result;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"cachedJSClass.usesOrdinaryIsExtensible()", "cachedJSClass.isInstance(object)"}, limit = "1", replaces = "doCachedShape")
    protected static boolean doCachedJSClass(DynamicObject object,
                    @Cached("getJSClassChecked(object)") JSClass cachedJSClass,
                    @Cached("createBinaryProfile()") @Shared("resultProfile") ConditionProfile resultProfile) {
        return resultProfile.profile(JSShape.isExtensible(object.getShape()));
    }

    @Specialization(replaces = {"doCachedJSClass"})
    protected static boolean doUncached(DynamicObject object,
                    @Cached("createBinaryProfile()") @Shared("resultProfile") ConditionProfile resultProfile) {
        return resultProfile.profile(JSObject.isExtensible(object));
    }

    public static IsExtensibleNode create() {
        return IsExtensibleNodeGen.create();
    }
}
