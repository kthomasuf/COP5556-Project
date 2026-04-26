PROGRAM GlobalFunctionsTest;
VAR 
  x: INTEGER;
  result: INTEGER;

FUNCTION Add(amount: INTEGER) : INTEGER;
BEGIN
  x := x + amount;
  Add := x;
END;

FUNCTION Sub(amount: INTEGER) : INTEGER;
BEGIN
  x := x - amount;
  Sub := x;
END;

BEGIN
  x := 0;
  result := Add(10);
  WriteLn(result);
  result := Sub(5);
  WriteLn(result);
END.