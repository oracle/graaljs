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

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.objects.Dead;

@NodeInfo(cost = NodeCost.NONE)
public class GlobalScopeNode extends JavaScriptNode {
    protected final JSContext context;

    protected GlobalScopeNode(JSContext context) {
        this.context = context;
    }

    public static JavaScriptNode create(JSContext context) {
        return new GlobalScopeNode(context);
    }

    public static JavaScriptNode createWithTDZCheck(JSContext context, String varName) {
        return GlobalScopeTDZCheckNodeGen.create(context, varName);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return context.getRealm().getGlobalScope();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return copy();
    }
}

@ImportStatic(JSTruffleOptions.class)
abstract class GlobalScopeTDZCheckNode extends GlobalScopeNode {
    final String varName;
    @Executed @Child JavaScriptNode scopeNode;

    GlobalScopeTDZCheckNode(JSContext context, String varName) {
        super(context);
        this.varName = varName;
        this.scopeNode = GlobalScopeNode.create(context);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"scope.getShape() == cachedShape"}, assumptions = {"cachedShape.getValidAssumption()"}, limit = "PropertyCacheLimit")
    final Object doCached(DynamicObject scope,
                    @Cached("scope.getShape()") Shape cachedShape,
                    @Cached("isDead(cachedShape)") boolean dead) {
        assert dead == (scope.get(varName) == Dead.instance());
        if (dead) {
            throw Errors.createReferenceErrorNotDefined(varName, this);
        }
        return scope;
    }

    @Specialization(replaces = "doCached")
    final Object doUncached(Object scope,
                    @Cached("create(varName, context)") PropertyGetNode getNode,
                    @Cached("create()") BranchProfile deadBranch) {
        if (getNode.getValue(scope) == Dead.instance()) {
            deadBranch.enter();
            throw Errors.createReferenceErrorNotDefined(varName, this);
        }
        return scope;
    }

    final boolean isDead(Shape shape) {
        Property property = shape.getProperty(varName);
        return property != null && property.getLocation().isDeclared();
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return GlobalScopeTDZCheckNodeGen.create(context, varName);
    }
}
