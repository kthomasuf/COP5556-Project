@x = global i32 0
define i32 @main() {
entry:
  %0 = add i32 3, 4
  store i32 %0, i32* @x
  ret i32 0
}
