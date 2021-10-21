/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.trufflenode;

import static com.oracle.truffle.trufflenode.buffer.NIOBuffer.NIO_BUFFER_MODULE_NAME;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.JSArguments;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.JavaScriptRootNode;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSFunctionData;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.trufflenode.buffer.NIOBuffer;
import com.oracle.truffle.trufflenode.node.debug.SetBreakPointNode;
import com.oracle.truffle.trufflenode.threading.SharedMemMessagingBindings;

class InternalScriptRootNode extends JavaScriptRootNode {

    private static final boolean USE_NIO_BUFFER = !"false".equals(System.getProperty("node.buffer.nio"));

    private final String internalScriptName;

    public InternalScriptRootNode(DynamicObject moduleFunction, String scriptNodeName) {
        this.internalScriptName = scriptNodeName;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        DynamicObject thisObject = (DynamicObject) JSArguments.getThisObject(args);
        RealmData realmData = GraalJSAccess.getRealmEmbedderData(getRealm());
        DynamicObject moduleFunction = realmData.getInternalScriptFunction(internalScriptName);
        return JSFunction.call(moduleFunction, thisObject, getInternalModuleUserArguments(args, internalScriptName, getRealm()));
    }

    @CompilerDirectives.TruffleBoundary
    private Object[] getInternalModuleUserArguments(Object[] args, String moduleName, JSRealm realm) {
        Object[] userArgs = JSArguments.extractUserArguments(args);
        Object extraArgument = getExtraArgumentOfInternalScript(moduleName, realm);
        if (extraArgument == null) {
            return userArgs;
        }
        Object[] extendedArgs = new Object[userArgs.length + 1];
        System.arraycopy(userArgs, 0, extendedArgs, 0, userArgs.length);
        extendedArgs[userArgs.length] = extraArgument;
        return extendedArgs;
    }

    private Object getExtraArgumentOfInternalScript(String moduleName, JSRealm realm) {
        Object extraArgument = null;
        JSContext context = realm.getContext();
        if (NIO_BUFFER_MODULE_NAME.equals(moduleName)) {
            // NIO-based buffer APIs in internal/graal/buffer.js are initialized by passing one
            // extra argument to the module loading function.
            extraArgument = USE_NIO_BUFFER ? NIOBuffer.createInitFunction(realm) : Null.instance;
        } else if ("internal/graal/debug.js".equals(moduleName)) {
            CallTarget setBreakPointCallTarget = Truffle.getRuntime().createCallTarget(new SetBreakPointNode(context));
            JSFunctionData setBreakPointData = JSFunctionData.createCallOnly(context, setBreakPointCallTarget, 3, SetBreakPointNode.NAME);
            DynamicObject setBreakPoint = JSFunction.create(realm, setBreakPointData);
            extraArgument = setBreakPoint;
        } else if ("internal/worker/io.js".equals(moduleName) || "internal/main/worker_thread.js".equals(moduleName)) {
            // The Shared-mem channel initialization is similar to NIO-based buffers.
            extraArgument = SharedMemMessagingBindings.createInitFunction(realm);
        } else if ("inspector.js".equals(moduleName)) {
            TruffleObject inspector = GraalJSAccess.get().lookupInstrument("inspect", TruffleObject.class);
            extraArgument = (inspector == null) ? Null.instance : inspector;
        }
        return extraArgument;
    }
}
