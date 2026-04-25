PROGRAM ControlBreakContinueTest;
VAR
  i: Integer;
  total: Integer;
BEGIN
  total := 0;
  FOR i := 1 TO 6 DO
  BEGIN
    IF i = 2 THEN
      CONTINUE;
    IF i = 5 THEN
      BREAK;
    total := total + i;
  END;
  WriteLn(total);
END.
