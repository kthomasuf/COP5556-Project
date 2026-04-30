PROGRAM ClassConstructorTest;

TYPE
  THero = CLASS
    PUBLIC
      CONSTRUCTOR Init;  
      PROCEDURE LevelWorld;    
  END;

VAR
  Player : THero;
  WorldLevel : INTEGER;

CONSTRUCTOR THero.Init;
BEGIN
  WorldLevel := 1;
END;

PROCEDURE THero.LevelWorld;
BEGIN
  WorldLevel := WorldLevel + 1;
END;

BEGIN
  Player := THero.Init(); 
  Player.LevelWorld();
  WriteLn(WorldLevel);
END.
