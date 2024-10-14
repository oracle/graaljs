/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Regression test of the processing of the enqueued messages by a finished worker.
 * 
 * @option worker
 */

load("assert.js");

var w = new Worker('for (let i=0; i<10; i++) postMessage(i*i);', { type: 'string' });

for (let i=0; i<10; i++) {
    assertSame(i*i, w.getMessage());
}
