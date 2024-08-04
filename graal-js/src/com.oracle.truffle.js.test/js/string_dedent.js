/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests String.dedent.
 *
 * @option ecmascript-version=staging
 */
load("assert.js");

// Dedent single line with tab.
assertSame('value', String.dedent({raw: [`\n\tvalue\n`]}));

// Dedent with substitutions.
assertSame(String.dedent`
                            create table student(
                              key: \t${1+2},\r
                              name: ${"John"}\r
                            )

                            create table student(
                              key: ${8},
                              name: ${"Doe"}
                            )

`,
`create table student(
  key: \t3,\r
  name: John\r
)

create table student(
  key: 8,
  name: Doe
)
`);

// Dedent with callback.
assertSame(String.dedent(String.raw)`
                            create table student(
                              key: \t${1+2},\r
                              name: ${"John"}\r
                            )

                            create table student(
                              key: ${8},
                              name: ${"Doe"}
                            )
`,
`create table student(
  key: \\t3,\\r
  name: John\\r
)

create table student(
  key: 8,
  name: Doe
)`);

assertSame(function dedentedTemplateStringsArrayIdentity() {
    let first;
    function interceptRaw(template, ...substitutions) {
        if (!first) {
            first = template;
        } else if (template !== first) {
            throw new Error(`expected same identity`);
        }
        if (template.length !== 5 || substitutions.length !== 4) {
            throw new Error(`unexpected length: ${template.length !== 5 || substitutions.length !== 4}`);
        }
        return String.raw(template, ...substitutions);
    }

    let result;
    for (let i = 0; i < 2; i++) {
        result = String.dedent(interceptRaw)`
                                create table student(
                                  key: \t${1+2},\r
                                  name: ${"John"}\r
                                )

                                create table student(
                                  key: ${8},
                                  name: ${"Doe"}
                                )
        `;
    }
    return result;
}(),
`\
create table student(
  key: \\t3,\\r
  name: John\\r
)

create table student(
  key: 8,
  name: Doe
)\
`);

assertSame('\v', String.dedent`
\v
`);

// TypeError: The opening line must contain a trailing newline.
assertThrows(() => String.dedent`value`, TypeError);
assertThrows(() => String.dedent`value
`, TypeError);
// TypeError: The closing line must be preceded by a newline.
assertThrows(() => String.dedent`
value`, TypeError);

// TypeError: The opening line must be empty.
assertThrows(() => String.dedent`not empty
value
`, TypeError);
// TypeError: The closing line must be empty...
assertThrows(() => String.dedent`
value
not empty`, TypeError);
// ... but whitespace is allowed (and ignored).
assertSame('value', String.dedent`
value
    `);

// TypeError: First argument must be an object.
assertThrows(() => String.dedent(42), TypeError);

// TypeError: Template raw array must contain at least 1 string.
assertThrows(() => String.dedent({raw: []}), TypeError);

// TypeError: Template raw array may only contain strings.
assertThrows(() => String.dedent({raw: [42]}), TypeError);

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
