PROGRAM TestForBreak;
VAR i, total: Integer;
BEGIN
  for i := 1 to 10 do
    BEGIN
      IF i = 5 THEN
        BREAK;
      WriteLn(i);
    END;
END.