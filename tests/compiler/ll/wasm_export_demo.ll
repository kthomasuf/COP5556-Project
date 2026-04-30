declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%TCounter = type { i32 }
@DemoCounter = global %TCounter* null

define void @TCounter_Init(%TCounter* %self, i32 %start) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %start.addr = alloca i32
  store i32 %start, i32* %start.addr
  %t0 = load i32, i32* %start.addr
  %t1 = load %TCounter*, %TCounter** %self.addr
  %t2 = getelementptr %TCounter, %TCounter* %t1, i32 0, i32 0
  store i32 %t0, i32* %t2
  ret void
}

define void @TCounter_Add(%TCounter* %self, i32 %step) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %step.addr = alloca i32
  store i32 %step, i32* %step.addr
  %t3 = load %TCounter*, %TCounter** %self.addr
  %t4 = getelementptr %TCounter, %TCounter* %t3, i32 0, i32 0
  %t5 = load i32, i32* %t4
  %t6 = load i32, i32* %step.addr
  %t7 = add i32 %t5, %t6
  %t8 = load %TCounter*, %TCounter** %self.addr
  %t9 = getelementptr %TCounter, %TCounter* %t8, i32 0, i32 0
  store i32 %t7, i32* %t9
  ret void
}

define i32 @TCounter_Current(%TCounter* %self) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %t10 = load %TCounter*, %TCounter** %self.addr
  %t11 = getelementptr %TCounter, %TCounter* %t10, i32 0, i32 0
  %t12 = load i32, i32* %t11
  store i32 %t12, i32* %result.addr
  %t13 = load i32, i32* %result.addr
  ret i32 %t13
}

define i32 @AddOne(i32 %value) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %value.addr = alloca i32
  store i32 %value, i32* %value.addr
  %t14 = load i32, i32* %value.addr
  %t15 = add i32 %t14, 1
  store i32 %t15, i32* %result.addr
  %t16 = load i32, i32* %result.addr
  ret i32 %t16
}

define i32 @MaxValue(i32 %left, i32 %right) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %left.addr = alloca i32
  store i32 %left, i32* %left.addr
  %right.addr = alloca i32
  store i32 %right, i32* %right.addr
  %t17 = load i32, i32* %left.addr
  %t18 = load i32, i32* %right.addr
  %t19 = icmp sgt i32 %t17, %t18
  br i1 %t19, label %if.then.0, label %if.else.1
if.then.0:
  %t20 = load i32, i32* %left.addr
  store i32 %t20, i32* %result.addr
  br label %if.end.2
if.else.1:
  %t21 = load i32, i32* %right.addr
  store i32 %t21, i32* %result.addr
  br label %if.end.2
if.end.2:
  %t22 = load i32, i32* %result.addr
  ret i32 %t22
}

define i32 @SumToN(i32 %limit) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %limit.addr = alloca i32
  store i32 %limit, i32* %limit.addr
  store i32 0, i32* %result.addr
  br label %while.cond.3
while.cond.3:
  %t23 = load i32, i32* %limit.addr
  %t24 = icmp sgt i32 %t23, 0
  br i1 %t24, label %while.body.4, label %while.end.5
while.body.4:
  %t25 = load i32, i32* %result.addr
  %t26 = load i32, i32* %limit.addr
  %t27 = add i32 %t25, %t26
  store i32 %t27, i32* %result.addr
  %t28 = load i32, i32* %limit.addr
  %t29 = sub i32 %t28, 1
  store i32 %t29, i32* %limit.addr
  br label %while.cond.3
while.end.5:
  %t30 = load i32, i32* %result.addr
  ret i32 %t30
}

define i32 @CounterDemo(i32 %start, i32 %step) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %start.addr = alloca i32
  store i32 %start, i32* %start.addr
  %step.addr = alloca i32
  store i32 %step, i32* %step.addr
  %t31 = call i8* @malloc(i64 4)
  %t32 = bitcast i8* %t31 to %TCounter*
  %t33 = load i32, i32* %start.addr
  call void @TCounter_Init(%TCounter* %t32, i32 %t33)
  store %TCounter* %t32, %TCounter** @DemoCounter
  %t34 = load %TCounter*, %TCounter** @DemoCounter
  %t35 = load i32, i32* %step.addr
  call void @TCounter_Add(%TCounter* %t34, i32 %t35)
  %t36 = load %TCounter*, %TCounter** @DemoCounter
  %t37 = call i32 @TCounter_Current(%TCounter* %t36)
  store i32 %t37, i32* %result.addr
  %t38 = load i32, i32* %result.addr
  ret i32 %t38
}
define i32 @main() {
entry:
  ret i32 0
}
