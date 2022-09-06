package com.oracle.truffle.js.builtins.wasm;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.js.lang.JavaScriptLanguage;
import com.oracle.truffle.js.runtime.interop.JSMetaType;
import com.oracle.truffle.js.runtime.objects.Undefined;

/**
 * Representation of undefined used for WebAssembly Interop.
 */
@ExportLibrary(InteropLibrary.class)
public final class WebAssemblyUndefined implements TruffleObject {
    public static final WebAssemblyUndefined instance = new WebAssemblyUndefined();

    private WebAssemblyUndefined() {

    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasLanguage() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Class<? extends TruffleLanguage<?>> getLanguage() {
        return JavaScriptLanguage.class;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return Undefined.NAME;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean hasMetaObject() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    Object getMetaObject() {
        return JSMetaType.JS_UNDEFINED;
    }
}
