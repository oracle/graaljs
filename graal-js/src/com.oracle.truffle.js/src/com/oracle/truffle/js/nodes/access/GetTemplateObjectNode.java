/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.js.nodes.*;
import com.oracle.truffle.js.runtime.*;
import com.oracle.truffle.js.runtime.objects.*;

import java.util.Set;

/**
 * ES6 12.2.9.3 Runtime Semantics: GetTemplateObject(templateLiteral).
 */
public abstract class GetTemplateObjectNode extends JavaScriptNode {
    protected final JSContext context;
    @Child private ArrayLiteralNode rawStrings;
    @Child private ArrayLiteralNode cookedStrings;
    private final Object identity;

    protected GetTemplateObjectNode(JSContext context, ArrayLiteralNode rawStrings, ArrayLiteralNode cookedStrings) {
        this.context = context;
        this.rawStrings = rawStrings;
        this.cookedStrings = cookedStrings;
        this.identity = this;
    }

    protected GetTemplateObjectNode(JSContext context, ArrayLiteralNode rawStrings, ArrayLiteralNode cookedStrings, Object identity) {
        this.context = context;
        this.rawStrings = rawStrings;
        this.cookedStrings = cookedStrings;
        this.identity = identity;
    }

    public static GetTemplateObjectNode create(JSContext context, ArrayLiteralNode rawStrings, ArrayLiteralNode cookedStrings) {
        return GetTemplateObjectNodeGen.create(context, rawStrings, cookedStrings);
    }

    @Specialization(guards = "!context.isMultiContext()", assumptions = "context.getSingleRealmAssumption()")
    protected DynamicObject doCached(@SuppressWarnings("unused") VirtualFrame frame,
                    @Cached("doUncached(frame)") DynamicObject cachedTemplate) {
        return cachedTemplate;
    }

    @Specialization(replaces = "doCached")
    protected DynamicObject doUncached(VirtualFrame frame) {
        DynamicObject cached = Boundaries.mapGet(getRealm().getTemplateRegistry(), identity);
        if (cached != null) {
            return cached;
        }
        cached = buildTemplateObject(frame);
        Boundaries.mapPut(getRealm().getTemplateRegistry(), identity, cached);
        return cached;
    }

    private DynamicObject buildTemplateObject(VirtualFrame frame) {
        DynamicObject template = cookedStrings.execute(frame);
        DynamicObject rawObj = rawStrings.execute(frame);
        JSObject.setIntegrityLevel(rawObj, true);
        JSObjectUtil.putDataProperty(context, template, "raw", rawObj, JSAttributes.notConfigurableNotEnumerableNotWritable());
        JSObject.setIntegrityLevel(template, true);

        return template;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return GetTemplateObjectNodeGen.create(context, cloneUninitialized(rawStrings, materializedTags), cloneUninitialized(cookedStrings, materializedTags), identity);
    }
}
