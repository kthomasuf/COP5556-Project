declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%Base = type { i32 }
%Child = type { i32, i32 }
@c = global %Child* null
define i32 @main() {
entry:
  %t0 = call i8* @malloc(i64 8)
  %t1 = bitcast i8* %t0 to %Child*
  store %Child* %t1, %Child** @c
  %t2 = load %Child*, %Child** @c
  %t3 = getelementptr %Child, %Child* %t2, i32 0, i32 0
  store i32 5, i32* %t3
  %t4 = load %Child*, %Child** @c
  %t5 = getelementptr %Child, %Child* %t4, i32 0, i32 1
  store i32 7, i32* %t5
  %t6 = load %Child*, %Child** @c
  %t7 = getelementptr %Child, %Child* %t6, i32 0, i32 0
  %t8 = load i32, i32* %t7
  %t9 = load %Child*, %Child** @c
  %t10 = getelementptr %Child, %Child* %t9, i32 0, i32 1
  %t11 = load i32, i32* %t10
  %t12 = add i32 %t8, %t11
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t12)
  ret i32 0
}
