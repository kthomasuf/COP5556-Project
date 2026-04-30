declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
@i = global i32 0
@total = global i32 0
define i32 @main() {
entry:
  store i32 1, i32* @i
  store i32 0, i32* @total
  br label %while.cond.0
while.cond.0:
  %t0 = load i32, i32* @i
  %t1 = icmp sle i32 %t0, 4
  br i1 %t1, label %while.body.1, label %while.end.2
while.body.1:
  %t2 = load i32, i32* @total
  %t3 = load i32, i32* @i
  %t4 = add i32 %t2, %t3
  store i32 %t4, i32* @total
  %t5 = load i32, i32* @i
  %t6 = add i32 %t5, 1
  store i32 %t6, i32* @i
  br label %while.cond.0
while.end.2:
  %t7 = load i32, i32* @total
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t7)
  ret i32 0
}
