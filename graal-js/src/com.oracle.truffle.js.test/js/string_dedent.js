/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test for String.dedent.
 * Note: There are more tests in StringFunctionBuiltinsTest.
 *
 * @option ecmascript-version=staging
 */
load("assert.js");


assertSame('\v', String.dedent`
\v
`);

// Various invalid escape sequences.
assertSame('undefined', String.dedent`
\1
`);
assertSame('undefined42',String.dedent`
\1${42}
`);

assertSame('undefined', String.dedent`
\02
`);
assertSame('undefined42', String.dedent`
\02${42}
`);
assertSame('42undefined', String.dedent`
\
${42}\
`);
assertSame('undefined', String.dedent`
 \
`);

// Special cases defined in CookStrings grammar (not possible in actual template literals):
// Note: String.dedent requires a leading and a trailing newline.
assertSame(String.dedent({raw: ['\n', '${', '\n']}, 6, 9), '6${9');
assertSame(String.dedent({raw: ['\n', '`',  '\n']}, 6, 9), '6`9');
assertSame(String.dedent({raw: ['\n', '',   '\n']}, 6, 9), '69');

assertSame(String.dedent({raw: ['\n\\\n']}, 'STOP'), 'undefined');
assertSame(String.dedent({raw: ['\n', '\\', '\n']}, 6, 9, 'STOP'), '6undefined9');
assertSame(String.dedent({raw: ['\n\\', '\\', '\n\\\n']}, 6, 9, 'STOP'), 'undefined6undefined9undefined');

// Missing substitutions
assertSame(String.dedent({raw: ['\n{', '}\n']}), "{}");
assertSame(String.dedent({raw: ['\n{', ',', '}\n']}), "{,}");
