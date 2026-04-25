PROGRAM ClassInheritedFieldsTest;
TYPE
  Base = CLASS
  PUBLIC
    a: Integer;
  END;
  Child = CLASS(Base)
  PUBLIC
    b: Integer;
  END;
VAR
  c: Child;
BEGIN
  c := Child.Create();
  c.a := 5;
  c.b := 7;
  WriteLn(c.a + c.b);
END.
