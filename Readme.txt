Language description

#Comment

Variables

Only integer type supported


Uninitialized variable
var a;

Initialized variable
var b = 1;

Constant
const daysInWeek = 7;

Uninitialized array
var someArray[10];

Initialized array
var otherArray = {1, 2, 3, 4, 5};


Arithmetics
y = (x + 1) / 2 * i;


Functions

fun main() {
	return 0;
}

fun func1(x, y) {
    # local variables don't need 'var' keyword
    a = 1;
	return x + y + a;
}


Conditional operations

x = (a > 0) ? 3 : 4;
y = (a == 1) ? (a + 2) : func(a);

(a == 0) ? {
	doSomething();
	doSomethingElse();
}


Cycles

{
	y[i] = i;
    i = i + 1;
} while (i < 10);


{
	a = a - 1;
} until (a > 0);






