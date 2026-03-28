PROGRAM FunctionStaticScopeTest;
VAR x: INTEGER;

FUNCTION a: INTEGER;
BEGIN
  a := x;
END;

FUNCTION b: INTEGER;
VAR x: INTEGER;
BEGIN
  x := 20;
  b := a;
END;

BEGIN
  x := 10;
  WriteLn(b);
END.