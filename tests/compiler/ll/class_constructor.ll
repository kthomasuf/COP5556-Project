declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%THero = type {  }
@Player = global %THero* null
@WorldLevel = global i32 0

define void @THero_Init(%THero* %self) {
entry:
  %self.addr = alloca %THero*
  store %THero* %self, %THero** %self.addr
  store i32 1, i32* @WorldLevel
  ret void
}

define void @THero_LevelWorld(%THero* %self) {
entry:
  %self.addr = alloca %THero*
  store %THero* %self, %THero** %self.addr
  %t0 = load i32, i32* @WorldLevel
  %t1 = add i32 %t0, 1
  store i32 %t1, i32* @WorldLevel
  ret void
}
define i32 @main() {
entry:
  %t2 = call i8* @malloc(i64 1)
  %t3 = bitcast i8* %t2 to %THero*
  call void @THero_Init(%THero* %t3)
  store %THero* %t3, %THero** @Player
  %t4 = load %THero*, %THero** @Player
  call void @THero_LevelWorld(%THero* %t4)
  %t5 = load i32, i32* @WorldLevel
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t5)
  ret i32 0
}
