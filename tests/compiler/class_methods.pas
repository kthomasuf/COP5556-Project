PROGRAM ClassMethodsTest;

TYPE
  TBankAccount = CLASS
    PUBLIC
      CONSTRUCTOR Init;
      PROCEDURE Deposit(Amount : INTEGER);
      FUNCTION Withdraw(Amount : INTEGER) : INTEGER;
  END;

VAR
  MyAccount : TBankAccount;
  UserInput : INTEGER;
  Balance : INTEGER;
  Cash : INTEGER;

CONSTRUCTOR TBankAccount.Init;
BEGIN
  Balance := 100;
  Cash := 0;
END;

PROCEDURE TBankAccount.Deposit(Amount : INTEGER);
BEGIN
  Balance := Balance + Amount;
END;

FUNCTION TBankAccount.Withdraw(Amount : INTEGER) : INTEGER;
BEGIN
  Balance := Balance - Amount;
  Withdraw := Amount;
END;

BEGIN
  MyAccount := TBankAccount.Init();
  ReadLn(UserInput); 
  MyAccount.Deposit(UserInput);
  ReadLn(UserInput);
  Cash := MyAccount.Withdraw(UserInput);
END.