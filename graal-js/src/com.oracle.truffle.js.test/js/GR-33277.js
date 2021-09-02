/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option intl-402
 */

load('assert.js');

for (const locale of ['yi', 'ji']) {
    assertSame('yi', Intl.DisplayNames.supportedLocalesOf([locale])[0]);
    assertSame('איירא', new Intl.DisplayNames(locale, { type: 'currency' }).of('eur'));
    assertSame('ענגליש', new Intl.DisplayNames(locale, { type: 'language' }).of('en'));
    assertSame('פֿאַראייניגטע שטאַטן', new Intl.DisplayNames(locale, { type: 'region' }).of('US'));
    assertSame('גַלחיש', new Intl.DisplayNames(locale, { type: 'script' }).of('Latn'));
}
