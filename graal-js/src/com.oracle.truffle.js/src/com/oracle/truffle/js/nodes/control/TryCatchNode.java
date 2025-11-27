/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.nodes.control;

import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleStackTrace;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags.TryBlockTag;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.InitErrorObjectNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNode;
import com.oracle.truffle.js.nodes.control.TryCatchNodeFactory.GetErrorObjectNodeGen;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.nodes.instrumentation.JSTags.ControlFlowRootTag;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSRuntime;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.UserScriptException;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * 12.14 The try Statement.
 */
@NodeInfo(shortName = "try-catch")
public class TryCatchNode extends StatementNode implements ResumableNode.WithObjectState {

    @Child private JavaScriptNode tryBlock;
    @Child private JavaScriptNode catchBlock;
    @Child private JSWriteFrameSlotNode writeErrorVar;
    @Child private BlockScopeNode blockScope;
    @Child private JavaScriptNode destructuring;
    @Child private JavaScriptNode conditionExpression; // non-standard extension
    @Child private GetErrorObjectNode getErrorObjectNode;
    private final JSContext context;

    protected TryCatchNode(JSContext context, JavaScriptNode tryBlock, JavaScriptNode catchBlock, JSWriteFrameSlotNode writeErrorVar, BlockScopeNode blockScope, JavaScriptNode destructuring,
                    JavaScriptNode conditionExpression) {
        this.context = context;
        this.tryBlock = tryBlock;
        this.writeErrorVar = writeErrorVar;
        this.catchBlock = catchBlock;
        this.blockScope = blockScope;
        this.destructuring = destructuring;
        this.conditionExpression = conditionExpression == null ? null : JSToBooleanUnaryNode.create(conditionExpression);
    }

    public static TryCatchNode create(JSContext context, JavaScriptNode tryBlock, JavaScriptNode catchBlock, JSWriteFrameSlotNode writeErrorVar, BlockScopeNode blockScope,
                    JavaScriptNode destructuring, JavaScriptNode conditionExpression) {
        return new TryCatchNode(context, tryBlock, catchBlock, writeErrorVar, blockScope, destructuring, conditionExpression);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        if (tag == ControlFlowRootTag.class || tag == TryBlockTag.class) {
            return true;
        }
        return super.hasTag(tag);
    }

    @Override
    public Object getNodeObject() {
        return JSTags.createNodeObjectDescriptor("type", ControlFlowRootTag.Type.ExceptionHandler.name());
    }

    @Override
    protected JavaScriptNode copyUninitialized(Set<Class<? extends Tag>> materializedTags) {
        return create(context, cloneUninitialized(tryBlock, materializedTags), cloneUninitialized(catchBlock, materializedTags), cloneUninitialized(writeErrorVar, materializedTags),
                        cloneUninitialized(blockScope, materializedTags), cloneUninitialized(destructuring, materializedTags),
                        cloneUninitialized(conditionExpression, materializedTags));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return tryBlock.isResultAlwaysOfType(clazz) && catchBlock.isResultAlwaysOfType(clazz);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        Throwable throwable;
        try {
            return tryBlock.execute(frame);
        } catch (ControlFlowException cfe) {
            throw cfe;
        } catch (AbstractTruffleException ex) {
            throwable = ex;
        } catch (StackOverflowError ste) {
            throwable = ste;
        }
        return executeCatch(frame, throwable);
    }

    @Override
    public final void executeVoid(VirtualFrame frame) {
        Throwable throwable;
        try {
            tryBlock.executeVoid(frame);
            return;
        } catch (ControlFlowException cfe) {
            throw cfe;
        } catch (AbstractTruffleException ex) {
            throwable = ex;
        } catch (StackOverflowError ste) {
            throwable = ste;
        }
        executeCatch(frame, throwable);
    }

    private Object executeCatch(VirtualFrame frame, Throwable ex) {
        if (blockScope != null) {
            blockScope.appendScopeFrame(frame);
        }
        try {
            if (prepareCatch(frame, ex)) {
                return catchBlock.execute(frame);
            } else {
                throw JSRuntime.rethrow(ex);
            }
        } finally {
            if (blockScope != null) {
                blockScope.exitScope(frame);
            }
        }
    }

    private boolean prepareCatch(VirtualFrame frame, Throwable ex) {
        if (writeErrorVar != null) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(GetErrorObjectNode.create(context));
            }
            Object exceptionObject = getErrorObjectNode.execute(ex);
            writeErrorVar.executeWrite(frame, exceptionObject);

            if (destructuring != null) {
                destructuring.execute(frame);
            }
        }

        return conditionExpression == null || executeConditionAsBoolean(frame, conditionExpression);
    }

    @Override
    public Object resume(VirtualFrame frame, int stateSlot) {
        Object state = getStateAndReset(frame, stateSlot);
        if (state == Undefined.instance) {
            Throwable throwable;
            try {
                return tryBlock.execute(frame);
            } catch (ControlFlowException cfe) {
                throw cfe;
            } catch (AbstractTruffleException ex) {
                throwable = ex;
            } catch (StackOverflowError ste) {
                throwable = ste;
            }
            if (blockScope != null) {
                blockScope.appendScopeFrame(frame);
            }
            if (!prepareCatch(frame, throwable)) {
                throw JSRuntime.rethrow(throwable);
            }
            // fall through to execute catch block
        } else {
            if (blockScope != null) {
                blockScope.setBlockScope(frame, state);
            }
        }
        boolean yield = false;
        try {
            return catchBlock.execute(frame);
        } catch (YieldException e) {
            yield = true;
            if (blockScope == null) {
                setState(frame, stateSlot, 1);
            } else {
                state = blockScope.getBlockScope(frame);
                assert state != Undefined.instance;
                setState(frame, stateSlot, state);
            }
            throw e;
        } finally {
            if (blockScope != null) {
                blockScope.exitScope(frame, yield);
            }
        }
    }

    public abstract static class GetErrorObjectNode extends JavaScriptBaseNode {
        @Child private InitErrorObjectNode initErrorObjectNode;
        @Child private TruffleString.FromJavaStringNode fromJavaStringNode;
        private final JSContext context;

        protected GetErrorObjectNode(JSContext context) {
            this.context = context;
            this.initErrorObjectNode = InitErrorObjectNode.create(context, context.isOptionNashornCompatibilityMode());
            this.fromJavaStringNode = TruffleString.FromJavaStringNode.create();
        }

        public static GetErrorObjectNode create(JSContext context) {
            return GetErrorObjectNodeGen.create(context);
        }

        public abstract Object execute(Throwable ex);

        @Specialization
        final Object doJSException(JSException ex) {
            // fill in any missing stack trace elements
            TruffleStackTrace.fillIn(ex);

            return getOrCreateErrorFromJSException(ex);
        }

        @Specialization
        static Object doUserScriptException(UserScriptException ex) {
            return ex.getErrorObject();
        }

        @Specialization
        final Object doStackOverflowError(StackOverflowError ex) {
            JSException rangeError = Errors.createRangeErrorStackOverflow(ex, this);
            return getOrCreateErrorFromJSException(rangeError);
        }

        @Fallback
        static Object doOther(Throwable ex) {
            assert !(ex instanceof GraalJSException) && (ex instanceof AbstractTruffleException) : ex;
            // fill in any missing stack trace elements
            TruffleStackTrace.fillIn(ex);
            return ex;
        }

        private Object getOrCreateErrorFromJSException(JSException exception) {
            JSDynamicObject errorObj = exception.getErrorObjectLazy();
            // not thread safe, but should be alright in this case
            if (errorObj == null) {
                errorObj = createErrorFromJSException(exception);
            }
            return errorObj;
        }

        private JSErrorObject createErrorFromJSException(JSException exception) {
            JSRealm errorRealm = exception.getRealm();
            String message = exception.getRawMessage();
            assert message != null;
            JSErrorObject errorObj = newErrorObject(context, errorRealm, exception.getErrorType());
            initErrorObjectNode.execute(errorObj, exception, Strings.fromJavaString(fromJavaStringNode, message));
            exception.setErrorObject(errorObj);
            return errorObj;
        }

        @TruffleBoundary
        private static JSErrorObject newErrorObject(JSContext context, JSRealm realm, JSErrorType errorType) {
            return JSError.createErrorObject(context, realm, errorType);
        }
    }
}
