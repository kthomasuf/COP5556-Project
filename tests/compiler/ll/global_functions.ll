declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
@x = global i32 0
@result = global i32 0

define i32 @Add(i32 %amount) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %amount.addr = alloca i32
  store i32 %amount, i32* %amount.addr
  %t0 = load i32, i32* @x
  %t1 = load i32, i32* %amount.addr
  %t2 = add i32 %t0, %t1
  store i32 %t2, i32* @x
  %t3 = load i32, i32* @x
  store i32 %t3, i32* %result.addr
  %t4 = load i32, i32* %result.addr
  ret i32 %t4
}

define i32 @Sub(i32 %amount) {
entry:
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %amount.addr = alloca i32
  store i32 %amount, i32* %amount.addr
  %t5 = load i32, i32* @x
  %t6 = load i32, i32* %amount.addr
  %t7 = sub i32 %t5, %t6
  store i32 %t7, i32* @x
  %t8 = load i32, i32* @x
  store i32 %t8, i32* %result.addr
  %t9 = load i32, i32* %result.addr
  ret i32 %t9
}
define i32 @main() {
entry:
  store i32 0, i32* @x
  %t10 = call i32 @Add(i32 10)
  store i32 %t10, i32* @result
  %t11 = load i32, i32* @result
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t11)
  %t12 = call i32 @Sub(i32 5)
  store i32 %t12, i32* @result
  %t13 = load i32, i32* @result
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t13)
  ret i32 0
}
