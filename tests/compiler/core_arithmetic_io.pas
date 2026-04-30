PROGRAM CoreArithmeticIoTest;
VAR
  x: Integer;
  y: Real;
  b: Boolean;
BEGIN
  x := 3 + 4 * 2;
  y := x + 1.5;
  b := x > 5;
  WriteLn(x);
  WriteLn(y);
  WriteLn(b);
END.
