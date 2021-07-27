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
package com.oracle.truffle.js.builtins.helper;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.js.builtins.RegExpPrototypeBuiltins;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.access.GetPrototypeNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.builtins.JSClass;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.builtins.JSRegExp;

/**
 * Helper node for optimization of built-in functions. Some built-in functions, like
 * RegExp.prototype.[@@replace], are suboptimal when implemented in full conformance to the
 * ECMAScript spec. We want to replace those functions with faster, non-spec-conforming variants
 * whenever possible. One of the prerequisites for doing so is that running the spec-conforming
 * variant must be side-effect-free, which can only be guaranteed when all properties and functions
 * used by the built-in function are known/built-in as well. Therefore, we define a "pristine"
 * property for built-in objects: <br/>
 * An object is considered "pristine" in respect to a given class and list of properties if and only
 * if:
 * <ul>
 * <li>The object is an instance of the given class</li>
 * <li>The object's prototype's shape has never been changed</li>
 * <li>None of the given properties have been overwritten in the prototype or the object</li>
 * </ul>
 */
public abstract class IsPristineObjectNode extends JavaScriptBaseNode {

    private final JSClass jsClass;
    private final Shape initialPrototypeShape;
    @CompilationFinal(dimensions = 1) private final Object[] propertyKeys;
    @CompilationFinal(dimensions = 1) private final Assumption[] propertyFinalAssumptions;
    @Child private GetPrototypeNode getPrototypeNode = GetPrototypeNode.create();

    IsPristineObjectNode(JSClass jsClass, Shape initialPrototypeShape, Object... propertyKeys) {
        assert jsClass != JSProxy.INSTANCE : "not supported because getting the prototype of proxy objects can have side effects";
        this.jsClass = jsClass;
        this.initialPrototypeShape = initialPrototypeShape;
        this.propertyKeys = propertyKeys;
        propertyFinalAssumptions = new Assumption[propertyKeys.length];
        for (int i = 0; i < propertyKeys.length; i++) {
            propertyFinalAssumptions[i] = initialPrototypeShape.getProperty(propertyKeys[i]).getLocation().getFinalAssumption();
        }
    }

    public static IsPristineObjectNode create(JSClass jsClass, Shape initialPrototypeShape, Object... propertyKeys) {
        return IsPristineObjectNodeGen.create(jsClass, initialPrototypeShape, propertyKeys);
    }

    public static IsPristineObjectNode createRegExpExecAndMatch(JSContext context) {
        assert context.getEcmaScriptVersion() >= 6;
        return IsPristineObjectNode.create(JSRegExp.INSTANCE, JSRealm.get(null).getInitialRegExpPrototypeShape(),
                        Symbol.SYMBOL_MATCH,
                        RegExpPrototypeBuiltins.RegExpPrototype.exec.getKey(),
                        JSRegExp.FLAGS,
                        JSRegExp.GLOBAL,
                        JSRegExp.UNICODE,
                        JSRegExp.STICKY);
    }

    public abstract boolean execute(DynamicObject object);

    @Specialization(guards = {"cachedShape.check(object)"}, assumptions = "getPropertyFinalAssumptions()")
    boolean doCached(@SuppressWarnings("unused") DynamicObject object,
                    @Cached("object.getShape()") @SuppressWarnings("unused") Shape cachedShape,
                    @Cached("isInstanceAndDoesNotOverwriteProps(cachedShape)") boolean isInstanceAndDoesNotOverwriteProps) {
        return isInstanceAndDoesNotOverwriteProps && prototypeShapeUnchanged(object);
    }

    @Specialization(assumptions = "getPropertyFinalAssumptions()", replaces = "doCached")
    boolean doDynamic(DynamicObject object) {
        return isInstanceAndDoesNotOverwriteProps(object.getShape()) && prototypeShapeUnchanged(object);
    }

    @Specialization
    boolean doAssumptionsInvalid(@SuppressWarnings("unused") DynamicObject object) {
        return false;
    }

    Assumption[] getPropertyFinalAssumptions() {
        return propertyFinalAssumptions;
    }

    private boolean prototypeShapeUnchanged(DynamicObject object) {
        return getPrototypeNode.executeJSObject(object).getShape() == initialPrototypeShape;
    }

    boolean isInstanceAndDoesNotOverwriteProps(Shape objectShape) {
        if (objectShape.getDynamicType() != jsClass) {
            return false;
        }
        for (Object key : propertyKeys) {
            if (objectShape.hasProperty(key)) {
                return false;
            }
        }
        return true;
    }
}
