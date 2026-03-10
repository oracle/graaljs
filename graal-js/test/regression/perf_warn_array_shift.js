/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function shiftNumbers() {
  const values = [];
  values.push(1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000);
  let total = 0;
  while (values.length !== 0) {
    total += values.shift();
  }
  return total;
}

function run() {
  let sum = 0;
  for (let i = 0; i < 200_000; i++) {
    sum += shiftNumbers();
  }
  return sum;
}

const sum = run();
if (sum !== 36_000 * 200_000) {
  throw new Error(`unexpected sum: ${sum}`);
}

print("done");
