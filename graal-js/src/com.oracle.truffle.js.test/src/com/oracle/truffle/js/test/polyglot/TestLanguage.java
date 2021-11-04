/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.polyglot;

import java.util.function.Consumer;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags.ExpressionTag;
import com.oracle.truffle.api.instrumentation.StandardTags.RootTag;
import com.oracle.truffle.api.instrumentation.StandardTags.StatementTag;
import com.oracle.truffle.api.nodes.ExecutableNode;
import com.oracle.truffle.js.test.polyglot.TestLanguage.LanguageContext;

/**
 * Reusable language for testing.
 */
@TruffleLanguage.Registration(id = TestLanguage.ID, name = TestLanguage.ID, version = "1.0", contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
@ProvidedTags({ExpressionTag.class, StatementTag.class, RootTag.class})
public class TestLanguage extends TruffleLanguage<LanguageContext> {
    public static final String ID = "jsTestLanguage";

    public static class LanguageContext {
        final Env env;

        LanguageContext(Env env) {
            this.env = env;
        }

        public Env getEnv() {
            return env;
        }
    }

    private static volatile TestLanguage delegate = new TestLanguage(false);
    private static final ContextReference<LanguageContext> CONTEXT_REF = ContextReference.create(TestLanguage.class);
    private static final LanguageReference<TestLanguage> LANGUAGE_REF = LanguageReference.create(TestLanguage.class);

    protected final boolean wrapper;
    protected TestLanguage languageInstance;

    private Consumer<LanguageContext> onCreate;

    public TestLanguage() {
        this.wrapper = getClass() == TestLanguage.class;
    }

    private TestLanguage(boolean wrapper) {
        this.wrapper = wrapper;
    }

    private static <T extends TestLanguage> T setDelegate(T delegate) {
        TestLanguage.delegate = delegate;
        return delegate;
    }

    public void setOnCreate(Consumer<LanguageContext> onCreate) {
        this.onCreate = onCreate;
    }

    public static AutoCloseable withTestLanguage(TestLanguage testLanguage) {
        setDelegate(testLanguage);
        return () -> setDelegate(null);
    }

    public static LanguageContext getCurrentContext() {
        return CONTEXT_REF.get(null);
    }

    public static TestLanguage getCurrentLanguage() {
        return LANGUAGE_REF.get(null);
    }

    @Override
    protected LanguageContext createContext(com.oracle.truffle.api.TruffleLanguage.Env env) {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.createContext(env);
        } else {
            LanguageContext c = new LanguageContext(env);
            if (onCreate != null) {
                onCreate.accept(c);
            }
            return c;
        }
    }

    @Override
    protected void finalizeContext(LanguageContext context) {
        if (wrapper) {
            delegate.finalizeContext(context);
        } else {
            super.finalizeContext(context);
        }
    }

    @Override
    protected void disposeContext(LanguageContext context) {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.disposeContext(context);
        } else {
            super.disposeContext(context);
        }
    }

    @Override
    protected void disposeThread(LanguageContext context, Thread thread) {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.disposeThread(context, thread);
        } else {
            super.disposeThread(context, thread);
        }
    }

    @Override
    protected void initializeContext(LanguageContext context) throws Exception {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.initializeContext(context);
        } else {
            super.initializeContext(context);
        }

    }

    @Override
    protected void initializeMultipleContexts() {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.initializeMultipleContexts();
        } else {
            super.initializeMultipleContexts();
        }
    }

    @Override
    protected void initializeMultiThreading(LanguageContext context) {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.initializeMultiThreading(context);
        } else {
            super.initializeMultiThreading(context);
        }
    }

    @Override
    protected void initializeThread(LanguageContext context, Thread thread) {
        if (wrapper) {
            delegate.languageInstance = this;
            delegate.initializeThread(context, thread);
        } else {
            super.initializeThread(context, thread);
        }
    }

    @Override
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.isThreadAccessAllowed(thread, singleThreaded);
        } else {
            return super.isThreadAccessAllowed(thread, singleThreaded);
        }
    }

    @Override
    protected boolean isVisible(LanguageContext context, Object value) {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.isVisible(context, value);
        } else {
            return super.isVisible(context, value);
        }
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.getOptionDescriptors();
        } else {
            return super.getOptionDescriptors();
        }
    }

    @Override
    protected CallTarget parse(com.oracle.truffle.api.TruffleLanguage.ParsingRequest request) throws Exception {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.parse(request);
        } else {
            return super.parse(request);
        }
    }

    @Override
    protected Object getScope(LanguageContext context) {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.getScope(context);
        } else {
            return super.getScope(context);
        }
    }

    @Override
    protected ExecutableNode parse(InlineParsingRequest request) throws Exception {
        if (wrapper) {
            delegate.languageInstance = this;
            return delegate.parse(request);
        } else {
            return super.parse(request);
        }
    }
}
