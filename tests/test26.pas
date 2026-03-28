PROGRAM TestWhileBoundaries;
VAR
  i, hits: INTEGER;
BEGIN
  hits := 0;
  i := 0;

  while i < 0 do
    hits := hits + 100;

  while i = 0 do
  begin
    hits := hits + 1;
    i := i + 1
  end;

  while i < 3 do
  begin
    hits := hits + 10;
    i := i + 1
  end;

  while i > 10 do
    hits := hits + 100;

  WriteLn(hits);
END.
