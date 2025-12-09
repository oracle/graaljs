/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Properties;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Performs a private brand check. If the brand check succeeds, returns the target object, otherwise
 * returns undefined.
 *
 * <ul>
 * <li>instance private methods: the object needs to contain the brand key of the class.
 * <li>static private methods: the object needs to be {@code ==} the constructor.
 * </ul>
 */
public abstract class PrivateBrandCheckNode extends JSTargetableNode {
    @Child @Executed protected JavaScriptNode targetNode;
    @Child @Executed protected JavaScriptNode brandNode;

    public static PrivateBrandCheckNode create(JavaScriptNode targetNode, JavaScriptNode brandNode) {
        return PrivateBrandCheckNodeGen.create(targetNode, brandNode);
    }

    protected PrivateBrandCheckNode(JavaScriptNode targetNode, JavaScriptNode brandNode) {
        this.targetNode = targetNode;
        this.brandNode = brandNode;
    }

    @Specialization
    Object doInstance(JSObject target, HiddenKey brandKey,
                    @Cached DynamicObject.ContainsKeyNode containsKey) {
        if (Properties.containsKey(containsKey, target, brandKey)) {
            return target;
        } else {
            return denied(target, brandKey);
        }
    }

    @Specialization
    Object doStatic(JSObject target, JSDynamicObject brand) {
        if (target == brand) {
            return target;
        } else {
            return denied(target, brand);
        }
    }

    @Fallback
    Object denied(@SuppressWarnings("unused") Object target, @SuppressWarnings("unused") Object brand) {
        // PrivateFieldGet/PrivateFieldSet will throw the TypeError.
        return Undefined.instance;
    }

    @Override
    public final JavaScriptNode getTarget() {
        return targetNode;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(targetNode, materializedTags), cloneUninitialized(brandNode, materializedTags));
    }
}
