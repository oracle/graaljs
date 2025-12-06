/**
 * @option js.extractors
 */

load('../../assert.js');

const MapExtractor = {
    [Symbol.customMatcher](map) {
        const obj = {};
        for (const [key, value] of map) {
            obj[typeof key === "symbol" ? key : `${key}`] = value;
        }
        return [obj];
    }
};

// Basic Map extraction
{
    const obj = {
        map: new Map([["a", 1], ["b", 2]])
    };

    const { map: MapExtractor({ a, b }) } = obj;

    assertSame(a, 1);
    assertSame(b, 2);
}

// Map with various key types
{
    const sym = Symbol('key');
    const map = new Map([
        ["string", 10],
        [42, 20],
        [sym, 30]
    ]);

    const MapExtractor({ string, "42": numKey, [sym]: symValue }) = map;

    assertSame(10, string);
    assertSame(20, numKey);
    assertSame(30, symValue);
}

// Nested object with Map
{
    const data = {
        metadata: {
            values: new Map([
                ["x", 100],
                ["y", 200],
                ["z", 300]
            ])
        }
    };

    const { metadata: { values: MapExtractor({ x, y, z }) } } = data;

    assertSame(100, x);
    assertSame(200, y);
    assertSame(300, z);
}

// Partial extraction from Map
{
    const map = new Map([
        ["a", 1],
        ["b", 2],
        ["c", 3],
        ["d", 4]
    ]);

    const MapExtractor({ a, c }) = map;

    assertSame(1, a);
    assertSame(3, c);
}

// Function parameter with Map extractor
{
    function processMap(MapExtractor({ x, y })) {
        return x + y;
    }

    const map = new Map([["x", 5], ["y", 10]]);
    const result = processMap(map);

    assertSame(15, result);
}

// Empty Map
{
    const map = new Map();
    const MapExtractor({}) = map;
    // Should succeed with no bindings
}

// Map in array destructuring
{
    const maps = [
        new Map([["value", 1]]),
        new Map([["value", 2]]),
        new Map([["value", 3]])
    ];

    const [
        MapExtractor({ value: v1 }),
        MapExtractor({ value: v2 }),
        MapExtractor({ value: v3 })
    ] = maps;

    assertSame(1, v1);
    assertSame(2, v2);
    assertSame(3, v3);
}

