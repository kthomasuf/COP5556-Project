PROGRAM TestPrivateMethod;

TYPE
  TSecretAgent = CLASS
    PRIVATE
      CodeName : INTEGER;
      PROCEDURE BurnAfterReading;
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE DoMission;
  END;

VAR
  Bond : TSecretAgent;

CONSTRUCTOR TSecretAgent.Init;
BEGIN
  WriteLn('Agent Initialized.');
  CodeName := 007;
END;

PROCEDURE TSecretAgent.BurnAfterReading;
BEGIN
  WriteLn('>> TOP SECRET: Destroying Evidence...');
END;

PROCEDURE TSecretAgent.DoMission;
BEGIN
  WriteLn('Starting Mission...');
  BurnAfterReading(); 
  WriteLn('Mission Complete.');
END;

BEGIN
  Bond := TSecretAgent.Init();

  Bond.DoMission();

 {Code will crash here due to private method calling in the wrong scope}
  Bond.BurnAfterReading(); 

  WriteLn('End of Tape.');
END.