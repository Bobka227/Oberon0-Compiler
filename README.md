Oberon-0 Kompilátor (Java + ANTLR4)

Plnohodnotný kompilátor podmnožiny Oberonu-0.
Řetězec: lexikální analýza → parser → AST → sémantika → generování C → překlad a běh.
Obsahuje výpis AST i ASCII strom AST pro čisté „frontend“ demo.


✨ Hlavní vlastnosti
Jazyk: integer, real, string, boolean, pole (i vícerozměrná)

Struktura: globální proměnné, procedury a funkce, vnořené deklarace, rekurze

Řídicí struktury: if / elseif / else, while, repeat…until, for

Výrazy: + - * / mod, relace = # < <= > >=, logika and or not (správná precedence)

I/O: write, writeln, read


Požadavky

Java 17+

Maven 3.8+

GCC nebo Clang v PATH (Windows: MSYS)

V projektu musí být vygenerované třídy ANTLR, to mužete udělat pomoci tlačitka:
<img width="362" height="116" alt="image" src="https://github.com/user-attachments/assets/ff8f2e86-5c4e-4373-8d13-df9496afd7ca" />

Vzpadá to takhle:
<img width="295" height="299" alt="image" src="https://github.com/user-attachments/assets/b5720b1d-5348-402c-bc56-9403fc4f2df1" />



