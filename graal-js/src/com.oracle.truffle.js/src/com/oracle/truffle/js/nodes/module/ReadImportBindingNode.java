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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSReadFrameSlotNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSModuleNamespaceObject;
import com.oracle.truffle.js.runtime.objects.AbstractModuleRecord;
import com.oracle.truffle.js.runtime.objects.CyclicModuleRecord;
import com.oracle.truffle.js.runtime.objects.ExportResolution;

/**
 * Reads the value of a resolved import binding from a resolved binding record (module, binding
 * name) returned by ResolveExport. Specializes on the imported module's FrameDescriptor.
 */
@ImportStatic(Strings.class)
public abstract class ReadImportBindingNode extends JavaScriptNode {

    @Child @Executed JavaScriptNode resolutionNode;

    ReadImportBindingNode(JavaScriptNode resolutionNode) {
        this.resolutionNode = resolutionNode;
    }

    public static JavaScriptNode create(JavaScriptNode resolutionNode) {
        return ReadImportBindingNodeGen.create(resolutionNode);
    }

    public static ReadImportBindingNode create() {
        return ReadImportBindingNodeGen.create(null);
    }

    public abstract Object execute(ExportResolution resolution);

    @Specialization(guards = {"!resolution.isNamespace()",
                    "frameDescriptor == resolution.getModule().getFrameDescriptor()",
                    "equals(equalNode, bindingName, resolution.getBindingName())"}, limit = "1")
    static Object doCached(ExportResolution.Resolved resolution,
                    @Cached("resolution.getModule().getFrameDescriptor()") @SuppressWarnings("unused") FrameDescriptor frameDescriptor,
                    @Cached("resolution.getBindingName()") @SuppressWarnings("unused") TruffleString bindingName,
                    @Cached("create(frameDescriptor, findImportedSlotIndex(bindingName, resolution.getModule()))") JSReadFrameSlotNode readFrameSlot,
                    @Cached @SuppressWarnings("unused") TruffleString.EqualNode equalNode) {
        AbstractModuleRecord module = resolution.getModule();
        assert !(module instanceof CyclicModuleRecord cyclicModule) || cyclicModule.isLinked() : module;
        MaterializedFrame environment = JSFrameUtil.castMaterializedFrame(module.getEnvironment());
        Object value = readFrameSlot.execute(environment);
        assert value != null;
        return value;
    }

    @TruffleBoundary
    @Specialization(guards = {"!resolution.isNamespace()"}, replaces = {"doCached"})
    final Object doUncached(ExportResolution.Resolved resolution) {
        AbstractModuleRecord module = resolution.getModule();
        assert !(module instanceof CyclicModuleRecord cyclicModule) || cyclicModule.isLinked() : module;
        TruffleString bindingName = resolution.getBindingName();
        FrameDescriptor moduleFrameDescriptor = module.getFrameDescriptor();
        int slotIndex = findImportedSlotIndex(bindingName, module);
        boolean hasTemporalDeadZone = JSFrameUtil.hasTemporalDeadZone(moduleFrameDescriptor, slotIndex);
        MaterializedFrame environment = JSFrameUtil.castMaterializedFrame(module.getEnvironment());
        if (hasTemporalDeadZone && environment.getTag(slotIndex) == FrameSlotKind.Illegal.tag) {
            // Uninitialized binding
            throw Errors.createReferenceErrorNotDefined(bindingName, this);
        }
        Object value = environment.getValue(slotIndex);
        assert value != null;
        return value;
    }

    static int findImportedSlotIndex(TruffleString bindingName, AbstractModuleRecord module) {
        return JSFrameUtil.findRequiredFrameSlotIndex(module.getFrameDescriptor(), bindingName);
    }

    @Specialization(guards = {"resolution.isNamespace()"})
    final Object doGetNamespace(ExportResolution.Resolved resolution,
                    @Cached InlinedBranchProfile slowPath) {
        AbstractModuleRecord module = resolution.getModule();
        assert !(module instanceof CyclicModuleRecord cyclicModule) || cyclicModule.isLinked() : module;
        var namespace = module.getModuleNamespaceOrNull();
        if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, namespace != null)) {
            return namespace;
        } else {
            slowPath.enter(this);
            return module.getModuleNamespace();
        }
    }

    @Specialization
    static Object doNamespace(JSModuleNamespaceObject namespace) {
        return namespace;
    }

    @Fallback
    static Object doUnresolved(@SuppressWarnings("unused") Object uninitialized) {
        // Note: Should not reach here normally; this is just a safeguard in case the scope is
        // intercepted before all imports are resolved and the environment is fully initialized.
        throw Errors.createReferenceError("Unresolved import");
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(cloneUninitialized(resolutionNode, materializedTags));
    }

}
