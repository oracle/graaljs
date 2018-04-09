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
package com.oracle.truffle.js.runtime.array;

import com.oracle.truffle.js.runtime.array.TypedArray.DirectFloat32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectFloat64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint8ClampedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.Float32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Float64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8ClampedArray;

public enum TypedArrayFactory {
    Int8Array(TypedArray.INT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectInt8Array(this, offset);
            } else {
                return new Int8Array(this, offset);
            }
        }
    },
    Uint8Array(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectUint8Array(this, offset);
            } else {
                return new Uint8Array(this, offset);
            }
        }
    },
    Uint8ClampedArray(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectUint8ClampedArray(this, offset);
            } else {
                return new Uint8ClampedArray(this, offset);
            }
        }
    },
    Int16Array(TypedArray.INT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectInt16Array(this, offset);
            } else {
                return new Int16Array(this, offset);
            }
        }
    },
    Uint16Array(TypedArray.UINT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectUint16Array(this, offset);
            } else {
                return new Uint16Array(this, offset);
            }
        }
    },
    Int32Array(TypedArray.INT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectInt32Array(this, offset);
            } else {
                return new Int32Array(this, offset);
            }
        }
    },
    Uint32Array(TypedArray.UINT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectUint32Array(this, offset);
            } else {
                return new Uint32Array(this, offset);
            }
        }
    },
    Float32Array(TypedArray.FLOAT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectFloat32Array(this, offset);
            } else {
                return new Float32Array(this, offset);
            }
        }
    },
    Float64Array(TypedArray.FLOAT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(boolean direct, boolean offset) {
            if (direct) {
                return new DirectFloat64Array(this, offset);
            } else {
                return new Float64Array(this, offset);
            }
        }
    };

    private final int bytesPerElement;
    private final TypedArray arrayType;
    private final TypedArray arrayTypeWithOffset;
    private final TypedArray directArrayType;
    private final TypedArray directArrayTypeWithOffset;

    TypedArrayFactory(int bytesPerElement) {
        this.bytesPerElement = bytesPerElement;
        this.arrayType = instantiateArrayType(false, false);
        this.arrayTypeWithOffset = instantiateArrayType(false, true);
        this.directArrayType = instantiateArrayType(true, false);
        this.directArrayTypeWithOffset = instantiateArrayType(true, true);
        assert !arrayType.hasOffset() && arrayTypeWithOffset.hasOffset() && !directArrayType.hasOffset() && directArrayTypeWithOffset.hasOffset();
    }

    public final TypedArray createArrayType(boolean direct, boolean offset) {
        if (direct) {
            if (offset) {
                return directArrayTypeWithOffset;
            } else {
                return directArrayType;
            }
        } else {
            if (offset) {
                return arrayTypeWithOffset;
            } else {
                return arrayType;
            }
        }
    }

    public final int bytesPerElement() {
        return bytesPerElement;
    }

    public final int getFactoryIndex() {
        return ordinal();
    }

    public final String getName() {
        return name();
    }

    abstract TypedArray instantiateArrayType(boolean direct, boolean offset);

    static final TypedArrayFactory[] FACTORIES = TypedArrayFactory.values();
}
