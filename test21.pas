PROGRAM FunctionStaticScopeTest;
VAR x: INTEGER;

FUNCTION f: INTEGER;
BEGIN
  f := x;
END;

FUNCTION g: INTEGER;
VAR x: INTEGER;
BEGIN
  x := 20;
  g := f;
END;

BEGIN
  x := 10;
  WriteLn(g);
END.

{Should print 10 not 20}