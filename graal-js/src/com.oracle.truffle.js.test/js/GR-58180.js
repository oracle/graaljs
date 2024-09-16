/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test of the correct broadcasting of SharedArrayBuffer to Test262 agents.
 * 
 * @option test262-mode
 */

load("assert.js");

assertThrows(() => $262.agent.broadcast({}), TypeError);

$262.agent.start(`
  $262.agent.receiveBroadcast(function(sab) {
    $262.agent.report((sab instanceof SharedArrayBuffer) && (sab.byteLength === 8));
    $262.agent.leaving();
  });
`);

$262.agent.broadcast(new SharedArrayBuffer(8));

let result;
while (true) {
    result = $262.agent.getReport();
    if (result !== null) { // result available
        assertSame('true', result);
        break;
    }
    $262.agent.sleep(100);
}
