/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load("./assert.js")

function check(actual, expected) {
    assertSame(JSON.stringify(expected), JSON.stringify(actual));
}

check(new Intl.NumberFormat('en').formatRangeToParts(42,Infinity),
    [
      { type: 'integer', value: '42', source: 'startRange' },
      { type: 'literal', value: '–', source: 'shared' },
      { type: 'infinity', value: '∞', source: 'endRange' }
    ]
);

check(new Intl.NumberFormat('en').formatRangeToParts(-Infinity, 42),
    [
      { type: 'minusSign', value: '-', source: 'startRange' },
      { type: 'infinity', value: '∞', source: 'startRange' },
      { type: 'literal', value: ' – ', source: 'shared' },
      { type: 'integer', value: '42', source: 'endRange' }
    ]
);
