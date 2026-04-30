declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%TCounter = type { i32 }
@Counter = global %TCounter* null

define void @TCounter_Init(%TCounter* %self) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %t0 = load %TCounter*, %TCounter** %self.addr
  %t1 = getelementptr %TCounter, %TCounter* %t0, i32 0, i32 0
  store i32 5, i32* %t1
  ret void
}

define void @TCounter_AddOne(%TCounter* %self) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %t2 = load %TCounter*, %TCounter** %self.addr
  %t3 = getelementptr %TCounter, %TCounter* %t2, i32 0, i32 0
  %t4 = load i32, i32* %t3
  %t5 = add i32 %t4, 1
  %t6 = load %TCounter*, %TCounter** %self.addr
  %t7 = getelementptr %TCounter, %TCounter* %t6, i32 0, i32 0
  store i32 %t5, i32* %t7
  ret void
}

define void @TCounter_PrintCount(%TCounter* %self) {
entry:
  %self.addr = alloca %TCounter*
  store %TCounter* %self, %TCounter** %self.addr
  %t8 = load %TCounter*, %TCounter** %self.addr
  %t9 = getelementptr %TCounter, %TCounter* %t8, i32 0, i32 0
  %t10 = load i32, i32* %t9
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t10)
  ret void
}
define i32 @main() {
entry:
  %t11 = call i8* @malloc(i64 4)
  %t12 = bitcast i8* %t11 to %TCounter*
  call void @TCounter_Init(%TCounter* %t12)
  store %TCounter* %t12, %TCounter** @Counter
  %t13 = load %TCounter*, %TCounter** @Counter
  call void @TCounter_AddOne(%TCounter* %t13)
  %t14 = load %TCounter*, %TCounter** @Counter
  call void @TCounter_PrintCount(%TCounter* %t14)
  ret i32 0
}
