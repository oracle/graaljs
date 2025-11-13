package com.oracle.truffle.js.nodes.extractor;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;
import com.oracle.truffle.js.nodes.JavaScriptNode;
import com.oracle.truffle.js.nodes.access.GetIteratorNode;
import com.oracle.truffle.js.nodes.access.GetMethodNode;
import com.oracle.truffle.js.nodes.access.IsObjectNode;
import com.oracle.truffle.js.nodes.function.JSFunctionCallNode;
import com.oracle.truffle.js.runtime.Errors;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.Symbol;
import com.oracle.truffle.js.runtime.objects.IteratorRecord;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * https://tc39.es/proposal-extractors/#sec-invokecustommatcherorthrow
 */
public abstract class InvokeCustomMatcherOrThrowNode extends JavaScriptNode {
    @Child private GetMethodNode getCustomMatcherNode;
    @Child private JSFunctionCallNode callNode;

    @Child @Executed JavaScriptNode matcher;
    @Child @Executed JavaScriptNode subject;
    @Child @Executed JavaScriptNode receiver;

    protected InvokeCustomMatcherOrThrowNode(JSContext context, JavaScriptNode matcher, JavaScriptNode subject, JavaScriptNode receiver) {
        this.getCustomMatcherNode = GetMethodNode.create(context, Symbol.SYMBOL_CUSTOM_MATCHER);
        this.callNode = JSFunctionCallNode.createCall();

        this.matcher = matcher;
        this.subject = subject;
        this.receiver = receiver;
    }

    public static InvokeCustomMatcherOrThrowNode create(JSContext context, JavaScriptNode matcher, JavaScriptNode subject, JavaScriptNode receiver) {
        return InvokeCustomMatcherOrThrowNodeGen.create(context, matcher, subject, receiver);
    }

    public abstract IteratorRecord execute(Object matcher, Object subject, Object receiver);

    @Specialization
    protected final IteratorRecord invokeCustomMatcherOrThrow(Object matcher, Object subject, Object receiver,
                                                              @Cached InlinedBranchProfile errorBranchProfile,
                                                              @Cached IsObjectNode isObjectNode,
                                                              @Cached GetIteratorNode getIteratorNode) {
        // 1. If matcher is not an Object, throw a TypeError exception.
        if (!isObjectNode.executeBoolean(matcher)) {
            errorBranchProfile.enter(this);
            throw Errors.createTypeErrorNotAnObject(matcher);
        }

        // 2. Let f be ? GetMethod(matcher, @@customMatcher).
        final var f = this.getCustomMatcherNode.executeWithTarget(matcher);

        // 3. If f is undefined, throw a TypeError exception.
        if (f == Undefined.instance) {
            errorBranchProfile.enter(this);
            throw Errors.createTypeError("matcher does not have Symbol.customMatcher method");
        }

        // 4. Let result be ? Call(f, matcher, « subject, "list", receiver »).
        final var result = this.callNode.executeCall(JSArguments.create(matcher, f, subject, "list", receiver));

        // 5. If result is not an Object, throw a TypeError exception.
        if (!isObjectNode.executeBoolean(result)) {
            errorBranchProfile.enter(this);
            throw Errors.createTypeErrorInvalidCustomMatcherReturnValue(result, this);
        }

        // 6. Let iteratorRecord be ? GetIterator(result, sync). & 7. Return iteratorRecord.
        return getIteratorNode.execute(this, result);
    }
}
