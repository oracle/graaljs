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
package com.oracle.truffle.js.runtime.array;

import java.util.Arrays;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray.BigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.BigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectFloat16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectFloat32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectFloat64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectInt8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectUint8ClampedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.Float16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Float32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Float64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Int8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropFloat16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropFloat32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropFloat64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropInt8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropUint8ClampedArray;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint16Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint32Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8Array;
import com.oracle.truffle.js.runtime.array.TypedArray.Uint8ClampedArray;
import com.oracle.truffle.js.runtime.builtins.PrototypeSupplier;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

public enum TypedArrayFactory implements PrototypeSupplier {
    Int8Array(TypedArray.INT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt8Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt8Array(this, offset, fixedLength);
            } else {
                return new Int8Array(this, offset, fixedLength);
            }
        }
    },
    Uint8Array(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint8Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint8Array(this, offset, fixedLength);
            } else {
                return new Uint8Array(this, offset, fixedLength);
            }
        }
    },
    Uint8ClampedArray(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint8ClampedArray(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint8ClampedArray(this, offset, fixedLength);
            } else {
                return new Uint8ClampedArray(this, offset, fixedLength);
            }
        }
    },
    Int16Array(TypedArray.INT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt16Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt16Array(this, offset, fixedLength);
            } else {
                return new Int16Array(this, offset, fixedLength);
            }
        }
    },
    Uint16Array(TypedArray.UINT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint16Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint16Array(this, offset, fixedLength);
            } else {
                return new Uint16Array(this, offset, fixedLength);
            }
        }
    },
    Int32Array(TypedArray.INT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt32Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt32Array(this, offset, fixedLength);
            } else {
                return new Int32Array(this, offset, fixedLength);
            }
        }
    },
    Uint32Array(TypedArray.UINT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint32Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint32Array(this, offset, fixedLength);
            } else {
                return new Uint32Array(this, offset, fixedLength);
            }
        }
    },
    Float32Array(TypedArray.FLOAT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropFloat32Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectFloat32Array(this, offset, fixedLength);
            } else {
                return new Float32Array(this, offset, fixedLength);
            }
        }
    },
    Float64Array(TypedArray.FLOAT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropFloat64Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectFloat64Array(this, offset, fixedLength);
            } else {
                return new Float64Array(this, offset, fixedLength);
            }
        }
    },
    BigInt64Array(TypedArray.BIGINT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropBigInt64Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectBigInt64Array(this, offset, fixedLength);
            } else {
                return new BigInt64Array(this, offset, fixedLength);
            }
        }
    },
    BigUint64Array(TypedArray.BIGUINT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropBigUint64Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectBigUint64Array(this, offset, fixedLength);
            } else {
                return new BigUint64Array(this, offset, fixedLength);
            }
        }
    },
    Float16Array(TypedArray.FLOAT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropFloat16Array(this, offset, fixedLength);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectFloat16Array(this, offset, fixedLength);
            } else {
                return new Float16Array(this, offset, fixedLength);
            }
        }
    };

    private final int bytesPerElement;
    private final TruffleString name;
    private final TypedArray arrayType;
    private final TypedArray arrayTypeWithOffset;
    private final TypedArray arrayTypeAutoLength;
    private final TypedArray arrayTypeWithOffsetAutoLength;
    private final TypedArray directArrayType;
    private final TypedArray directArrayTypeWithOffset;
    private final TypedArray directArrayTypeAutoLength;
    private final TypedArray directArrayTypeWithOffsetAutoLength;
    private final TypedArray interopArrayType;
    private final TypedArray interopArrayTypeWithOffset;
    private final TypedArray interopArrayTypeAutoLength;
    private final TypedArray interopArrayTypeWithOffsetAutoLength;

    TypedArrayFactory(int bytesPerElement) {
        this.bytesPerElement = bytesPerElement;
        this.name = Strings.constant(name());
        this.arrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, false, true);
        this.arrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, true, true);
        this.arrayTypeAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, false, false);
        this.arrayTypeWithOffsetAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, true, false);
        this.directArrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, false, true);
        this.directArrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, true, true);
        this.directArrayTypeAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, false, false);
        this.directArrayTypeWithOffsetAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, true, false);
        this.interopArrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, false, true);
        this.interopArrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, true, true);
        this.interopArrayTypeAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, false, false);
        this.interopArrayTypeWithOffsetAutoLength = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, true, false);

        assert !arrayType.hasOffset() && arrayTypeWithOffset.hasOffset() && !arrayType.isDirect() && !arrayTypeWithOffset.isDirect() &&
                        !directArrayType.hasOffset() && directArrayTypeWithOffset.hasOffset() && directArrayType.isDirect() && directArrayTypeWithOffset.isDirect() &&
                        !interopArrayType.hasOffset() && interopArrayTypeWithOffset.hasOffset() && interopArrayType.isInterop() && interopArrayTypeWithOffset.isInterop();
    }

    public final TypedArray createArrayType(boolean direct, boolean offset, boolean fixedLength) {
        return createArrayType(direct, offset, fixedLength, false);
    }

    public final TypedArray createArrayType(boolean direct, boolean offset, boolean fixedLength, boolean interop) {
        if (interop) {
            if (offset) {
                return fixedLength ? interopArrayTypeWithOffset : interopArrayTypeWithOffsetAutoLength;
            } else {
                return fixedLength ? interopArrayType : interopArrayTypeAutoLength;
            }
        } else if (direct) {
            if (offset) {
                return fixedLength ? directArrayTypeWithOffset : directArrayTypeWithOffsetAutoLength;
            } else {
                return fixedLength ? directArrayType : directArrayTypeAutoLength;
            }
        } else {
            if (offset) {
                return fixedLength ? arrayTypeWithOffset : arrayTypeWithOffsetAutoLength;
            } else {
                return fixedLength ? arrayType : arrayTypeAutoLength;
            }
        }
    }

    public final int getBytesPerElement() {
        return bytesPerElement;
    }

    public final int getFactoryIndex() {
        return ordinal();
    }

    public final TruffleString getName() {
        return name;
    }

    abstract TypedArray instantiateArrayType(byte bufferType, boolean offset, boolean fixedLength);

    @Override
    public final JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getArrayBufferViewPrototype(this);
    }

    public final boolean isBigInt() {
        return this == BigInt64Array || this == BigUint64Array;
    }

    public final boolean isFloat() {
        return this == Float64Array || this == Float32Array || this == Float16Array;
    }

    static final TypedArrayFactory[] FACTORIES_ALL = TypedArrayFactory.values();
    static final TypedArrayFactory[] FACTORIES_DEFAULT = Arrays.copyOf(FACTORIES_ALL, Float16Array.ordinal());
    static final TypedArrayFactory[] FACTORIES_NO_BIGINT = Arrays.copyOf(FACTORIES_ALL, BigInt64Array.ordinal());

}
