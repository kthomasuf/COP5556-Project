PROGRAM TestFor;
VAR i, total: Integer;
BEGIN
  total := 0;
  for i := 1 to 5 do
    total := total + i;
  WriteLn(total);

  for i := 3 downto 1 do
    WriteLn(i);

  total := 1;
  for i := 1 to 4 do
    total := total * 2;
  WriteLn(total)
END.
