.class public MonteCarloPi
.super Math

.field public a I
.field public b [I
.field public ola I
.field public pir MonteCarloPi


.method public performSingleEstimate()I
	.limit stack 99
	.limit locals 99

	.invokevirtual MonteCarloPi/performSingleEstimate()I

	.invokevirtual MonteCarloPi/performSingleEstimate()I

	.invokevirtual MonteCarloPi/performSingleEstimate()I

	.invokespecial Math/cenas()I

.end method

.method public estimatePi100(I)I
	.limit stack 99
	.limit locals 99

	.invokevirtual MonteCarloPi/estimatePi100(I)I

.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 99
	.limit locals 99

	.invokestatic ioPlus/printResult(I)V

.end method


