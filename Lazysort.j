.class public Lazysort
.super Quicksort


.method public<init>()V
	aload_0
	invokenonvirtual Quicksort/<init>()V
	return
.end method



.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99


	bipush 0
	istore 2


	.invokevirtual Lazysort/quicksort([I)I

	istore 3

.end method

.method public quicksort([I)I
	.limit stack 99
	.limit locals 99

.end method

.method public beLazy([I)I
	.limit stack 99
	.limit locals 99

	istore 1

	bipush 0
	istore 2

.end method


