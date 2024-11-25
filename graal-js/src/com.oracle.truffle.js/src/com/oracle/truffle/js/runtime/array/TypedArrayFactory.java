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

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.array.TypedArray.BigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.BigUint64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.DirectBigUint64Array;
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
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigInt64Array;
import com.oracle.truffle.js.runtime.array.TypedArray.InteropBigUint64Array;
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
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt8Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt8Array(this, offset);
            } else {
                return new Int8Array(this, offset);
            }
        }
    },
    Uint8Array(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint8Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint8Array(this, offset);
            } else {
                return new Uint8Array(this, offset);
            }
        }
    },
    Uint8ClampedArray(TypedArray.UINT8_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint8ClampedArray(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint8ClampedArray(this, offset);
            } else {
                return new Uint8ClampedArray(this, offset);
            }
        }
    },
    Int16Array(TypedArray.INT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt16Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt16Array(this, offset);
            } else {
                return new Int16Array(this, offset);
            }
        }
    },
    Uint16Array(TypedArray.UINT16_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint16Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint16Array(this, offset);
            } else {
                return new Uint16Array(this, offset);
            }
        }
    },
    Int32Array(TypedArray.INT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropInt32Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectInt32Array(this, offset);
            } else {
                return new Int32Array(this, offset);
            }
        }
    },
    Uint32Array(TypedArray.UINT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropUint32Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectUint32Array(this, offset);
            } else {
                return new Uint32Array(this, offset);
            }
        }
    },
    Float32Array(TypedArray.FLOAT32_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropFloat32Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectFloat32Array(this, offset);
            } else {
                return new Float32Array(this, offset);
            }
        }
    },
    Float64Array(TypedArray.FLOAT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropFloat64Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectFloat64Array(this, offset);
            } else {
                return new Float64Array(this, offset);
            }
        }
    },
    BigInt64Array(TypedArray.BIGINT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropBigInt64Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectBigInt64Array(this, offset);
            } else {
                return new BigInt64Array(this, offset);
            }
        }
    },
    BigUint64Array(TypedArray.BIGUINT64_BYTES_PER_ELEMENT) {
        @Override
        TypedArray instantiateArrayType(byte bufferType, boolean offset) {
            if (bufferType == TypedArray.BUFFER_TYPE_INTEROP) {
                return new InteropBigUint64Array(this, offset);
            } else if (bufferType == TypedArray.BUFFER_TYPE_DIRECT) {
                return new DirectBigUint64Array(this, offset);
            } else {
                return new BigUint64Array(this, offset);
            }
        }
    };

    private final int bytesPerElement;
    private final TruffleString name;
    private final TypedArray arrayType;
    private final TypedArray arrayTypeWithOffset;
    private final TypedArray directArrayType;
    private final TypedArray directArrayTypeWithOffset;
    private final TypedArray interopArrayType;
    private final TypedArray interopArrayTypeWithOffset;

    TypedArrayFactory(int bytesPerElement) {
        this.bytesPerElement = bytesPerElement;
        this.name = Strings.constant(name());
        this.arrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, false);
        this.arrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_ARRAY, true);
        this.directArrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, false);
        this.directArrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_DIRECT, true);
        this.interopArrayType = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, false);
        this.interopArrayTypeWithOffset = instantiateArrayType(TypedArray.BUFFER_TYPE_INTEROP, true);

        assert !arrayType.hasOffset() && arrayTypeWithOffset.hasOffset() && !arrayType.isDirect() && !arrayTypeWithOffset.isDirect() &&
                        !directArrayType.hasOffset() && directArrayTypeWithOffset.hasOffset() && directArrayType.isDirect() && directArrayTypeWithOffset.isDirect() &&
                        !interopArrayType.hasOffset() && interopArrayTypeWithOffset.hasOffset() && interopArrayType.isInterop() && interopArrayTypeWithOffset.isInterop();
    }

    public final TypedArray createArrayType(boolean direct, boolean offset) {
        return createArrayType(direct, offset, false);
    }

    public final TypedArray createArrayType(boolean direct, boolean offset, boolean interop) {
        if (interop) {
            if (offset) {
                return interopArrayTypeWithOffset;
            } else {
                return interopArrayType;
            }
        } else if (direct) {
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

    public final int getBytesPerElement() {
        return bytesPerElement;
    }

    public final int getFactoryIndex() {
        return ordinal();
    }

    public final TruffleString getName() {
        return name;
    }

    abstract TypedArray instantiateArrayType(byte bufferType, boolean offset);

    @Override
    public final JSDynamicObject getIntrinsicDefaultProto(JSRealm realm) {
        return realm.getArrayBufferViewPrototype(this);
    }

    public final boolean isBigInt() {
        return this == BigInt64Array || this == BigUint64Array;
    }

    public final boolean isFloat() {
        return this == Float64Array || this == Float32Array;
    }

    static final TypedArrayFactory[] FACTORIES = TypedArrayFactory.values();
    private static TypedArrayFactory[] FACTORIES_NO_BIGINT;

    public static TypedArrayFactory[] getNoBigIntFactories() {
        if (FACTORIES_NO_BIGINT == null) {
            TypedArrayFactory[] allFactories = TypedArrayFactory.values();
            List<TypedArrayFactory> noBigIntFactories = new ArrayList<>(allFactories.length);
            for (TypedArrayFactory factory : allFactories) {
                if (!factory.isBigInt()) {
                    noBigIntFactories.add(factory);
                }
            }
            FACTORIES_NO_BIGINT = noBigIntFactories.toArray(new TypedArrayFactory[0]);
        }
        return FACTORIES_NO_BIGINT;
    }

}
