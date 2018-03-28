/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
