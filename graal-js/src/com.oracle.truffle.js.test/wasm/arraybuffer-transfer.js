/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Checks that a buffer of WebAssembly.Memory cannot be transferred.
 * 
 * @option webassembly
 * @option ecmascript-version=staging
 */

load('../js/assert.js');

var memory = new WebAssembly.Memory({ initial: 1 });
assertThrows(() => memory.buffer.transfer(), TypeError);
assertThrows(() => memory.buffer.transferToFixedLength(), TypeError);
