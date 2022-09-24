/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.decorators;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.js.nodes.function.ClassElementDefinitionRecord;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;

@ImportStatic(ClassElementDefinitionRecord.class)
public abstract class DefineMethodPropertyNode extends Node {

    public static DefineMethodPropertyNode create() {
        return DefineMethodPropertyNodeGen.create();
    }

    public abstract void executeDefine(JSDynamicObject homeObject, ClassElementDefinitionRecord record, boolean enumerable);

    @SuppressWarnings("unused")
    @Specialization(guards = "record.isPrivate()")
    public void doPrivate(JSDynamicObject homeObject, ClassElementDefinitionRecord record, boolean enumerable) {
        // NOP
    }

    @Specialization(guards = {"record.isMethod()", "!record.isPrivate()"})
    public void doMethod(JSDynamicObject homeObject, ClassElementDefinitionRecord record, boolean enumerable) {
        PropertyDescriptor desc = PropertyDescriptor.createData(record.getValue(), enumerable, true, true);
        JSRuntime.definePropertyOrThrow(homeObject, record.getKey(), desc);
    }

    @Specialization(guards = {"record.isGetter()", "!record.isPrivate()"})
    public void doGetter(JSDynamicObject homeObject, ClassElementDefinitionRecord record, boolean enumerable) {
        JSDynamicObject otherAccessor = Undefined.instance;
        PropertyDescriptor ownProperty = JSObject.getOwnProperty(homeObject, record.getKey());
        if (ownProperty != null) {
            otherAccessor = (JSDynamicObject) ownProperty.getSet();
        }
        PropertyDescriptor desc = PropertyDescriptor.createAccessor(record.getValue(), otherAccessor, enumerable, true);
        JSRuntime.definePropertyOrThrow(homeObject, record.getKey(), desc);
    }

    @Specialization(guards = {"record.isSetter()", "!record.isPrivate()"})
    public void doSetter(JSDynamicObject homeObject, ClassElementDefinitionRecord record, boolean enumerable) {
        Object otherAccessor = Undefined.instance;
        PropertyDescriptor ownProperty = JSObject.getOwnProperty(homeObject, record.getKey());
        if (ownProperty != null) {
            otherAccessor = ownProperty.getGet();
        }
        PropertyDescriptor desc = PropertyDescriptor.createAccessor(otherAccessor, record.getValue(), enumerable, true);
        JSRuntime.definePropertyOrThrow(homeObject, record.getKey(), desc);
    }

    @Specialization(guards = {"record.isAutoAccessor()", "!record.isPrivate()"})
    public void doAutoAccessor(JSDynamicObject homeObject, ClassElementDefinitionRecord.AutoAccessor record, boolean enumerable) {
        PropertyDescriptor desc = PropertyDescriptor.createAccessor(record.getGetter(), record.getSetter(), enumerable, true);
        JSRuntime.definePropertyOrThrow(homeObject, record.getKey(), desc);
    }
}
