PROGRAM DestructorTest;
TYPE
  TDemo = class
  PUBLIC
    CONSTRUCTOR Create;
    DESTRUCTOR Destroy;
  end;

CONSTRUCTOR TDemo.Create;
BEGIN
  WriteLn('Constructor called');
END;

DESTRUCTOR TDemo.Destroy;
BEGIN
  WriteLn('Destructor called');
END;

PROCEDURE TDemo.Test;
BEGIN
  WriteLn('Testing...');
END;

VAR
  obj: TDemo;
BEGIN
  obj := TDemo.Create();
  obj.Test();
  obj.Destroy();
END.