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
  %t0 = add i32 3, 4
  store i32 %t0, i32* @x
  ret i32 0
}
