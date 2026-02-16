PROGRAM ComparisonTest;
VAR x, y, result: Integer;
BEGIN
  x := 10;
  y := 5;
  
  result := x = y;
  WriteLn(result);

  result := x <> y;
  WriteLn(result);

  result := x > y;
  WriteLn(result);
  
  result := x >= y;
  WriteLn(result);

  result := x < y;
  WriteLn(result);
  
  result := x <= y;
  WriteLn(result);
  
END.