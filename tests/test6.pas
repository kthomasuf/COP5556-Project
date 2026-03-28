{Code will crash due to line 55}

PROGRAM TestFeatures;

TYPE
  TBank = CLASS
    PRIVATE
      Balance : INTEGER;      
      SecretCode : INTEGER;   
    PUBLIC
      CONSTRUCTOR Create;
      PROCEDURE Deposit(Amount : INTEGER);  
      PROCEDURE ShowBalance;
      PROCEDURE HackAttempt;
  END;

VAR
  MyAccount : TBank;

CONSTRUCTOR TBank.Create;
BEGIN
  WriteLn('Creating Bank Account...');
  Balance := 0;
  SecretCode := 1234;
END;

PROCEDURE TBank.Deposit(Amount : INTEGER);
BEGIN
  WriteLn('Depositing money...');
  Balance := Balance + Amount;
END;

PROCEDURE TBank.ShowBalance;
BEGIN
  WriteLn('Current Balance is:');
  WriteLn(Balance);
END;

PROCEDURE TBank.HackAttempt;
BEGIN
  WriteLn('Checking Secret Code internally...');
  WriteLn(SecretCode); { Allowed: We are inside TBank }
END;

BEGIN
  MyAccount := TBank.Create();

  WriteLn('--- Testing Parameters ---');
  MyAccount.Deposit(500); 
  MyAccount.ShowBalance();

  WriteLn('--- Testing Internal Private Access ---');
  MyAccount.HackAttempt();
  
  MyAccount.Balance := 1000000;  
  
  WriteLn('Test Complete.');
END.