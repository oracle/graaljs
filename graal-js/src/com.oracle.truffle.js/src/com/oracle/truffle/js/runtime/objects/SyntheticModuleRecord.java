/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.runtime.objects;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.promise.NewPromiseCapabilityNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSFrameUtil;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSPromiseObject;
import com.oracle.truffle.js.runtime.util.Pair;

/**
 * A Synthetic Module Record, used for JSON modules, etc.
 *
 * Its exported names are statically defined at creation, while their corresponding values can
 * change over time using SetSyntheticModuleExport. It has no imports or dependencies.
 */
public final class SyntheticModuleRecord extends AbstractModuleRecord {

    private final List<TruffleString> exportedNames;
    private Consumer<SyntheticModuleRecord> evaluationSteps;

    public SyntheticModuleRecord(JSContext context, Source source, Object hostDefined,
                    List<TruffleString> exportedNames, Consumer<SyntheticModuleRecord> evaluationSteps) {
        super(context, source, hostDefined, createFrameDescriptor(exportedNames));
        this.exportedNames = Objects.requireNonNull(exportedNames);
        this.evaluationSteps = Objects.requireNonNull(evaluationSteps);
    }

    @TruffleBoundary
    @Override
    public JSPromiseObject loadRequestedModules(JSRealm realm, Object hostDefinedArg) {
        PromiseCapabilityRecord pc = NewPromiseCapabilityNode.createDefault(realm);
        JSFunction.call(JSArguments.createOneArg(Undefined.instance, pc.getResolve(), Undefined.instance));
        return (JSPromiseObject) pc.getPromise();
    }

    @TruffleBoundary
    @Override
    public void link(JSRealm realm) {
        if (getEnvironment() != null) {
            // already linked
            return;
        }
        initializeEnvironment();
    }

    @TruffleBoundary
    @Override
    public JSPromiseObject evaluate(JSRealm realm) {
        PromiseCapabilityRecord pc = NewPromiseCapabilityNode.createDefault(realm);
        try {
            evaluateSync(realm);
            JSFunction.call(JSArguments.createOneArg(Undefined.instance, pc.getResolve(), Undefined.instance));
        } catch (AbstractTruffleException e) {
            Object errorObj = e instanceof GraalJSException ? ((GraalJSException) e).getErrorObject() : e;
            JSFunction.call(JSArguments.createOneArg(Undefined.instance, pc.getReject(), errorObj));
        }
        return (JSPromiseObject) pc.getPromise();
    }

    @TruffleBoundary
    @Override
    public void evaluateSync(JSRealm realm) {
        if (evaluationSteps == null) {
            // module has already been evaluated, with normal completion.
            return;
        }
        evaluationSteps.accept(this);
        evaluationSteps = null;
    }

    @Override
    public Collection<TruffleString> getExportedNames(Set<JSModuleRecord> exportStarSet) {
        return exportedNames;
    }

    @TruffleBoundary
    @Override
    public ExportResolution resolveExport(TruffleString exportName, Set<Pair<? extends AbstractModuleRecord, TruffleString>> resolveSet) {
        for (TruffleString name : getExportedNames()) {
            if (name.equals(exportName)) {
                return ExportResolution.resolved(this, name);
            }
        }
        return ExportResolution.notFound();
    }

    private static FrameDescriptor createFrameDescriptor(Collection<TruffleString> exportNames) {
        FrameDescriptor.Builder b = FrameDescriptor.newBuilder(exportNames.size());
        b.defaultValue(Undefined.instance);
        for (TruffleString name : exportNames) {
            b.addSlot(FrameSlotKind.Illegal, name, null);
        }
        return b.build();
    }

    private void initializeEnvironment() {
        MaterializedFrame env = Truffle.getRuntime().createMaterializedFrame(JSArguments.EMPTY_ARGUMENTS_ARRAY, getFrameDescriptor());
        setEnvironment(env);
    }

    @TruffleBoundary
    public void setSyntheticModuleExport(TruffleString exportName, Object exportValue) {
        MaterializedFrame frame = getEnvironment();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        OptionalInt frameSlot = JSFrameUtil.findOptionalFrameSlotIndex(frameDescriptor, exportName);
        if (!frameSlot.isPresent()) {
            throw Errors.createReferenceError("Export '" + exportName + "' is not defined in module");
        }
        frame.setObject(frameSlot.getAsInt(), exportValue);
    }

    @Override
    public Object getModuleSource() {
        throw Errors.createSyntaxError("Source phase import is not available for Synthetic Module");
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return "SyntheticModule" + "@" + Integer.toHexString(System.identityHashCode(this)) + "[source=" + getSource() + "]";
    }
}
