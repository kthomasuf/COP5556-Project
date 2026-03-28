PROGRAM ProcedureStaticScopeTest;
VAR x: INTEGER;

PROCEDURE Test;
BEGIN
  WriteLn(x);
END;

PROCEDURE Caller;
VAR x: INTEGER;
BEGIN
  x := 99;
  Test();
END;

BEGIN
  x := 5;
  Caller();
END.