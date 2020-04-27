.class public FindMaximum
.super java/lang/Object

.field public test_arr [I

.method public<init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public find_maximum([I)I
	.limit stack 99
	.limit locals 99

	bipush 1
	istore 1

	istore 2

.end method

.method public build_test_arr()I
	.limit stack 99
	.limit locals 99


	bipush 14

	bipush 28

	bipush 0

	bipush 0
	bipush 5
	isub

	bipush 12

.end method

.method public get_array()[I
	.limit stack 99
	.limit locals 99

.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99


	.invokevirtual FindMaximum/build_test_arr()I

	.invokestatic ioPlus/printResult(I)V

.end method


