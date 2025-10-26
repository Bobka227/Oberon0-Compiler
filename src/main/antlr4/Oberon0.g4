grammar Oberon0;
@header { package app.parser; }


module
    : MODULE ID SEMI declarations? BEGIN statements? END ID DOT EOF
    ;

declarations : vardecl* procdecl_list? ;
vardecl        : VAR vardecl_list ;
vardecl_list   : idlist COLON vartype SEMI (idlist COLON vartype SEMI)* ;
idlist         : ID (COMMA ID)* ;

vartype        : basetype
    | arraytype
    ;
basetype       : BOOLEAN | INTEGER | REAL | STRING ;
arraytype      : ARRAY LBRACK dim_list RBRACK OF vartype ;
dim_list       : INTEGER_LITERAL (COMMA INTEGER_LITERAL)* ;

procdecl_list  : procdecl+ ;
procdecl       : procheader procbody ;
procheader     : PROCEDURE ID formalpars SEMI
    | FUNCTION  ID formalpars COLON vartype SEMI ;
procbody
    : vardecl? procdecl_list? BEGIN statements? END ID SEMI
    ;


formalpars     : LPAREN fpsection_list? RPAREN ;
fpsection_list : fpsection (SEMI fpsection)* ;
fpsection      : idlist COLON vartype ;

statements : statement (SEMI statement)* SEMI? ;

statement
    : assignment
    | conditional
    | repetition
    | proccall
    | io_statement
    | CONTINUE
    | BREAK
    | RETURN expression?
    ;


assignment     : variable ASSIGN expression ;

conditional    : IF expression THEN statements?
    (ELSEIF expression THEN statements?)*
    (ELSE statements?)?
    END ;

repetition
    : WHILE  expression DO statements? END
    | REPEAT statements? UNTIL expression
    | FOR ID ASSIGN expression TO expression DO statements? END
    ;

io_statement
    : WRITE   LPAREN expression_list? RPAREN
    | WRITELN (LPAREN expression_list? RPAREN)?
    | READ    LPAREN expression_list? RPAREN
    ;


variable : ID (LBRACK expression_list RBRACK)* ;
proccall       : ID actualpar ;
actualpar      : LPAREN expression_list? RPAREN ;
expression_list: expression (COMMA expression)* ;

expression        : logicOr ;
logicOr           : logicAnd (OR  logicAnd)* ;
logicAnd          : relation (AND relation)* ;
relation          : additive (relop additive)? ;
relop             : GT | LT | GE | LE | EQ | NE ;
additive          : multiplicative ((PLUS | MINUS) multiplicative)* ;
multiplicative    : unary ((STAR | SLASH | MOD) unary)* ;
unary             : (PLUS | MINUS | NOT)? primary ;
primary           : LPAREN expression RPAREN
    
    | proccall         
    | literal
    | variable          
    ;

literal           : BOOLEAN_LITERAL
    | INTEGER_LITERAL
    | REAL_LITERAL
    | STRING_LITERAL
    ;


MODULE  : 'module';   BEGIN:'begin'; END:'end';
PROCEDURE:'procedure'; FUNCTION:'function';
VAR:'var'; BOOLEAN:'boolean'; INTEGER:'integer'; REAL:'real'; STRING:'string';
ARRAY:'array'; OF:'of';

AND:'and'; OR:'or'; NOT:'not'; MOD:'mod';
CONTINUE:'continue'; BREAK:'break'; RETURN:'return';
IF:'if'; THEN:'then'; ELSEIF:'elseif'; ELSE:'else';
WHILE:'while'; DO:'do'; REPEAT:'repeat'; UNTIL:'until';
FOR:'for'; TO:'to';

WRITE:'write'; WRITELN:'writeln'; READ:'read';

ASSIGN:':='; LE:'<='; GE:'>='; COLON:':'; SEMI:';'; DOT:'.';
COMMA:','; PLUS:'+'; MINUS:'-'; STAR:'*'; SLASH:'/'; EQ:'=';
NE:'#'; LT:'<'; GT:'>'; LPAREN:'(' ; RPAREN:')';
LBRACK:'['; RBRACK:']';

BOOLEAN_LITERAL : 'TRUE' | 'FALSE';
INTEGER_LITERAL : DIGIT+ ;
REAL_LITERAL    : DIGIT+ '.' DIGIT+ (EXP)? | '.' DIGIT+ (EXP)? | DIGIT+ EXP ;
fragment EXP    : [eE] [+\-]? DIGIT+ ;

STRING_LITERAL  : '"' (~["\r\n])* '"' ;

ID              : (LETTER | '_') (LETTER | DIGIT | '_')* ;

WS      : [ \t\r\n]+       -> skip ;
COMMENT : '(*' .*? '*)'    -> skip ;

fragment DIGIT  : [0-9] ;
fragment LETTER : [A-Za-z] ;
