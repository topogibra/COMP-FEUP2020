.class public TicTacToe
.super java/lang/Object

.field public row0 [I
.field public row1 [I
.field public row2 [I
.field public whoseturn I
.field public movesmade I
.field public pieces [I

.method public<init>()V
	aload_0
	invokenonvirtual java/lang/Object/<init>()V
	return
.end method

.method public init()I
	.limit stack 99
	.limit locals 99





	bipush 1

	bipush 2

	bipush 0
	istore -1

	bipush 0
	istore -1

.end method

.method public getRow0()[I
	.limit stack 99
	.limit locals 99

.end method

.method public getRow1()[I
	.limit stack 99
	.limit locals 99

.end method

.method public getRow2()[I
	.limit stack 99
	.limit locals 99

.end method

.method public MoveRow([II)I
	.limit stack 99
	.limit locals 99

.end method

.method public Move(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public inbounds(II)I
	.limit stack 99
	.limit locals 99

.end method

.method public changeturn()I
	.limit stack 99
	.limit locals 99

	bipush 1
	isub
	istore -1

.end method

.method public getCurrentPlayer()I
	.limit stack 99
	.limit locals 99

.end method

.method public winner()I
	.limit stack 99
	.limit locals 99

	bipush 0
	bipush 1
	isub
	istore 2


.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99


	.invokevirtual TicTacToe/init()I

	.invokestatic BoardBase/printBoard([I[I[I)V

	istore 2

	.invokestatic BoardBase/printWinner(I)V

.end method


