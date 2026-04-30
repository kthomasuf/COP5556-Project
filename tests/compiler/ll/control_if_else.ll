declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
@x = global i32 0
define i32 @main() {
entry:
  store i32 7, i32* @x
  %t0 = load i32, i32* @x
  %t1 = icmp sgt i32 %t0, 5
  br i1 %t1, label %if.then.0, label %if.else.1
if.then.0:
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 1)
  br label %if.end.2
if.else.1:
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 0)
  br label %if.end.2
if.end.2:
  ret i32 0
}
