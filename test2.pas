{Current interpreter cannot handle parameter passing, need to implement that still, so this code will not do anything meaningful}

PROGRAM TestOOP;

TYPE
  THero = CLASS
    PRIVATE
      Health : INTEGER;
      Experience : INTEGER;
      Mana : INTEGER;
    PUBLIC
      CONSTRUCTOR Init;  
      PROCEDURE Heal;    
      PROCEDURE LevelUp; 
      PROCEDURE IncreaseMana(Amount : INTEGER);
  END;

VAR
  Player : THero;
  Fizz : INTEGER;

CONSTRUCTOR THero.Init;
BEGIN
  WriteLn('Initializing Hero (Real Constructor)...');
  Health := 43; 
  Experience := 100;
  Mana := 10;
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

PROCEDURE THero.IncreaseMana(Amount : INTEGER);
BEGIN
  WriteLn('Increasing Mana...');
  Mana := Mana + Amount;
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
  
  Player.IncreaseMana(50);

  Random();
   
  WriteLn('Final Health: ');
  WriteLn(Player.Health);

  WriteLn('Final Experience: ');
  WriteLn(Player.Experience);

  WriteLn('Final Mana: ');
  WriteLn(Player.Mana);

  WriteLn('Random variable value: ');
  WriteLn(Fizz);
END.