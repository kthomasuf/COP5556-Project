declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
@x = global i32 0
@y = global float 0.0
@b = global i1 0
define i32 @main() {
entry:
  %t0 = mul i32 4, 2
  %t1 = add i32 3, %t0
  store i32 %t1, i32* @x
  %t2 = load i32, i32* @x
  %t3 = sitofp i32 %t2 to float
  %t4 = fadd float %t3, 1.5
  store float %t4, float* @y
  %t5 = load i32, i32* @x
  %t6 = icmp sgt i32 %t5, 5
  store i1 %t6, i1* @b
  %t7 = load i32, i32* @x
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t7)
  %t8 = load float, float* @y
  %t9 = fpext float %t8 to double
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.float, i32 0, i32 0), double %t9)
  %t10 = load i1, i1* @b
  %t11 = zext i1 %t10 to i32
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t11)
  ret i32 0
}
