.class public Life
.super java/lang/Object

.field public UNDERPOP_LIM I
.field public OVERPOP_LIM I
.field public REPRODUCE_NUM I
.field public LOOPS_PER_MS I
.field public xMax I
.field public yMax I
.field public field [I

.method public<init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99


	.invokevirtual Life/init()I


.end method

.method public init()I
	.limit stack 99
	.limit locals 99


	bipush 2
	istore -1

	bipush 3
	istore -1

	bipush 3
	istore -1

	bipush 225000
	istore -1


	istore 2

	bipush 1
	isub
	istore -1

	bipush 1
	isub
	istore -1

.end method

.method public field([I)[I
	.limit stack 99
	.limit locals 99


	bipush 10

	bipush 0

	bipush 0

	bipush 1

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 1

	bipush 0

	bipush 1

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 1

	bipush 1

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

	bipush 0

.end method

.method public update()I
	.limit stack 99
	.limit locals 99


	bipush 0
	istore 1


.end method

.method public printField()I
	.limit stack 99
	.limit locals 99

	bipush 0
	istore 1

	bipush 0
	istore 2

	.invokestatic io/println()V

	.invokestatic io/println()V

.end method

.method public trIdx(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public cartIdx(I)[I
	.limit stack 99
	.limit locals 99

	bipush 1
	iadd
	istore 3

	istore 2

	isub
	istore 1




.end method

.method public getNeighborCoords(I)[I
	.limit stack 99
	.limit locals 99


	istore 1

	istore 2










.end method

.method public getLiveNeighborN(I)I
	.limit stack 99
	.limit locals 99

	bipush 0
	istore 3


	bipush 0
	istore 2

.end method

.method public busyWait(I)I
	.limit stack 99
	.limit locals 99

	istore 2

	bipush 0
	istore 1

.end method

.method public eq(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public ne(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public lt(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public le(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public gt(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public ge(II)I
	.limit stack 99
	.limit locals 99

.end method


