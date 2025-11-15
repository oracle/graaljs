import { spawnPromisified } from '../common/index.mjs';
import { describe, it } from 'node:test';
import { strictEqual, match } from 'node:assert';

describe('unusual top-level await syntax errors', () => {
  const expressions = [
    // string
    { expression: '""' },
    // number
    { expression: '0' },
    // boolean
    { expression: 'true' },
    // null
    { expression: 'null' },
    // undefined
    { expression: 'undefined' },
    // object
    { expression: '{}' },
    // array
    { expression: '[]' },
    // new
    { expression: 'new Date()' },
    // identifier
    { initialize: 'const a = 2;', expression: 'a' },
  ];
  it('should not crash the process', async () => {
    for (const { expression, initialize } of expressions) {
      const wrapperExpressions = [
        `function callAwait() {}; callAwait(await ${expression});`,
        `if (await ${expression}) {}`,
        `{ key: await ${expression} }`,
        `[await ${expression}]`,
        `(await ${expression})`,
      ];
      for (const wrapperExpression of wrapperExpressions) {
        const { code, signal, stdout, stderr } = await spawnPromisified(process.execPath, [
          '--eval',
          `
          ${initialize || ''}
          ${wrapperExpression}
          `,
        ]);

        strictEqual(stderr, '');
        strictEqual(stdout, '');
        strictEqual(code, 0);
        strictEqual(signal, null);
      }
    }
  });

  it('should throw the error for unrelated syntax errors', async () => {
    const expression = 'foo bar';
    const wrapperExpressions = [
      [`function callSyntaxError() {}; callSyntaxError(${expression});`, /Expected . but found|missing \) after argument list/],
      [`if (${expression}) {}`, /Expected . but found|Unexpected identifier/],
      [`{ key: ${expression} }`, /Expected . but found|Unexpected identifier/],
      [`[${expression}]`, /Expected comma but found|Unexpected identifier/],
      [`(${expression})`, /Expected . but found|Unexpected identifier/],
      [`const ${expression} = 1;`, /'const' declarations must be initialized|Missing initializer in const declaration/],
      [`console.log('PI: ' Math.PI);`, /Expected ',', got|missing \) after argument list/],
      [`callAwait(await "" "");`, /Expected ',', got|missing \) after argument list/],
    ];

    for (const [wrapperExpression, error] of wrapperExpressions) {
      const { code, signal, stdout, stderr } = await spawnPromisified(process.execPath, [
        '--eval',
        `
          ${wrapperExpression}
          `,
      ]);
      match(stderr, error);
      strictEqual(stdout, '');
      strictEqual(code, 1);
      strictEqual(signal, null);
    }
  });
});
