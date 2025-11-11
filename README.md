# Oberon-0 Kompilátor (Java + ANTLR4)

Plnohodnotný kompilátor podmnožiny **Oberonu-0**.  
Celý řetězec: **lexikální analýza → parser → AST → sémantika → generování C → překlad a běh**.

Součástí je i výpis AST a ASCII strom AST, takže lze snadno použít jen „frontend“ část kompilátoru (lex + syntaktická + sémantická analýza) bez generování kódu.

---

##  Hlavní vlastnosti

###  Jazykové konstrukce

- Datové typy:
  - `integer`
  - `real`
  - `string`
  - `boolean`
  - pole (včetně vícerozměrných)

- Struktura programu:
  - globální proměnné
  - procedury a funkce
  - vnořené deklarace
  - podpora rekurze

###  Řídicí struktury

- `IF / ELSIF / ELSE`
- `WHILE`
- `REPEAT … UNTIL`
- `FOR`

###  Výrazy

- Aritmetika: `+`, `-`, `*`, `/`, `mod`
- Relační operátory: `=`, `#`, `<`, `<=`, `>`, `>=`
- Logické operátory: `and`, `or`, `not`  
  → se **správnou precedencí a asociativitou**

###  Vstup / výstup

- `write(expr)`
- `writeln(expr)`
- `read(var)`

---

##  Pipeline kompilátoru

1. **Lexikální analýza** (ANTLR4 lexer)
2. **Parser** (ANTLR4 parser)
3. **AST** – konstrukce abstraktního syntaktického stromu
4. **Sémantická analýza** – kontroly typů, deklarací, rozsahu identifikátorů atd.
5. **Generování C** – překlad do mezijazyka v C
6. **Překlad a spuštění** – volání GCC/Clang a běh výsledného programu

Pro účely demonstrace „frontend“ části lze kompilátor spustit tak, aby:
- pouze vygeneroval a vypsal **AST**
- nebo zobrazil **ASCII strom** v konzoli

---

##  Požadavky

- **Java 17+**
- **Maven 3.8+**
- **GCC nebo Clang v PATH**
  - Windows: doporučeno přes **MSYS** nebo podobné prostředí
- **ANTLR4** (v projektu musí být vygenerované třídy)

---

## 🐉 Generování tříd ANTLR

V projektu musí být přítomné vygenerované třídy ANTLR (lexer, parser atd.).

V IDE (např. IntelliJ IDEA s ANTLR pluginem) lze použít tlačítko:

![Generate ANTLR classes](https://github.com/user-attachments/assets/ff8f2e86-5c4e-4373-8d13-df9496afd7ca)

Po úspěšném vygenerování to vypadá například takto:

![Generated ANTLR classes](https://github.com/user-attachments/assets/b5720b1d-5348-402c-bc56-9403fc4f2df1)
