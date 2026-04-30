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
  store i32 0, i32* @total
  store i32 1, i32* @i
  br label %for.cond.0
for.cond.0:
  %t0 = load i32, i32* @i
  %t1 = icmp sle i32 %t0, 6
  br i1 %t1, label %for.body.1, label %for.end.3
for.body.1:
  %t2 = load i32, i32* @i
  %t3 = icmp eq i32 %t2, 2
  br i1 %t3, label %if.then.4, label %if.end.5
if.then.4:
  br label %for.update.2
if.end.5:
  %t4 = load i32, i32* @i
  %t5 = icmp eq i32 %t4, 5
  br i1 %t5, label %if.then.6, label %if.end.7
if.then.6:
  br label %for.end.3
if.end.7:
  %t6 = load i32, i32* @total
  %t7 = load i32, i32* @i
  %t8 = add i32 %t6, %t7
  store i32 %t8, i32* @total
  br label %for.update.2
for.update.2:
  %t9 = load i32, i32* @i
  %t10 = add i32 %t9, 1
  store i32 %t10, i32* @i
  br label %for.cond.0
for.end.3:
  %t11 = load i32, i32* @total
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t11)
  ret i32 0
}
