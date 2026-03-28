PROGRAM TestInheritance;
TYPE
  TAnimal = CLASS
  PUBLIC
    Health : INTEGER;
    CONSTRUCTOR Create;
    PROCEDURE ShowStats;
  END;
  
  TDog = CLASS(TAnimal)
  PUBLIC
    Breed : INTEGER;
    CONSTRUCTOR Create;
  END;

VAR labrador : TDog;

CONSTRUCTOR TAnimal.Create;
BEGIN
  Health := 100;
END;

CONSTRUCTOR TDog.Create;
BEGIN
  Breed := 1;
  Health := Health + 50;
END;

PROCEDURE TAnimal.ShowStats;
BEGIN
  WriteLn(Health);
END;

BEGIN
  labrador := TDog.Create();
  labrador.ShowStats();
END.