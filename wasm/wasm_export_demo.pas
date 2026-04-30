PROGRAM WasmExportDemo;

TYPE
  TCounter = CLASS
    PUBLIC
      Value : INTEGER;
      CONSTRUCTOR Init(Start : INTEGER);
      PROCEDURE Add(Step : INTEGER);
      FUNCTION Current : INTEGER;
  END;

VAR
  DemoCounter : TCounter;

CONSTRUCTOR TCounter.Init(Start : INTEGER);
BEGIN
  Self.Value := Start;
END;

PROCEDURE TCounter.Add(Step : INTEGER);
BEGIN
  Self.Value := Self.Value + Step;
END;

FUNCTION TCounter.Current : INTEGER;
BEGIN
  Current := Self.Value;
END;

FUNCTION AddOne(Value : INTEGER) : INTEGER;
BEGIN
  AddOne := Value + 1;
END;

FUNCTION MaxValue(Left : INTEGER; Right : INTEGER) : INTEGER;
BEGIN
  IF Left > Right THEN
    MaxValue := Left
  ELSE
    MaxValue := Right;
END;

FUNCTION SumToN(Limit : INTEGER) : INTEGER;
BEGIN
  SumToN := 0;
  WHILE Limit > 0 DO
  BEGIN
    SumToN := SumToN + Limit;
    Limit := Limit - 1;
  END;
END;

FUNCTION CounterDemo(Start : INTEGER; Step : INTEGER) : INTEGER;
BEGIN
  DemoCounter := TCounter.Init(Start);
  DemoCounter.Add(Step);
  CounterDemo := DemoCounter.Current();
END;

BEGIN
END.
