/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
(async function() {

    let assert = await import('assert');
    assert.equal(42, 42);

    let undef = await import('./foo/bar/esModule.mjs');
    assert.equal(undef.default(undefined), true);

    let {hello} = await import('./a.mjs');
    assert.equal('hello module!', hello);

    return 'all OK!';

})().then(x => console.log(x)).catch(console.log);
