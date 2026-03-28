PROGRAM TestForContinue;
VAR i, total: Integer;
BEGIN
  for i := 1 to 10 do
    BEGIN
      IF i = 5 THEN
        CONTINUE;
      WriteLn(i);
    END;
END.
