/**
 * Pipeline operator proposal.
 *
 */

/**
*@option --ecmascript-version=staging
*/

load('assert.js');

function double(number){
    return number * 2;
}

function add(number1, number2){
    return number1 + number2;
}

class Rectangle {
  constructor(height, width) {
    this.height = height;
    this.width = width;
  }

  get area() {
    return this.calcArea();
  }

  calcArea() {
    return this.height * this.width;
  }
}

const array = ['Apple', 'Orange', 'Strawberry'];

let unaryFuncBody = 5 |> double(%);
assertEqual(10, unaryFuncBody);

let funcBody = double(3) |> add(%, 2);
assertEqual(8, funcBody);

let methodPipeBody = new Rectangle(2, 3) |> %.calcArea();
assertEqual(6, methodPipeBody);

let arithmetic = (14 * 4) / 2 |> % + 1;
assertEqual(29, arithmetic);

let arrayLiteral = array.indexOf('Orange') |> array[%];
assertEqual('Orange', arrayLiteral);

let arrayLiteral2 = 2 |> [1, %, 3];
assertEqual(JSON.stringify([1, 2, 3]), JSON.stringify(arrayLiteral2));

let objectLiteral = 2 * 3 |> { type: "rectangle", area : %};
assertEqual(JSON.stringify({type: "rectangle", area: 6}), JSON.stringify(objectLiteral));

let templateLiteral = array[2] |> `${%}`;
assertEqual('Strawberry', templateLiteral);

let construction = (8/2) |> new Rectangle(2, %);
assertEqual(JSON.stringify(new Rectangle(2, 4)), JSON.stringify(construction));

function resolveAfter2Seconds(x) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(x);
    }, 2000);
  });
}

async function f1() {
  const x = 10 |> await resolveAfter2Seconds(%);
  console.log(x);
}

f1();

//yield
function* counter(value) {
   while (true) {
     const step = value++ |> yield %;

     if (step) {
       value += step;
     }
   }
}

const generatorFunc = counter(0);
assertEqual(0, generatorFunc.next().value);
assertEqual(1, generatorFunc.next().value);
assertEqual(2, generatorFunc.next().value);

//function body
let funcExpression = function test(value){
  let var1 = 4 + value;
  let var2 = 7 |> % * var1;
  console.log("Result: " + var2);
} |> %(2);

/*
* Test chaining of pipeline
*/

const chainingExample1 = 7 |> new Rectangle(6, %) |> %.calcArea();
assertEqual(42, chainingExample1);
const chainingExample2 = 7 |> new Rectangle(6, %) |> %.calcArea() |> % % %;
assertEqual(0, chainingExample2);
const chainingExample3 = 7 |> new Rectangle(6, %) |> %.calcArea() |> % % 2 |> array[%];
assertEqual('Apple', chainingExample3);
const chainingExample4 = 7 |> new Rectangle(6, %) |> %.calcArea() |> % % 2 |> array[%] |> `${%}`;
assertEqual('Apple', chainingExample4);
const chainingExample5 = 7 |> new Rectangle(6, %) |> %.calcArea() |> % % 2 |> array[%] |> `${%}` |> array.indexOf(%);
assertEqual(0, chainingExample5);

/*
* Error testing
*/

assertThrows(() => new Rectangle(2, 3) |> %.squareFootage(), TypeError);


