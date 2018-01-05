/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.oracle.truffle.js.nodes.control;

import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.js.nodes.JavaScriptBaseNode;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.JSWriteFrameSlotNode;
import com.oracle.truffle.js.nodes.access.PropertySetNode;
import com.oracle.truffle.js.nodes.cast.JSToBooleanNode;
import com.oracle.truffle.js.nodes.function.BlockScopeNode;
import com.oracle.truffle.js.nodes.function.CreateMethodPropertyNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.GraalJSException.JSStackTraceElement;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSErrorType;
import com.oracle.truffle.js.runtime.JSException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JSTruffleOptions;
import com.oracle.truffle.js.runtime.builtins.JSError;
import com.oracle.truffle.js.runtime.objects.JSAttributes;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.JSProperty;
import com.oracle.truffle.js.runtime.objects.PropertyDescriptor;
import com.oracle.truffle.js.runtime.objects.Undefined;
import com.oracle.truffle.js.runtime.util.JSClassProfile;

/**
 * 12.14 The try Statement.
 */
@NodeInfo(shortName = "try-catch")
public class TryCatchNode extends StatementNode implements ResumableNode {

    @Child private JavaScriptNode tryBlock;
    @Child private JavaScriptNode catchBlock;
    @Child private JSWriteFrameSlotNode writeErrorVar;
    @Child private BlockScopeNode blockScope;
    @Child private JavaScriptNode destructuring;
    @Child private JavaScriptNode conditionExpression; // non-standard extension
    @Child private GetErrorObjectNode getErrorObjectNode;
    private final JSContext context;

    private final BranchProfile catchBranch = BranchProfile.create();

    protected TryCatchNode(JSContext context, JavaScriptNode tryBlock, JavaScriptNode catchBlock, JSWriteFrameSlotNode writeErrorVar, BlockScopeNode blockScope, JavaScriptNode destructuring,
                    JavaScriptNode conditionExpression) {
        this.context = context;
        this.tryBlock = tryBlock;
        this.writeErrorVar = writeErrorVar;
        this.catchBlock = catchBlock;
        this.blockScope = blockScope;
        this.destructuring = destructuring;
        this.conditionExpression = conditionExpression == null ? null : JSToBooleanNode.create(conditionExpression);
        assert blockScope != null || writeErrorVar == null;
    }

    public static TryCatchNode create(JSContext context, JavaScriptNode tryBlock, JavaScriptNode catchBlock, JSWriteFrameSlotNode writeErrorVar, BlockScopeNode blockScope,
                    JavaScriptNode destructuring, JavaScriptNode conditionExpression) {
        return new TryCatchNode(context, tryBlock, catchBlock, writeErrorVar, blockScope, destructuring, conditionExpression);
    }

    @Override
    protected JavaScriptNode copyUninitialized() {
        return create(context, cloneUninitialized(tryBlock), cloneUninitialized(catchBlock), cloneUninitialized(writeErrorVar), cloneUninitialized(blockScope), cloneUninitialized(destructuring),
                        cloneUninitialized(conditionExpression));
    }

    @Override
    public boolean isResultAlwaysOfType(Class<?> clazz) {
        return tryBlock.isResultAlwaysOfType(clazz) && catchBlock.isResultAlwaysOfType(clazz);
    }

    @Override
    public final Object execute(VirtualFrame frame) {
        try {
            return tryBlock.execute(frame);
        } catch (ControlFlowException cfe) {
            throw cfe;
        } catch (Throwable ex) {
            catchBranch.enter();
            if (ex instanceof TruffleException || ex instanceof StackOverflowError) {
                return executeCatch(frame, ex);
            } else {
                throw ex;
            }
        }
    }

    private Object executeCatch(VirtualFrame frame, Throwable ex) {
        VirtualFrame catchFrame = blockScope == null ? frame : blockScope.appendScopeFrame(frame);
        try {
            return executeCatchInner(catchFrame, ex);
        } finally {
            if (blockScope != null) {
                blockScope.exitScope(catchFrame);
            }
        }
    }

    private Object executeCatchInner(VirtualFrame catchFrame, Throwable ex) {
        if (writeErrorVar != null) {
            if (getErrorObjectNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getErrorObjectNode = insert(GetErrorObjectNode.create(context));
            }
            Object exceptionObject = getErrorObjectNode.execute(ex);
            writeErrorVar.executeWrite(catchFrame, exceptionObject);

            if (destructuring != null) {
                destructuring.execute(catchFrame);
            }
        }

        if (conditionExpression == null || executeConditionAsBoolean(catchFrame, conditionExpression)) {
            return catchBlock.execute(catchFrame);
        } else {
            throw ((RuntimeException) ex);
        }
    }

    @Override
    public Object resume(VirtualFrame frame) {
        Object state = getStateAndReset(frame);
        if (state == Undefined.instance) {
            try {
                return tryBlock.execute(frame);
            } catch (ControlFlowException cfe) {
                throw cfe;
            } catch (GraalJSException | StackOverflowError ex) {
                catchBranch.enter();
                VirtualFrame catchFrame = blockScope == null ? frame : blockScope.appendScopeFrame(frame);
                try {
                    return executeCatchInner(catchFrame, ex);
                } catch (YieldException e) {
                    setState(frame, catchFrame);
                    throw e;
                } finally {
                    if (blockScope != null) {
                        blockScope.exitScope(catchFrame);
                    }
                }
            }
        } else {
            VirtualFrame catchFrame = (VirtualFrame) state;
            try {
                return catchBlock.execute(catchFrame);
            } catch (YieldException e) {
                setState(frame, catchFrame);
                throw e;
            }
        }
    }

    static final class GetErrorObjectNode extends JavaScriptBaseNode {
        @Child InitErrorObjectNode initErrorObjectNode;
        private final ConditionProfile isJSError = ConditionProfile.createBinaryProfile();
        private final ConditionProfile isJSException = ConditionProfile.createBinaryProfile();
        private final BranchProfile truffleExceptionBranch = BranchProfile.create();
        private final JSContext context;

        private GetErrorObjectNode(JSContext context) {
            this.context = context;
            this.initErrorObjectNode = InitErrorObjectNode.create(context, JSTruffleOptions.NashornCompatibilityMode);
        }

        static GetErrorObjectNode create(JSContext context) {
            return new GetErrorObjectNode(context);
        }

        Object execute(Throwable ex) {
            if (isJSError.profile(ex instanceof JSException)) {
                return doJSException((JSException) ex);
            } else if (isJSException.profile(ex instanceof GraalJSException)) {
                return ((GraalJSException) ex).getErrorObject();
            } else if (ex instanceof StackOverflowError) {
                CompilerDirectives.transferToInterpreter();
                JSException rangeException = Errors.createCallStackSizeExceededError();
                return doJSException(rangeException);
            } else {
                truffleExceptionBranch.enter();
                assert ex instanceof TruffleException : ex;
                TruffleException truffleException = (TruffleException) ex;
                Object exceptionObject = truffleException.getExceptionObject();
                if (exceptionObject != null) {
                    return exceptionObject;
                }

                if (JSTruffleOptions.NashornJavaInterop) {
                    return ex;
                } else {
                    return JavaInterop.asTruffleObject(ex);
                }
            }
        }

        private Object doJSException(JSException exception) {
            DynamicObject errorObj = exception.getErrorObject();
            // not thread safe, but should be alright in this case
            if (errorObj == null) {
                JSRealm realm = exception.getRealm();
                if (realm == null) {
                    realm = context.getRealm();
                }
                errorObj = createErrorFromJSException(exception, realm);
                initErrorObjectNode.execute(errorObj, exception, realm);
                exception.setErrorObject(errorObj);
            }
            return errorObj;
        }

        @TruffleBoundary
        private static DynamicObject createErrorFromJSException(JSException exception, JSRealm realm) {
            JSErrorType errorType = exception.getErrorType();
            JSContext ctx = realm.getContext();
            return JSObject.create(ctx, ctx.getErrorFactory(errorType, true), Objects.requireNonNull(exception.getRawMessage()));
        }
    }

    public static final class InitErrorObjectNode extends JavaScriptBaseNode {
        @Child private CreateMethodPropertyNode setLineNumber;
        @Child private CreateMethodPropertyNode setColumnNumber;
        @Child private PropertySetNode setException;
        @Child private PropertySetNode setFormattedStack;
        private final JSClassProfile classProfile = JSClassProfile.create();
        private final boolean defaultColumnNumber;

        private InitErrorObjectNode(JSContext context, boolean defaultColumnNumber) {
            this.setLineNumber = CreateMethodPropertyNode.create(context, JSError.LINE_NUMBER_PROPERTY_NAME);
            this.setColumnNumber = CreateMethodPropertyNode.create(context, JSError.COLUMN_NUMBER_PROPERTY_NAME);
            this.setException = PropertySetNode.create(JSError.EXCEPTION_PROPERTY_NAME, false, context, false);
            this.setFormattedStack = PropertySetNode.create(JSError.FORMATTED_STACK_NAME, false, context, false);
            this.defaultColumnNumber = defaultColumnNumber;
        }

        public static InitErrorObjectNode create(JSContext context, boolean defaultColumnNumber) {
            return new InitErrorObjectNode(context, defaultColumnNumber);
        }

        public DynamicObject execute(DynamicObject errorObj, GraalJSException exception, JSRealm realm) {
            setException.setValue(errorObj, exception);
            // stack is not formatted until it is accessed
            setFormattedStack.setValue(errorObj, null);
            PropertyDescriptor desc = JSObject.getOwnProperty(errorObj, JSError.STACK_NAME, classProfile);
            int attrs = JSAttributes.getDefaultNotEnumerable();
            if (desc != null) {
                if (!desc.getConfigurable()) {
                    throw Errors.createTypeErrorCannotRedefineProperty(JSError.STACK_NAME);
                }
                if (desc.getEnumerable()) {
                    attrs = JSAttributes.getDefault();
                }
            }
            /// use nodes (GR-1989)
            errorObj.define(JSError.STACK_NAME, realm.getErrorStackAccessor(), attrs | JSProperty.ACCESSOR);
            if (JSTruffleOptions.NashornCompatibilityMode && exception.getJSStackTrace().length > 0) {
                JSStackTraceElement topStackTraceElement = exception.getJSStackTrace()[0];
                setLineNumber.executeVoid(errorObj, topStackTraceElement.getLineNumber());
                setColumnNumber.executeVoid(errorObj, defaultColumnNumber ? JSError.DEFAULT_COLUMN_NUMBER : topStackTraceElement.getColumnNumber());
            }
            return errorObj;
        }
    }
}
