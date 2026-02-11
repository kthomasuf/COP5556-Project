PROGRAM TestDelphi;
TYPE
  THero = CLASS
    PRIVATE
      Health : INTEGER;
    PUBLIC
      CONSTRUCTOR Init;
  END;

VAR
  Player : THero;

BEGIN
  Player := THero.Init();
END.