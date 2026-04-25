PROGRAM ClassPublicFieldTest;
TYPE
  Person = CLASS
  PUBLIC
    age: Integer;
  END;
VAR
  p: Person;
BEGIN
  p := Person.Create();
  p.age := 42;
  WriteLn(p.age);
END.
