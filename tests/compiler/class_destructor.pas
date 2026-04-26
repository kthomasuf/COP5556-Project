PROGRAM ClassDestructorTest;
TYPE
  TDemo = class
  PUBLIC
    CONSTRUCTOR Create;
    DESTRUCTOR Destroy;
  end;

CONSTRUCTOR TDemo.Create;
BEGIN
  WriteLn(1);
END;

DESTRUCTOR TDemo.Destroy;
BEGIN
  WriteLn(2);
END;

VAR
  obj: TDemo;
BEGIN
  obj := TDemo.Create();
  obj.Destroy();
END.