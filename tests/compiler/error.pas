PROGRAM ClassMethodsTest;

TYPE
  TBankAccount = CLASS
    PUBLIC
      Balance : INTEGER;
      CONSTRUCTOR Init;
      PROCEDURE Deposit(Amount : INTEGER);
      FUNCTION Withdraw(Amount : INTEGER) : INTEGER;
  END;

VAR
  MyAccount : TBankAccount;
  UserInput : INTEGER;
  Result : Integer;

CONSTRUCTOR TBankAccount.Init;
BEGIN
  Balance := 100;
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
  Result := MyAccount.Withdraw(UserInput);
END.