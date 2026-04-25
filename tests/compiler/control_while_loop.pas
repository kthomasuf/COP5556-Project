PROGRAM ControlWhileLoopTest;
VAR
  i: Integer;
  total: Integer;
BEGIN
  i := 1;
  total := 0;
  WHILE i <= 4 DO
  BEGIN
    total := total + i;
    i := i + 1;
  END;
  WriteLn(total);
END.
