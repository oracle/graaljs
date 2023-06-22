/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests AsyncContext proposal.
 *
 * @option unhandled-rejections=throw
 * @option async-context
 * @option testV8-mode
 */

load("assert.js");

const context = new AsyncContext.Variable({name: 'ctx', defaultValue: 'default'});
const timeout = 10; // random timeout

if (typeof setTimeout === 'undefined' && typeof TestV8 !== 'undefined') {
  setTimeout = TestV8.setTimeout;
}

assertSame(context.name, 'ctx');
assertSame(context.get(), 'default');

context.run("top", main);

function main() {
  setTimeout(() => {
    assertSame(context.get(), 'top');

    context.run("A", () => {
      assertSame(context.get(), 'A');

      setTimeout(() => {
        assertSame(context.get(), 'A');
      }, timeout);
    });
  }, timeout);

  context.run("B", () => {
    assertSame(context.get(), 'B');

    setTimeout(() => {
      assertSame(context.get(), 'B');
    }, timeout);
  });

  assertSame(context.get(), 'top');

  const snapshotDuringTop = new AsyncContext.Snapshot();

  context.run("C", () => {
    assertSame(context.get(), 'C');

    snapshotDuringTop.run(() => {
      assertSame(context.get(), 'top');
    });
  });
}
