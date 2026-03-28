PROGRAM TestAll;

TYPE
  TEntity = CLASS
    PUBLIC
      CONSTRUCTOR Init(EntityID : INTEGER);
  END;

  TPlayer = CLASS (TEntity)
    PRIVATE
      Name : STRING;
      Health : REAL;
    PUBLIC
      IsAlive : BOOLEAN; 
      CONSTRUCTOR Create(PlayerName : STRING; StartingHealth : REAL);
      PROCEDURE TakeDamage(Amount : REAL);
      FUNCTION GetStatus : STRING;
  END;

VAR
  Hero : TPlayer;
  CalculatedDamage : REAL;
  Greeting : STRING;
  CheckBool : BOOLEAN;

CONSTRUCTOR TEntity.Init(EntityID : INTEGER);
BEGIN
  WriteLn('--> [Parent] TEntity initialized. Auto-filled ID:');
  WriteLn(EntityID);
END;

CONSTRUCTOR TPlayer.Create(PlayerName : STRING; StartingHealth : REAL);
BEGIN
  WriteLn('--> [Child] TPlayer Created.');
  Name := PlayerName;
  Health := StartingHealth;
  IsAlive := TRUE;
END;

PROCEDURE TPlayer.TakeDamage(Amount : REAL);
BEGIN
  WriteLn('Taking damage...');
  Health := Health - Amount;

  IsAlive := (Health > 0.0); 
END;

FUNCTION TPlayer.GetStatus : STRING;
BEGIN
  GetStatus := 'Status: ' + Name + ' is still standing!';
END;

FUNCTION CalculateMultiplier(BaseValue : REAL; Multiplier : REAL) : REAL;
BEGIN
  CalculateMultiplier := BaseValue * Multiplier;
END;


BEGIN
  WriteLn('=== 1. TESTING CONSTRUCTORS & MAP PARAMS ===');
  Hero := TPlayer.Create('Shriyans', 100.5);

  WriteLn('');
  WriteLn('=== 2. TESTING STRINGS ===');
  Greeting := 'Welcome to ' + 'the ' + 'Arena!';
  WriteLn(Greeting);

  WriteLn('');
  WriteLn('=== 3. TESTING REALS & GLOBAL FUNCTIONS ===');
  CalculatedDamage := CalculateMultiplier(12.5, 2.0); 
  WriteLn('Calculated Damage:');
  WriteLn(CalculatedDamage);

  WriteLn('');
  WriteLn('=== 4. TESTING METHOD PROCEDURES ===');
  Hero.TakeDamage(CalculatedDamage);
  WriteLn('Damage taken successfully.');

  WriteLn('');
  WriteLn('=== 5. TESTING METHOD FUNCTIONS ===');
  WriteLn(Hero.GetStatus());

  WriteLn('');
  WriteLn('=== 6. TESTING BOOLEANS ===');
  WriteLn('Is the Hero alive? (Should be true)');
  WriteLn(Hero.IsAlive);

  CheckBool := TRUE AND FALSE;
  WriteLn('Testing TRUE AND FALSE (Should be false):');
  WriteLn(CheckBool);

  WriteLn('');
  WriteLn('Testing Complete!');
END.