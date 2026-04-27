PROGRAM ClassVisbilityTestPass2;

TYPE
  TSecretAgent = CLASS
    PRIVATE
      FUNCTION Spying : INTEGER; 
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE DoMission;
  END;

VAR
  Bond : TSecretAgent;
  Result: INTEGER;

CONSTRUCTOR TSecretAgent.Init;
BEGIN
  Result := 0;
END;

FUNCTION TSecretAgent.Spying(Time : INTEGER) : INTEGER;
BEGIN
  Spying := Time;
END;

PROCEDURE TSecretAgent.DoMission;
BEGIN
  Result := Spying(5);
END;

BEGIN
  Bond := TSecretAgent.Init();
  Bond.DoMission();
END.