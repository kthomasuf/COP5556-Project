PROGRAM TestIfElse;
VAR x, a, b: Integer;
BEGIN
  x := 10;

  { simple if-then }
  if x = 10 then
    WriteLn('x is 10');

  { if-then-else }
  a := 20;
  b := 5;
  if a < b then
    WriteLn('b is greater')
  else
    WriteLn('a is greater');

  { nested if }
  if x > 0 then
  begin
    if x > 5 then
      WriteLn('nested: x is positive and big')
    else
      WriteLn('nested: x is positive but small')
  end
  else
    WriteLn('nested: x is not positive');

  WriteLn('done')
END.
