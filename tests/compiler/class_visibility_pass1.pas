PROGRAM ClassVisbilityTestPass1;

TYPE
  TSecretAgent = CLASS
    PRIVATE
      PROCEDURE BurnAfterReading;
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE DoMission;
  END;

VAR
  Bond : TSecretAgent;
  Note: INTEGER;

CONSTRUCTOR TSecretAgent.Init;
BEGIN
  Note := 1;
END;

PROCEDURE TSecretAgent.BurnAfterReading;
BEGIN
  WriteLn(0);
END;

PROCEDURE TSecretAgent.DoMission;
BEGIN
  BurnAfterReading();
END;

BEGIN
  Bond := TSecretAgent.Init();
  Bond.DoMission();
  WriteLn(1);
END.