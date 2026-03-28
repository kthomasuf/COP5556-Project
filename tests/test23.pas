PROGRAM TestForBoundaries;
VAR
  i, hits: INTEGER;
BEGIN
  hits := 0;

  for i := 5 to 1 do
    hits := hits + 100;

  for i := 1 downto 5 do
    hits := hits + 100;

  for i := 3 to 3 do
    hits := hits + 1;

  for i := 7 downto 7 do
    hits := hits + 10;

  WriteLn(hits);
END.
