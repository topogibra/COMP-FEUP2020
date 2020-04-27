.class public MonteCarloPi
.super Math

.field public a I
.field public b [I
.field public ola I
.field public pir MonteCarloPi

.method public<init>()V
	aload_0
	invokenonvirtual Math/<init>()V
	return
.end method

.method public performSingleEstimate()MonteCarloPi
	.limit stack 99
	.limit locals 99

	.invokevirtual MonteCarloPi/performSingleEstimate()MonteCarloPi

	.invokevirtual MonteCarloPi/performSingleEstimate()MonteCarloPi

	.invokevirtual MonteCarloPi/performSingleEstimate()MonteCarloPi

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


