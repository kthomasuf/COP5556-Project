PROGRAM TestNestedLoops;
VAR
  i, j, total: INTEGER;
BEGIN
  total := 0;

  for i := 1 to 3 do
  begin
    j := 0;

    while j < 5 do
    begin
      j := j + 1;

      IF j = 2 THEN
        CONTINUE;

      IF j = 4 THEN
        BREAK;

      total := total + (i * 10 + j)
    end
  end;

  WriteLn(total);
END.
