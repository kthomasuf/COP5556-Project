PROGRAM TestOOP;

TYPE
  THero = CLASS
    PRIVATE
      Health : INTEGER;
      Experience : INTEGER;
    PUBLIC
      CONSTRUCTOR Init;  { 1. Header inside class }
      PROCEDURE Heal;    { 2. Header inside class }
      PROCEDURE LevelUp; 
  END;

VAR
  Player : THero;
  Fizz : INTEGER;

CONSTRUCTOR THero.Init;
BEGIN
  WriteLn('Initializing Hero (Real Constructor)...');
  Health := 43; 
  Experience := 100;
END;

PROCEDURE THero.Heal;
BEGIN
  WriteLn('Healing Hero...');
  Health := Health + 27;
END;

PROCEDURE THero.LevelUp;
BEGIN 
  WriteLn('Leveling Up Hero...');
  Experience := Experience + 50;
END;

PROCEDURE Random;
BEGIN
  WriteLn('Not Tied To Any Class...');
END;

BEGIN
  Player := THero.Init(); 
  Fizz := 10;

  Player.Heal();
  Player.LevelUp();

  Random();
   
  WriteLn('Final Health: ');
  WriteLn(Player.Health);

  WriteLn('Final Experience: ');
  WriteLn(Player.Experience);

  WriteLn('Random variable value: ');
  WriteLn(Fizz);
END;