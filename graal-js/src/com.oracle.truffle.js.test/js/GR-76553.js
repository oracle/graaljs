/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load("assert.js");

(function copyWithinPreservesFloat32NaNPayload() {
    const buffer = new ArrayBuffer(8);
    const dataView = new DataView(buffer);
    dataView.setUint32(0, 0x7f801234, true);

    const float32Array = new Float32Array(buffer);
    float32Array.copyWithin(1, 0, 1);

    assertSame(0x7f801234, dataView.getUint32(4, true));
})();

(function copyWithinPreservesFloat16NaNPayload() {
    const buffer = new ArrayBuffer(4);
    const dataView = new DataView(buffer);
    dataView.setUint16(0, 0x7c01, true);

    const float16Array = new Float16Array(buffer);
    float16Array.copyWithin(1, 0, 1);

    assertSame(0x7c01, dataView.getUint16(2, true));
})();
