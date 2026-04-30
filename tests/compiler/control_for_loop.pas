PROGRAM ControlForLoopTest;
VAR
  i: Integer;
  total: Integer;
BEGIN
  total := 0;
  FOR i := 1 TO 4 DO
  BEGIN
    total := total + i;
  END;
  WriteLn(total);
END.
