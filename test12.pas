PROGRAM TestOOPInput;

TYPE
  TBankAccount = CLASS
    PRIVATE
      Balance : INTEGER;
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE Deposit(Amount : INTEGER);
      PROCEDURE ShowBalance;
  END;

VAR
  MyAccount : TBankAccount;
  UserInput : INTEGER;

CONSTRUCTOR TBankAccount.Init;
BEGIN
  Balance := 0;
END;

PROCEDURE TBankAccount.Deposit(Amount : INTEGER);
BEGIN
  Balance := Balance + Amount;
END;

PROCEDURE TBankAccount.ShowBalance;
BEGIN
  WriteLn('Your current balance is: ');
  WriteLn(Balance);
END;

BEGIN
  WriteLn('--- Bank Terminal ---');
  MyAccount := TBankAccount.Init();
  
  WriteLn('How much money would you like to deposit? (Enter an integer)');
  
  ReadLn(UserInput); 
  
  WriteLn('Depositing your money into the object...');
  MyAccount.Deposit(UserInput);
  
  MyAccount.ShowBalance();
END.