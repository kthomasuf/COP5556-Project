PROGRAM ArithmeticTest;
VAR 
  a, b, c, d, e: Integer;
BEGIN
  a := 1;
  b := 0;
  
  c := a + b;
  WriteLn(c);
  
  c := b - b;
  WriteLn(d);
  
  c := 5 OR 3;
  WriteLn(c);

  d := 20;
  e := 5;

  a := d * e;
  WriteLn(a);

  a := d / e;
  WriteLn(a);

  a := d DIV e;
  WriteLn(a);

  a := d MOD e;
  WriteLn(a);

  c := 5 AND 3;
  WriteLn(c);
END.