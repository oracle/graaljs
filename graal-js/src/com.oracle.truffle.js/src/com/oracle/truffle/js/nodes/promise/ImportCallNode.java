/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.promise;

import java.util.Set;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSHasPropertyNode;
import com.oracle.truffle.js.nodes.access.PropertyGetNode;
import com.oracle.truffle.js.nodes.arguments.AccessIndexedArgumentNode;
import com.oracle.truffle.js.nodes.cast.JSToStringNode;
import com.oracle.truffle.js.nodes.control.TryCatchNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSConfig;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.builtins.JSPromise;
import com.oracle.truffle.js.runtime.objects.JSModuleRecord;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSObjectUtil;
import com.oracle.truffle.js.runtime.objects.PromiseCapabilityRecord;
import com.oracle.truffle.js.runtime.objects.PromiseReactionRecord;
import com.oracle.truffle.js.runtime.objects.ScriptOrModule;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.Pair;
import com.oracle.truffle.js.runtime.util.Triple;

import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.js.nodes.module.ModuleBlockNode;

/**
 * Represents the import call expression syntax: {@code import(specifier)}.
 */
public class ImportCallNode extends JavaScriptNode {
    @Child private JavaScriptNode argRefNode;
    @Child private JavaScriptNode activeScriptOrModuleNode;
    @Child private NewPromiseCapabilityNode newPromiseCapabilityNode;
    @Child private JSToStringNode toStringNode;
    @Child private PromiseReactionJobNode promiseReactionJobNode;

    // lazily initialized
    @Child private JSFunctionCallNode callRejectNode;
    @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
    @Child private InteropLibrary exceptions;

    // read the hidden key with @ child
    @Child private PropertyGetNode body;

    private final JSContext context;

    protected ImportCallNode(JSContext context, JavaScriptNode argRefNode, JavaScriptNode activeScriptOrModuleNode) {
        this.context = context;
        this.argRefNode = argRefNode;
        this.activeScriptOrModuleNode = activeScriptOrModuleNode;
        this.newPromiseCapabilityNode = NewPromiseCapabilityNode.create(context);
        this.toStringNode = JSToStringNode.create();
        this.promiseReactionJobNode = PromiseReactionJobNode.create(context);
    }

    public static ImportCallNode create(JSContext context, JavaScriptNode argRefNode, JavaScriptNode activeScriptOrModuleNode) {
        return new ImportCallNode(context, argRefNode, activeScriptOrModuleNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object referencingScriptOrModule = getActiveScriptOrModule(frame);
        Object specifier = argRefNode.execute(frame);
        String specifierString;

        if (specifier instanceof DynamicObject && JSObjectUtil.hasHiddenProperty((DynamicObject) specifier,
                        ModuleBlockNode.getModuleBodyKey())) {
            // read the hidden key from 'specifier'
            this.body = PropertyGetNode.createGetHidden(ModuleBlockNode.getModuleBodyKey(), this.context);
            PropertyGetNode getSourceCode = PropertyGetNode.createGetHidden(ModuleBlockNode.getModuleSourceKey(), this.context);
            PropertyGetNode getModuleBlockName = PropertyGetNode.createGetHidden(ModuleBlockNode.getHostDefinedSlotKey(),
                            this.context);

            Object bodyNode = this.body.getValue(specifier);
            Object sourceText = getSourceCode.getValue(specifier);
            Object moduleBlockName = getModuleBlockName.getValue(specifier);

            Object executedBody = ((JavaScriptNode) bodyNode).execute(frame);

            Source source = Source.newBuilder(JavaScriptLanguage.ID, sourceText.toString(), (String) moduleBlockName).build();

            return hostImportModuleDynamically(referencingScriptOrModule, specifier, source);
        }

        try {
            specifierString = toStringNode.executeString(specifier);
        } catch (Throwable ex) {
            if (TryCatchNode.shouldCatch(ex, exceptions())) {
                return newRejectedPromiseFromException(ex);
            } else {
                throw ex;
            }
        }
        return hostImportModuleDynamically(referencingScriptOrModule, specifierString);
    }

    private Object getActiveScriptOrModule(VirtualFrame frame) {
        if (activeScriptOrModuleNode != null) {
            return activeScriptOrModuleNode.execute(frame);
        } else {
            return new ScriptOrModule(context, getEncapsulatingSourceSection().getSource());
        }
    }

    private DynamicObject hostImportModuleDynamically(Object referencingScriptOrModule, String specifier) {
        JSRealm realm = context.getRealm();
        if (context.hasImportModuleDynamicallyCallbackBeenSet()) {
            DynamicObject promise = context.hostImportModuleDynamically(realm, (ScriptOrModule) referencingScriptOrModule, specifier);
            if (promise == null) {
                return newRejectedPromiseFromException(createTypeErrorCannotImport(specifier));
            }
            assert JSPromise.isJSPromise(promise);
            return promise;
        } else {
            PromiseCapabilityRecord promiseCapability = newPromiseCapability();
            context.promiseEnqueueJob(realm, createImportModuleDynamicallyJob((ScriptOrModule) referencingScriptOrModule, specifier, promiseCapability));
            return promiseCapability.getPromise();
        }
    }

    private DynamicObject hostImportModuleDynamically(Object referencingScriptOrModule, Object specifier, Source source) {
        JSRealm realm = context.getRealm();

        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        context.promiseEnqueueJob(realm, createImportModuleDynamicallyJob((ScriptOrModule) referencingScriptOrModule, (DynamicObject) specifier, source, promiseCapability));
        return promiseCapability.getPromise();
    }

    private PromiseCapabilityRecord newPromiseCapability() {
        if (newPromiseCapabilityNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            newPromiseCapabilityNode = insert(NewPromiseCapabilityNode.create(context));
        }
        return newPromiseCapabilityNode.executeDefault();
    }

    private DynamicObject newRejectedPromiseFromException(Throwable ex) {
        if (callRejectNode == null || getErrorObjectNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callRejectNode = insert(JSFunctionCallNode.createCall());
            getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
        }
        PromiseCapabilityRecord promiseCapability = newPromiseCapability();
        callRejectNode.executeCall(JSArguments.createOneArg(Undefined.instance, promiseCapability.getReject(), getErrorObjectNode.execute(ex)));
        return promiseCapability.getPromise();
    }

    private InteropLibrary exceptions() {
        InteropLibrary e = exceptions;
        if (e == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            exceptions = e = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
        }
        return e;
    }

    @TruffleBoundary
    private static JSException createTypeErrorCannotImport(String specifier) {
        return Errors.createError("Cannot dynamically import module: " + specifier);
    }

    /**
     * Returns a promise job that performs both HostImportModuleDynamically and FinishDynamicImport.
     */
    public DynamicObject createImportModuleDynamicallyJob(ScriptOrModule referencingScriptOrModule, String specifier, PromiseCapabilityRecord promiseCapability) {
        if (context.isOptionTopLevelAwait()) {
            Triple<ScriptOrModule, String, PromiseCapabilityRecord> request = new Triple<>(referencingScriptOrModule, specifier, promiseCapability);
            PromiseCapabilityRecord startModuleLoadCapability = newPromiseCapability();
            PromiseReactionRecord startModuleLoad = PromiseReactionRecord.create(startModuleLoadCapability, createImportModuleDynamicallyHandler(RootType.TopLevelAwait), true);
            return promiseReactionJobNode.execute(startModuleLoad, request);
        } else {
            Pair<ScriptOrModule, String> request = new Pair<>(referencingScriptOrModule, specifier);
            return promiseReactionJobNode.execute(PromiseReactionRecord.create(promiseCapability, createImportModuleDynamicallyHandler(RootType.ImportModuleDynamically), true), request);
        }
    }

    public DynamicObject createImportModuleDynamicallyJob(ScriptOrModule referencingScriptOrModule, DynamicObject specifier, Source source, PromiseCapabilityRecord promiseCapability) {
        Triple<ScriptOrModule, DynamicObject, Source> request = new Triple<>(referencingScriptOrModule, specifier, source);

        return promiseReactionJobNode.execute(PromiseReactionRecord.create(promiseCapability, createImportModuleDynamicallyHandler(RootType.ModuleBlock), true), request);
    }

    /**
     * Returns a handler function to be used together with a PromiseReactionJob in order to perform
     * the steps of both HostImportModuleDynamically and FinishDynamicImport.
     */
    private DynamicObject createImportModuleDynamicallyHandler(RootType root) {
        JSFunctionData functionData = context.getOrCreateBuiltinFunctionData(JSContext.BuiltinFunctionKey.ImportModuleDynamically, (c) -> createImportModuleDynamicallyHandlerImpl(c, root));
        return JSFunction.create(context.getRealm(), functionData);
    }

    private static enum RootType {
        ImportModuleDynamically,
        TopLevelAwait,
        ModuleBlock;
    }

    private static JSFunctionData createImportModuleDynamicallyHandlerImpl(JSContext context, RootType type) {

        class ImportModuleDynamicallyRootNode extends JavaScriptRootNode {
            @Child protected JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                Pair<ScriptOrModule, String> request = (Pair<ScriptOrModule, String>) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.getFirst();
                String specifier = request.getSecond();
                JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, specifier);
                return finishDynamicImport(context.getRealm(), moduleRecord, referencingScriptOrModule, specifier);
            }

            protected Object finishDynamicImport(JSRealm realm, JSModuleRecord moduleRecord, ScriptOrModule referencingScriptOrModule, String specifier) {
                context.getEvaluator().moduleInstantiation(realm, moduleRecord);
                context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                if (moduleRecord.getEvaluationError() != null) {
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                }
                // Note: PromiseReactionJob performs the promise rejection and resolution.
                assert moduleRecord == context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, specifier);
                // Evaluate has already been invoked on moduleRecord and successfully completed.
                assert moduleRecord.isEvaluated();
                return context.getEvaluator().getModuleNamespace(moduleRecord);
            }
        }

        class TopLevelAwaitImportModuleDynamicallyRootNode extends ImportModuleDynamicallyRootNode {
            @Child private PerformPromiseThenNode promiseThenNode = PerformPromiseThenNode.create(context);
            @Child private JSFunctionCallNode callPromiseReaction = JSFunctionCallNode.createCall();
            @Child private TryCatchNode.GetErrorObjectNode getErrorObjectNode;
            @Child private InteropLibrary exceptions;

            @SuppressWarnings("unchecked")
            @Override
            public Object execute(VirtualFrame frame) {
                Triple<ScriptOrModule, String, PromiseCapabilityRecord> request = (Triple<ScriptOrModule, String, PromiseCapabilityRecord>) argumentNode.execute(frame);
                ScriptOrModule referencingScriptOrModule = request.getFirst();
                String specifier = request.getSecond();
                PromiseCapabilityRecord moduleLoadedCapability = request.getThird();
                try {
                    JSModuleRecord moduleRecord = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, specifier);
                    JSRealm realm = context.getRealm();
                    if (moduleRecord.isTopLevelAsync()) {
                        context.getEvaluator().moduleInstantiation(realm, moduleRecord);
                        Object moduleLoadedStartPromise = context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                        assert JSPromise.isJSPromise(moduleLoadedStartPromise);
                        promiseThenNode.execute((DynamicObject) moduleLoadedStartPromise, moduleLoadedCapability.getResolve(), moduleLoadedCapability.getReject(), moduleLoadedCapability);
                    } else {
                        Object result = finishDynamicImport(realm, moduleRecord, referencingScriptOrModule, specifier);
                        if (moduleRecord.isAsyncEvaluating()) {
                            // Some module import started an async loading chain. The top-level
                            // capability will reject/resolve the dynamic import promise.
                            PromiseCapabilityRecord topLevelCapability = moduleRecord.getTopLevelCapability();
                            promiseThenNode.execute(topLevelCapability.getPromise(), moduleLoadedCapability.getResolve(), moduleLoadedCapability.getReject(), null);
                        } else {
                            callPromiseReaction.executeCall(JSArguments.create(Undefined.instance, moduleLoadedCapability.getResolve(), result));
                        }
                    }
                } catch (Throwable t) {
                    if (shouldCatch(t)) {
                        Object errorObject = getErrorObjectNode.execute(t);
                        callPromiseReaction.executeCall(JSArguments.create(Undefined.instance, moduleLoadedCapability.getReject(), errorObject));
                    } else {
                        throw t;
                    }
                }
                return Undefined.instance;
            }

            private boolean shouldCatch(Throwable exception) {
                if (getErrorObjectNode == null || exceptions == null) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    getErrorObjectNode = insert(TryCatchNode.GetErrorObjectNode.create(context));
                    exceptions = insert(InteropLibrary.getFactory().createDispatched(JSConfig.InteropLibraryLimit));
                }
                return TryCatchNode.shouldCatch(exception, exceptions);
            }
        }

        class ModuleBlockRootNode extends JavaScriptRootNode {

            @Child protected JavaScriptNode argumentNode = AccessIndexedArgumentNode.create(0);

            @Override
            public Object execute(VirtualFrame frame) {
                assert argumentNode.execute(frame) instanceof Triple<?, ?, ?>;
                Triple<?, ?, ?> request = (Triple<?, ?, ?>) argumentNode.execute(frame);

                assert request.getFirst() instanceof ScriptOrModule;
                ScriptOrModule referencingScriptOrModule = (ScriptOrModule) request.getFirst();

                assert request.getSecond() instanceof DynamicObject;
                DynamicObject moduleBlock = (DynamicObject) request.getSecond();

                assert request.getThird() instanceof Source;
                Source source = (Source) request.getThird();

                JSModuleRecord test = context.getEvaluator().hostResolveImportedModule(context, referencingScriptOrModule, moduleBlock, source);

                return finishDynamicImport(context.getRealm(), test, referencingScriptOrModule, moduleBlock);
            }

            protected Object finishDynamicImport(JSRealm realm, JSModuleRecord moduleRecord, ScriptOrModule referencingScriptOrModule, DynamicObject specifier) {
                context.getEvaluator().moduleInstantiation(realm, moduleRecord);
                context.getEvaluator().moduleEvaluation(realm, moduleRecord);
                if (moduleRecord.getEvaluationError() != null) {
                    throw JSRuntime.rethrow(moduleRecord.getEvaluationError());
                }
                // Note: PromiseReactionJob performs the promise rejection and resolution.
                // Probably unwanted in module blocks since we do not need to get the module block
                // via specifier
                assert moduleRecord == context.getEvaluator().hostResolveImportedModuleBlock(context, referencingScriptOrModule, moduleRecord, specifier);
                // Evaluate has already been invoked on moduleRecord and successfully completed.
                assert moduleRecord.isEvaluated();
                return context.getEvaluator().getModuleNamespace(moduleRecord);
            }

        }

        JavaScriptRootNode root;
        switch (type) {
            case TopLevelAwait:
                root = new TopLevelAwaitImportModuleDynamicallyRootNode();
                break;
            case ImportModuleDynamically:
                root = new ImportModuleDynamicallyRootNode();
                break;
            case ModuleBlock:
                root = new ModuleBlockRootNode();
                break;
            default:
                root = null;
        }

        CallTarget callTarget = Truffle.getRuntime().createCallTarget(root);
        return JSFunctionData.createCallOnly(context, callTarget, 0, "");
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return ImportCallNode.create(context, cloneUninitialized(argRefNode, materializedTags), cloneUninitialized(activeScriptOrModuleNode, materializedTags));
    }
}
