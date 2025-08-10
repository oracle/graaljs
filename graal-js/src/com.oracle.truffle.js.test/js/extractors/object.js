load('../assert.js');

const MapExtractor = {
    [Symbol.customMatcher](map) {
        const obj = {};
        for (const [key, value] of map) {
            obj[typeof key === "symbol" ? key : `${key}`] = value;
        }
        return [obj];
    }
};

const obj = {
    map: new Map([["a", 1], ["b", 2]])
};

const { map: MapExtractor({ a, b }) } = obj;

assertSame(a, 1);
assertSame(b, 2);
