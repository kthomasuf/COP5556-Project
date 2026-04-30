declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%TSecretAgent = type {  }
@Bond = global %TSecretAgent* null
@Result = global i32 0

define void @TSecretAgent_Init(%TSecretAgent* %self) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  store i32 0, i32* @Result
  ret void
}

define i32 @TSecretAgent_Spying(%TSecretAgent* %self, i32 %time) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %time.addr = alloca i32
  store i32 %time, i32* %time.addr
  %t0 = load i32, i32* %time.addr
  store i32 %t0, i32* %result.addr
  %t1 = load i32, i32* %result.addr
  ret i32 %t1
}

define void @TSecretAgent_DoMission(%TSecretAgent* %self) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  %t2 = load %TSecretAgent*, %TSecretAgent** %self.addr
  %t3 = call i32 @TSecretAgent_Spying(%TSecretAgent* %t2, i32 5)
  store i32 %t3, i32* @Result
  ret void
}
define i32 @main() {
entry:
  %t4 = call i8* @malloc(i64 1)
  %t5 = bitcast i8* %t4 to %TSecretAgent*
  call void @TSecretAgent_Init(%TSecretAgent* %t5)
  store %TSecretAgent* %t5, %TSecretAgent** @Bond
  %t6 = load %TSecretAgent*, %TSecretAgent** @Bond
  call void @TSecretAgent_DoMission(%TSecretAgent* %t6)
  %t7 = load i32, i32* @Result
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t7)
  ret i32 0
}
