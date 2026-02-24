PROGRAM TestInhertianceEncapsulation;
TYPE
  TAnimal = CLASS
  PUBLIC
    Health : INTEGER;
    CONSTRUCTOR Create;
    PROCEDURE ShowStats;
  PRIVATE
    Secret : INTEGER;
  END;
  
  TDog = CLASS(TAnimal)
  PUBLIC
    Breed : INTEGER;
    CONSTRUCTOR Create;
    PROCEDURE ShowSecret;
  END;

VAR labrador : TDog;

CONSTRUCTOR TAnimal.Create;
BEGIN
  Health := 100;
  Secret := 999;
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

PROCEDURE TDog.ShowSecret;
BEGIN
  WriteLn(Secret);
END;

BEGIN
  labrador := TDog.Create();
  labrador.ShowSecret();
END.