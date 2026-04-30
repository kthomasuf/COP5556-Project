declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%Person = type { i32 }
@p = global %Person* null
define i32 @main() {
entry:
  %t0 = call i8* @malloc(i64 4)
  %t1 = bitcast i8* %t0 to %Person*
  store %Person* %t1, %Person** @p
  %t2 = load %Person*, %Person** @p
  %t3 = getelementptr %Person, %Person* %t2, i32 0, i32 0
  store i32 42, i32* %t3
  %t4 = load %Person*, %Person** @p
  %t5 = getelementptr %Person, %Person* %t4, i32 0, i32 0
  %t6 = load i32, i32* %t5
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t6)
  ret i32 0
}
