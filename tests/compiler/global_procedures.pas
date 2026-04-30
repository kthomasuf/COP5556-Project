PROGRAM GlobalProceduresTest;
VAR x: INTEGER;

PROCEDURE PrintX;
BEGIN
  WriteLn(x);
END;

PROCEDURE Caller;
BEGIN
  PrintX();
END;

BEGIN
  x := 5;
  Caller();
END.