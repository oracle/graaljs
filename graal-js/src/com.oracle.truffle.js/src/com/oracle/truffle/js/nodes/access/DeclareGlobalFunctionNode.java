/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSGlobalObject;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

public class DeclareGlobalFunctionNode extends DeclareGlobalNode {
    private final boolean configurable;
    @Child private JavaScriptNode valueNode;
    @Child private JSGetOwnPropertyNode getOwnPropertyNode;
    private final JSClassProfile classProfile = JSClassProfile.create();
    private final ConditionProfile isGlobalObject = ConditionProfile.createBinaryProfile();

    public DeclareGlobalFunctionNode(String varName, boolean configurable, JavaScriptNode valueNode) {
        super(varName);
        this.configurable = configurable;
        this.valueNode = valueNode;
        this.getOwnPropertyNode = JSGetOwnPropertyNode.create(false);
    }

    @Override
    public void verify(JSContext context, JSRealm realm) {
        super.verify(context, realm);
        // CanDeclareGlobalFunction
        DynamicObject globalObject = realm.getGlobalObject();
        PropertyDescriptor desc = getOwnPropertyNode.execute(globalObject, varName);
        if (desc == null) {
            if (JSGlobalObject.isJSGlobalObject(globalObject) && !JSObject.isExtensible(globalObject, classProfile)) {
                errorProfile.enter();
                throw Errors.createTypeErrorGlobalObjectNotExtensible(this);
            }
        } else {
            if (!desc.getConfigurable() && !(desc.isDataDescriptor() && desc.getWritable() && desc.getEnumerable())) {
                errorProfile.enter();
                throw Errors.createTypeErrorCannotDeclareGlobalFunction(varName, this);
            }
        }
    }

    @Override
    public void executeVoid(VirtualFrame frame, JSContext context, JSRealm realm) {
        Object value = valueNode == null ? Undefined.instance : valueNode.execute(frame);
        DynamicObject globalObject = realm.getGlobalObject();
        PropertyDescriptor desc = getOwnPropertyNode.execute(globalObject, varName);
        if (valueNode == null && desc == null && isGlobalObject.profile(JSGlobalObject.isJSGlobalObject(globalObject))) {
            JSObjectUtil.putDeclaredDataProperty(context, globalObject, varName, value,
                            configurable ? JSAttributes.configurableEnumerableWritable() : JSAttributes.notConfigurableEnumerableWritable());
        } else {
            if (desc == null || desc.getConfigurable()) {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value, true, true, configurable), true);
            } else {
                JSObject.defineOwnProperty(globalObject, varName, PropertyDescriptor.createData(value), true);
            }
            if (valueNode != null) {
                JSObject.set(globalObject, varName, value, false, classProfile);
            }
        }
    }

    @Override
    public boolean isGlobalFunctionDeclaration() {
        return true;
    }

    @Override
    protected DeclareGlobalNode copyUninitialized() {
        return new DeclareGlobalFunctionNode(varName, configurable, valueNode);
    }
}
