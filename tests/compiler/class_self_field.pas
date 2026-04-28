PROGRAM ClassSelfFieldTest;

TYPE
  TCounter = CLASS
    PRIVATE
      Count : INTEGER;
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE AddOne;
      PROCEDURE PrintCount;
  END;

VAR
  Counter : TCounter;

CONSTRUCTOR TCounter.Init;
BEGIN
  Self.Count := 5;
END;

PROCEDURE TCounter.AddOne;
BEGIN
  Self.Count := Self.Count + 1;
END;

PROCEDURE TCounter.PrintCount;
BEGIN
  WriteLn(Self.Count);
END;

BEGIN
  Counter := TCounter.Init();
  Counter.AddOne();
  Counter.PrintCount();
END.
