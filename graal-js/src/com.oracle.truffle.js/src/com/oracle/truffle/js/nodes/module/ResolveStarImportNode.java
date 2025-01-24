/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.module;

import java.util.Set;

import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.ValueProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.control.StatementNode;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;

/**
 * Resolves a star import: gets the imported module's namespace object and initializes the local
 * binding (frame slot) with it.
 */
public class ResolveStarImportNode extends StatementNode {

    private final JSContext context;
    private final Module.ModuleRequest moduleRequest;
    @Child private JavaScriptNode moduleNode;
    @Child private JSWriteFrameSlotNode writeLocalNode;
    private final ValueProfile moduleProfile = ValueProfile.createClassProfile();

    ResolveStarImportNode(JSContext context, JavaScriptNode moduleNode, Module.ModuleRequest moduleRequest, JSWriteFrameSlotNode writeLocalNode) {
        this.context = context;
        this.moduleRequest = moduleRequest;
        this.moduleNode = moduleNode;
        this.writeLocalNode = writeLocalNode;
    }

    public static StatementNode create(JSContext context, JavaScriptNode moduleNode, Module.ModuleRequest moduleRequest, JSWriteFrameSlotNode writeLocalNode) {
        return new ResolveStarImportNode(context, moduleNode, moduleRequest, writeLocalNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        JSModuleRecord referrer = (JSModuleRecord) moduleNode.execute(frame);
        // Let importedModule be GetImportedModule(module, in.[[ModuleRequest]]).
        AbstractModuleRecord importedModule = moduleProfile.profile(referrer.getImportedModule(moduleRequest));
        // If in.[[ImportName]] is namespace-object, then
        // Let namespace be GetModuleNamespace(importedModule)
        JSDynamicObject namespace = importedModule.getModuleNamespace();
        // envRec.CreateImmutableBinding(in.[[LocalName]], true).
        // Call envRec.InitializeBinding(in.[[LocalName]], namespace).
        writeLocalNode.executeWrite(frame, namespace);
        return EMPTY;
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, cloneUninitialized(moduleNode, materializedTags), moduleRequest, cloneUninitialized(writeLocalNode, materializedTags));
    }

}
