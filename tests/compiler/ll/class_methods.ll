declare i32 @printf(i8*, ...)
declare i32 @scanf(i8*, ...)
declare i8* @malloc(i64)
declare void @free(i8*)
@.fmt.int = private constant [4 x i8] c"%d\0A\00"
@.fmt.float = private constant [4 x i8] c"%f\0A\00"
@.scan.int = private constant [3 x i8] c"%d\00"
@.scan.float = private constant [3 x i8] c"%f\00"
%TBankAccount = type {  }
@MyAccount = global %TBankAccount* null
@UserInput = global i32 0
@Balance = global i32 0
@Cash = global i32 0

define void @TBankAccount_Init(%TBankAccount* %self) {
entry:
  %self.addr = alloca %TBankAccount*
  store %TBankAccount* %self, %TBankAccount** %self.addr
  store i32 100, i32* @Balance
  store i32 0, i32* @Cash
  ret void
}

define void @TBankAccount_Deposit(%TBankAccount* %self, i32 %amount) {
entry:
  %self.addr = alloca %TBankAccount*
  store %TBankAccount* %self, %TBankAccount** %self.addr
  %amount.addr = alloca i32
  store i32 %amount, i32* %amount.addr
  %t0 = load i32, i32* @Balance
  %t1 = load i32, i32* %amount.addr
  %t2 = add i32 %t0, %t1
  store i32 %t2, i32* @Balance
  ret void
}

define i32 @TBankAccount_Withdraw(%TBankAccount* %self, i32 %amount) {
entry:
  %self.addr = alloca %TBankAccount*
  store %TBankAccount* %self, %TBankAccount** %self.addr
  %result.addr = alloca i32
  store i32 0, i32* %result.addr
  %amount.addr = alloca i32
  store i32 %amount, i32* %amount.addr
  %t3 = load i32, i32* @Balance
  %t4 = load i32, i32* %amount.addr
  %t5 = sub i32 %t3, %t4
  store i32 %t5, i32* @Balance
  %t6 = load i32, i32* %amount.addr
  store i32 %t6, i32* %result.addr
  %t7 = load i32, i32* %result.addr
  ret i32 %t7
}
define i32 @main() {
entry:
  %t8 = call i8* @malloc(i64 1)
  %t9 = bitcast i8* %t8 to %TBankAccount*
  call void @TBankAccount_Init(%TBankAccount* %t9)
  store %TBankAccount* %t9, %TBankAccount** @MyAccount
  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.scan.int, i32 0, i32 0), i32* @UserInput)
  %t10 = load %TBankAccount*, %TBankAccount** @MyAccount
  %t11 = load i32, i32* @UserInput
  call void @TBankAccount_Deposit(%TBankAccount* %t10, i32 %t11)
  call i32 (i8*, ...) @scanf(i8* getelementptr ([3 x i8], [3 x i8]* @.scan.int, i32 0, i32 0), i32* @UserInput)
  %t12 = load %TBankAccount*, %TBankAccount** @MyAccount
  %t13 = load i32, i32* @UserInput
  %t14 = call i32 @TBankAccount_Withdraw(%TBankAccount* %t12, i32 %t13)
  store i32 %t14, i32* @Cash
  %t15 = load i32, i32* @Balance
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t15)
  %t16 = load i32, i32* @Cash
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.fmt.int, i32 0, i32 0), i32 %t16)
  ret i32 0
}
