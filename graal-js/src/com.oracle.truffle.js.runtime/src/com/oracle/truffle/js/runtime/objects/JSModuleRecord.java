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
package com.oracle.truffle.js.runtime.objects;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;

/**
 * Source Text Module Record.
 */
public final class JSModuleRecord {
    private final Object module;
    private final JSContext context;
    private final JSModuleLoader moduleLoader;
    private final Source source;

    /** Resolved is true after ModuleDeclarationInstantiation(). */
    private boolean resolved;
    /** Evaluated is true after ModuleEvaluation(). */
    private boolean evaluated;

    private JSFunctionData functionData;

    /** Lazily initialized Module Namespace object ({@code [[Namespace]]}). */
    private DynamicObject namespace;
    /** Lazily initialized frame ({@code [[Environment]]}). */
    private MaterializedFrame environment;

    private Runnable finishTranslation;

    public JSModuleRecord(Object module, JSContext context, JSModuleLoader moduleLoader, Source source, Runnable finishTranslation) {
        this.module = module;
        this.context = context;
        this.moduleLoader = moduleLoader;
        this.source = source;
        this.finishTranslation = finishTranslation;
    }

    public Object getModule() {
        return module;
    }

    public JSContext getContext() {
        return context;
    }

    public JSModuleLoader getModuleLoader() {
        return moduleLoader;
    }

    public Source getSource() {
        return source;
    }

    public JSFunctionData getFunctionData() {
        assert functionData != null;
        return functionData;
    }

    public void setFunctionData(JSFunctionData functionData) {
        assert this.functionData == null;
        this.functionData = functionData;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void setEvaluated(boolean evaluated) {
        this.evaluated = evaluated;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public DynamicObject getNamespace() {
        return namespace;
    }

    public void setNamespace(DynamicObject namespace) {
        assert this.namespace == null;
        this.namespace = namespace;
    }

    public MaterializedFrame getEnvironment() {
        return environment;
    }

    public void setEnvironment(MaterializedFrame environment) {
        assert this.environment == null;
        this.environment = environment;
    }

    public void finishTranslation() {
        assert isResolved();
        finishTranslation.run();
        finishTranslation = null;
    }
}
