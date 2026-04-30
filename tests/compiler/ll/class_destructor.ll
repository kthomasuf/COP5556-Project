declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%TDemo = type {  }
@obj = global %TDemo* null

define void @TDemo_Create(%TDemo* %self) {
entry:
  %self.addr = alloca %TDemo*
  store %TDemo* %self, %TDemo** %self.addr
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 1)
  ret void
}

define void @TDemo_Destroy(%TDemo* %self) {
entry:
  %self.addr = alloca %TDemo*
  store %TDemo* %self, %TDemo** %self.addr
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 2)
  %t0 = load %TDemo*, %TDemo** %self.addr
  %t1 = bitcast %TDemo* %t0 to i8*
  call void @free(i8* %t1)
  ret void
}
define i32 @main() {
entry:
  %t2 = call i8* @malloc(i64 1)
  %t3 = bitcast i8* %t2 to %TDemo*
  call void @TDemo_Create(%TDemo* %t3)
  store %TDemo* %t3, %TDemo** @obj
  %t4 = load %TDemo*, %TDemo** @obj
  call void @TDemo_Destroy(%TDemo* %t4)
  ret i32 0
}
