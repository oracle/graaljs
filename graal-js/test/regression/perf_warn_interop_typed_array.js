/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

const ByteBuffer = Java.type("java.nio.ByteBuffer");

const foreignBuffer = ByteBuffer.allocateDirect(64);
for (let i = 0; i < 64; i++) {
  foreignBuffer.put(i, i);
}

const source = new Uint8Array(foreignBuffer);

function copyFromSource() {
  const copy = new Uint8Array(source);
  if (copy.length !== 64 || copy[0] !== 0 || copy[63] !== 63) {
    throw new Error("unexpected copy contents");
  }
  return copy[1] + copy[62];
}

let sum = 0;
for (let i = 0; i < 100_000; i++) {
  sum += copyFromSource();
}

if (sum !== 63 * 100_000) {
  throw new Error(`unexpected sum: ${sum}`);
}

print("done");
