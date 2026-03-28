PROGRAM TestWhileBreak;
VAR i, sum: Integer;
BEGIN
  i := 1;

  while i <= 10 do
  begin
    IF i = 5 THEN
      BREAK;
    WriteLn(i);
    i := i + 1
  end;
END.
