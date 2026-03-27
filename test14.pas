PROGRAM TestWhile;
VAR i, sum: Integer;
BEGIN
  i := 1;
  sum := 0;

  while i <= 5 do
  begin
    sum := sum + i;
    i := i + 1
  end;

  WriteLn(sum);

  i := 10;
  while i > 0 do
  begin
    if i = 5 then
      WriteLn('halfway');
    i := i - 2
  end;

  WriteLn(i)
END.
