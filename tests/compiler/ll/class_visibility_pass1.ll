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
@Note = global i32 0

define void @TSecretAgent_Init(%TSecretAgent* %self) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  store i32 1, i32* @Note
  ret void
}

define void @TSecretAgent_BurnAfterReading(%TSecretAgent* %self) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 0)
  ret void
}

define void @TSecretAgent_DoMission(%TSecretAgent* %self) {
entry:
  %self.addr = alloca %TSecretAgent*
  store %TSecretAgent* %self, %TSecretAgent** %self.addr
  %t0 = load %TSecretAgent*, %TSecretAgent** %self.addr
  call void @TSecretAgent_BurnAfterReading(%TSecretAgent* %t0)
  ret void
}
define i32 @main() {
entry:
  %t1 = call i8* @malloc(i64 1)
  %t2 = bitcast i8* %t1 to %TSecretAgent*
  call void @TSecretAgent_Init(%TSecretAgent* %t2)
  store %TSecretAgent* %t2, %TSecretAgent** @Bond
  %t3 = load %TSecretAgent*, %TSecretAgent** @Bond
  call void @TSecretAgent_DoMission(%TSecretAgent* %t3)
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 1)
  ret i32 0
}
