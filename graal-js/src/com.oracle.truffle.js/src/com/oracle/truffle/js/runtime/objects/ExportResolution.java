/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Objects;

import com.oracle.js.parser.ir.Module;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.strings.TruffleString;

/**
 * Result of the {@code ResolveExport} method of module records.
 */
public abstract class ExportResolution {
    private static final ExportResolution NULL = new Null();
    private static final ExportResolution AMBIGUOUS = new Ambiguous();

    private ExportResolution() {
    }

    /**
     * Definition not found or circular request.
     */
    public boolean isNull() {
        return false;
    }

    public boolean isAmbiguous() {
        return false;
    }

    public boolean isNamespace() {
        return false;
    }

    @TruffleBoundary
    public AbstractModuleRecord getModule() {
        throw new UnsupportedOperationException();
    }

    @TruffleBoundary
    public TruffleString getBindingName() {
        throw new UnsupportedOperationException();
    }

    public static ExportResolution resolved(AbstractModuleRecord module, TruffleString bindingName) {
        return new Resolved(module, bindingName);
    }

    /**
     * Definition not found or circular request.
     */
    public static ExportResolution notFound() {
        return NULL;
    }

    public static ExportResolution ambiguous() {
        return AMBIGUOUS;
    }

    public static final class Resolved extends ExportResolution {
        private final AbstractModuleRecord module;
        private final TruffleString bindingName;

        Resolved(AbstractModuleRecord module, TruffleString bindingName) {
            this.module = module;
            this.bindingName = bindingName;
            assert bindingName == Module.NAMESPACE_EXPORT_BINDING_NAME || !bindingName.equals(Module.NAMESPACE_EXPORT_BINDING_NAME);
        }

        @Override
        public AbstractModuleRecord getModule() {
            return module;
        }

        @Override
        public TruffleString getBindingName() {
            return bindingName;
        }

        @Override
        public boolean isNamespace() {
            return bindingName == Module.NAMESPACE_EXPORT_BINDING_NAME;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bindingName == null) ? 0 : bindingName.hashCode());
            result = prime * result + ((module == null) ? 0 : module.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Resolved other = (Resolved) obj;
            return Objects.equals(this.module, other.module) && Objects.equals(this.bindingName, other.bindingName);
        }
    }

    private static final class Null extends ExportResolution {
        @Override
        public boolean isNull() {
            return true;
        }
    }

    private static final class Ambiguous extends ExportResolution {
        @Override
        public boolean isAmbiguous() {
            return true;
        }
    }
}
