/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test --regexp-match-indices with ES version < 2018.
 *
 * @option ecmascript-version=6
 * @option regexp-match-indices=true
 */

load('assert.js');

assertThrows(() => eval('/./s'), SyntaxError);
assertThrows(() => eval('/./sd'), SyntaxError);
assertSame('d', /./d.flags);
assertSame('dy', /./dy.flags);
