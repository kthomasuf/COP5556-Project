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
  %t1 = icmp sle i32 %t0, 5
  br i1 %t1, label %for.body.1, label %for.end.3
for.body.1:
  %t2 = load i32, i32* @total
  %t3 = load i32, i32* @i
  %t4 = add i32 %t2, %t3
  store i32 %t4, i32* @total
  br label %for.update.2
for.update.2:
  %t5 = load i32, i32* @i
  %t6 = add i32 %t5, 1
  store i32 %t6, i32* @i
  br label %for.cond.0
for.end.3:
  %t7 = load i32, i32* @total
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t7)
  store i32 3, i32* @i
  br label %for.cond.4
for.cond.4:
  %t8 = load i32, i32* @i
  %t9 = icmp sge i32 %t8, 1
  br i1 %t9, label %for.body.5, label %for.end.7
for.body.5:
  %t10 = load i32, i32* @i
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t10)
  br label %for.update.6
for.update.6:
  %t11 = load i32, i32* @i
  %t12 = sub i32 %t11, 1
  store i32 %t12, i32* @i
  br label %for.cond.4
for.end.7:
  store i32 1, i32* @total
  store i32 1, i32* @i
  br label %for.cond.8
for.cond.8:
  %t13 = load i32, i32* @i
  %t14 = icmp sle i32 %t13, 4
  br i1 %t14, label %for.body.9, label %for.end.11
for.body.9:
  %t15 = load i32, i32* @total
  %t16 = mul i32 %t15, 2
  store i32 %t16, i32* @total
  br label %for.update.10
for.update.10:
  %t17 = load i32, i32* @i
  %t18 = add i32 %t17, 1
  store i32 %t18, i32* @i
  br label %for.cond.8
for.end.11:
  %t19 = load i32, i32* @total
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t19)
  ret i32 0
}
