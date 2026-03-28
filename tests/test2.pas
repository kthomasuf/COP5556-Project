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
      PROCEDURE ShowStats;
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
  WriteLn('Increasing Mana by Amount...');
  Mana := Mana + Amount;
END;

PROCEDURE THero.ShowStats;
BEGIN
  WriteLn('--- Hero Stats ---');
  WriteLn('Final Health: ');
  WriteLn(Health);
  
  WriteLn('Final Experience: ');
  WriteLn(Experience);
  
  WriteLn('Final Mana: ');
  WriteLn(Mana);
  WriteLn('------------------');
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
   
  Player.ShowStats();

  WriteLn('Random variable value: ');
  WriteLn(Fizz);
END.