Language description


Variables

Only integer type supported

var a;
var b = 1;
var someArray[10];
var otherArray = {1, 2, 3, 4, 5};


Arithmetics

y = (x + 1) / 2 * i;


Functions

fun main() {
	return 0;
}

fun func1(x, y) {
	return x + y;
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
	x = x + 1;
} repeat(10);


{
	y[i] = i;
    i = i + 1;
} while (i < 10);


{
	a = a - 1;
} until (a > 0);






