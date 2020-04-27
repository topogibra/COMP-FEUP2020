.class public Quicksort
.super java/lang/Object


.method public<init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99


	bipush 0
	istore 2


	.invokevirtual Quicksort/quicksort([I)I

	.invokevirtual Quicksort/printL([I)I

.end method

.method public printL([I)I
	.limit stack 99
	.limit locals 99

	bipush 0
	istore 1

.end method

.method public quicksort([I)I
	.limit stack 99
	.limit locals 99

.end method

.method public quicksort([III)I
	.limit stack 99
	.limit locals 99

.end method

.method public partition([III)I
	.limit stack 99
	.limit locals 99

	istore 1

	istore 2

	istore 3

	istore 4



.end method


