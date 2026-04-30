PROGRAM ClassPrivateFieldRejectedTest;
TYPE
  Person = CLASS
  PRIVATE
    age: Integer;
  PUBLIC
    height: Integer;
  END;
VAR
  p: Person;
BEGIN
  p := Person.Create();
  p.age := 42;
END.
