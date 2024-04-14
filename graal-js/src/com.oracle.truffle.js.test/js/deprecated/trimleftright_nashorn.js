/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * String methods trimLeft() and trimRight() should be available in nashorn-compat mode.
 * 
 * @option nashorn-compat
 */

load('../assert.js');

assertSame('ok ', " ok ".trimLeft());
assertSame(' ok', " ok ".trimRight());
