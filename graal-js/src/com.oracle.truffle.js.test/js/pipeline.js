/**
 * Pipeline operator proposal.
 */


load('assert.js');

function double(number){
    return number * 2;
}

function add(number1, number2){
    return number1 + number2;
}

function resolveAfter2Seconds(x) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(x);
    }, 2000);
  });
}

class Rectangle {
  constructor(height, width) {
    this.height = height;
    this.width = width;
  }
  // Getter
  get area() {
    return this.calcArea();
  }
  // Method
  calcArea() {
    return this.height * this.width;
  }
}

const array = ['Apple', 'Orange', 'Strawberry'];

const unaryFuncBody = 5 |> double(%);
assertEqual(10, unaryFunc);

const funcBody = double(3) |> add(%, 2);
assertEqual(8, func);

const methodPipeBody = new Rectangle(2, 3) |> %.calcArea();
assertEqual(6, methodPipeBody);

const arithmetic = (14 * 4) / 2 |> % + 1;
assertEqual(29, arithmetic);

const arrayLiteral = array.indexOf('Orange') |> array[%];
assertEqual('Orange', arrayLiteral);

const objectLiteral = 2 * 3 |> { type: "rectangle", area : %};
assertEqual({type: "rectangle", area: 6}, objectLiteral);

const templateLiteral = array[2] |> `${%}`;
assertEqual('Strawberry', templateLiteral);

const construction = (8/2) |> new Rectangle(2, %);
assertEqual(new Rectangle(2, 4), construction);

const awaiting = resolveAfter2Seconds(10) |> await %;
assertEqual(10, awaiting);

//yield

//import

/*
* Test chaining of pipeline
*/

const chainingExample1 = 7 |> new Rectangle(6, %) |> %.calcArea();
assertEqual(42, chainingExample1);

/*
* Error testing
*/

assertThrows(() => 15 |> 1 + 2, SyntaxError);
assertThrows(() => new Rectangle(2, 3) |> %.squareFootage(), TypeError);



