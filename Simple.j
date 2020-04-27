.class public Simple
.super java/lang/Object


.method public<init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99

	bipush 5
	bipush 4
	imul
	bipush 30
	idiv
	istore 1

	bipush 10
	bipush 0
	isub
	istore 2

	new Simple
	dup
	invokespecial Simple/<init>()V
	astore 3

	iload 2
	iload 1
	.invokevirtual Simple/add(II)I
	istore 4

	iload 4
	.invokestatic io/println(I)V

.end method

.method public add(II)I
	.limit stack 99
	.limit locals 99

.end method


