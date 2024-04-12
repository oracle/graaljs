/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * String methods trimLeft() and trimRight() should be available when Annex B is enabled,
 * regardless of ecmascript-version, i.e. also when trimStart() and trimEnd() are not available.
 * 
 * @option annex-b
 * @option ecmascript-version=2018
 */

load('../assert.js');

// ES2019+ only.
assertFalse(String.prototype.hasOwnProperty("trimStart"));
assertFalse(String.prototype.hasOwnProperty("trimEnd"));

assertSame('ok ', " ok ".trimLeft());
assertSame(' ok', " ok ".trimRight());
