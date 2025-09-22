/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.array;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.nodes.unary.IsConstructorNode;
import com.oracle.truffle.js.nodes.unary.JSIsArrayNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * ArraySpeciesCreate ( originalArray, length ).
 * <p>
 * Creates a new Array or similar object using a constructor function derived from originalArray.
 */
@ImportStatic({JSObject.class, Symbol.class})
public abstract class ArraySpeciesCreateNode extends JavaScriptBaseNode {

    public abstract Object execute(Object originalArray, long length);

    @Specialization
    static Object doArraySpeciesCreate(Object originalArray, long length,
                    @Bind Node node,
                    @Cached(parameters = {"CONSTRUCTOR", "getJSContext()"}) PropertyGetNode getConstructorNode,
                    @Cached(parameters = {"SYMBOL_SPECIES", "getJSContext()"}) PropertyGetNode getSpeciesNode,
                    @Cached("createIsArray()") JSIsArrayNode isArrayNode,
                    @Cached(parameters = {"getJSContext()"}) ArrayCreateNode arrayCreateNode,
                    @Cached("createNew()") JSFunctionCallNode constructorCall,
                    @Cached IsConstructorNode isConstructorNode,
                    @Cached InlinedBranchProfile arraySpeciesIsArray,
                    @Cached InlinedBranchProfile arraySpeciesGetSymbol,
                    @Cached InlinedConditionProfile arraySpeciesEmpty,
                    @Cached InlinedBranchProfile differentRealm,
                    @Cached InlinedBranchProfile errorBranch) {
        Object ctor = Undefined.instance;
        if (isArrayNode.execute(originalArray)) {
            arraySpeciesIsArray.enter(node);
            ctor = getConstructorNode.getValue(originalArray);
            if (ctor instanceof JSObject ctorObj) {
                if (ctorObj instanceof JSFunctionObject ctorFunction && JSFunction.isConstructor(ctorFunction)) {
                    JSRealm thisRealm = JSRealm.get(node);
                    JSRealm ctorRealm = JSFunction.getRealm(ctorFunction);
                    if (thisRealm != ctorRealm) {
                        differentRealm.enter(node);
                        if (ctorRealm.getArrayConstructor() == ctor) {
                            /*
                             * If originalArray was created using the standard built-in Array
                             * constructor for a realm that is not the realm of the running
                             * execution context, then a new Array is created using the realm of the
                             * running execution context.
                             */
                            return arrayCreateNode.execute(length);
                        }
                    }
                }
                arraySpeciesGetSymbol.enter(node);
                ctor = getSpeciesNode.getValue(ctor);
                ctor = ctor == Null.instance ? Undefined.instance : ctor;
            }
        }
        if (arraySpeciesEmpty.profile(node, ctor == Undefined.instance)) {
            return arrayCreateNode.execute(length);
        }
        if (!isConstructorNode.executeBoolean(ctor)) {
            errorBranch.enter(node);
            throw Errors.createTypeErrorNotAConstructor(ctor, JSContext.get(node));
        }
        return constructorCall.executeCall(JSArguments.create(JSFunction.CONSTRUCT, ctor, JSRuntime.longToIntOrDouble(length)));
    }

    @NeverDefault
    public static ArraySpeciesCreateNode create() {
        return ArraySpeciesCreateNodeGen.create();
    }
}
