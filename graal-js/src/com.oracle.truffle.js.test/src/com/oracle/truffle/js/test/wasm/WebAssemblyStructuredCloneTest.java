/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.js.test.wasm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotAccess;
import org.junit.Test;

import com.oracle.truffle.js.runtime.JSContextOptions;
import com.oracle.truffle.js.runtime.SerializedData;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.wasm.JSWebAssemblyModuleObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.test.JSTest;
import com.oracle.truffle.js.test.TestHelper;

public class WebAssemblyStructuredCloneTest {
    private static final String RESULT_MODULE_SOURCE = """
                    var bytes = new Uint8Array([
                        0, 97, 115, 109, 1, 0, 0, 0, 1, 5, 1, 96, 0, 1, 127, 3, 2, 1, 0,
                        7, 10, 1, 6, 114, 101, 115, 117, 108, 116, 0, 0, 10, 6, 1, 4, 0,
                        65, 42, 11
                    ]);
                    var module = new WebAssembly.Module(bytes);
                    var instance = new WebAssembly.Instance(module);
                    if (instance.exports.result() !== 42) {
                        throw new Error('unexpected wasm result');
                    }
                    """;

    @Test
    public void testWebAssemblyModuleCloneAcrossSharingLayers() {
        try (Engine engine = Engine.newBuilder().allowExperimentalOptions(true).option("engine.DisableCodeSharing", "true").build();
                        TestHelper source = new TestHelper(newContextBuilder(engine));
                        TestHelper target = new TestHelper(newContextBuilder(engine))) {
            source.runVoid(RESULT_MODULE_SOURCE);

            Object module = JSObject.get(source.getGlobalObject(), Strings.fromJavaString("module"));
            assertTrue(module instanceof JSWebAssemblyModuleObject);

            target.enterContext();
            Object clonedModule = new SerializedData(module).deserialize(target.getRealm());
            target.leaveContext();
            assertTrue(clonedModule instanceof JSWebAssemblyModuleObject);
            assertNotSame(((JSWebAssemblyModuleObject) module).getWASMModule(),
                            ((JSWebAssemblyModuleObject) clonedModule).getWASMModule());

            JSObject.set(target.getGlobalObject(), Strings.fromJavaString("module"), clonedModule);
            assertEquals(42, target.runValue("new WebAssembly.Instance(module).exports.result()").asInt());
        }
    }

    private static Context.Builder newContextBuilder(Engine engine) {
        PolyglotAccess polyglotAccess = PolyglotAccess.newBuilder().allowEval("js", "wasm").allowBindingsAccess("js").allowBindingsAccess("wasm").build();
        return JSTest.newContextBuilder("js", "wasm").engine(engine).allowPolyglotAccess(polyglotAccess).option(JSContextOptions.WEBASSEMBLY_NAME, "true");
    }
}
