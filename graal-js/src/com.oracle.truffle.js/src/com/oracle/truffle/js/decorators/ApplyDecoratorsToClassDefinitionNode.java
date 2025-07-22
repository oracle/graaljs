/*
 * Copyright (c) 2022, 2025, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.decorators.CreateDecoratorContextObjectNode.DecorationState;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsCallableNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.SimpleArrayList;

public class ApplyDecoratorsToClassDefinitionNode extends JavaScriptBaseNode {

    @Child private JSFunctionCallNode callNode;
    @Child private IsCallableNode isCallableNode;
    @Child private CreateDecoratorContextObjectNode createDecoratorContextObject;

    private final BranchProfile errorProfile = BranchProfile.create();

    public static ApplyDecoratorsToClassDefinitionNode create(JSContext context) {
        return new ApplyDecoratorsToClassDefinitionNode(context);
    }

    ApplyDecoratorsToClassDefinitionNode(JSContext context) {
        this.createDecoratorContextObject = CreateDecoratorContextObjectNode.createForClass(context);
        this.callNode = JSFunctionCallNode.createCall();
        this.isCallableNode = IsCallableNode.create();
    }

    @ExplodeLoop
    public Object executeDecorators(Object className, JSObject constructor, Object[] decorators, SimpleArrayList<Object> extraInitializers) {
        Object classDef = constructor;
        JSRealm realm = getRealm();
        for (Object decorator : decorators) {
            DecorationState state = new DecorationState();
            JSObject contextObj = createDecoratorContextObject.evaluateClass(realm, className, extraInitializers, state);
            Object newDef = callNode.executeCall(JSArguments.create(Undefined.instance, decorator, classDef, contextObj));
            state.finished = true;
            if (isCallableNode.executeBoolean(newDef)) {
                classDef = newDef;
            } else if (newDef != Undefined.instance) {
                errorProfile.enter();
                throw Errors.createTypeErrorWrongDecoratorReturn(this);
            }
        }
        return classDef;
    }
}
