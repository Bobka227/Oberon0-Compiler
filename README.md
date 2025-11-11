# Oberon-0 Kompilátor (Java + ANTLR4)

Plnohodnotný kompilátor podmnožiny **Oberonu-0**.  

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

##  Generování tříd ANTLR

V projektu musí být přítomné vygenerované třídy ANTLR (lexer, parser atd.).

V IDE (např. IntelliJ IDEA s ANTLR pluginem) lze použít tlačítko:

![Generate ANTLR classes](https://github.com/user-attachments/assets/ff8f2e86-5c4e-4373-8d13-df9496afd7ca)

Po úspěšném vygenerování to vypadá například takto:

![Generated ANTLR classes](https://github.com/user-attachments/assets/b5720b1d-5348-402c-bc56-9403fc4f2df1)




## Spuštění
### PowerShell/Bash
### Frontend režimy (bez kompilace do C)

Tyto režimy nevyžadují GCC/Clang – stačí Java a Maven.

**Základní syntaxe:**
```bash
mvn -q exec:java "-Dexec.args=<soubor.ob0> <přepínač>"
```

**Parametry:**
- `mvn` – spuštění Maven buildu
- `-q` – tichý režim (zobrazí jen výstup aplikace)
- `exec:java` – spustí hlavní třídu `app.Oberon0Compiler`
- `-Dexec.args=...` – argumenty předané do `main()`

**Přepínače:**

**1. Vizualizace AST jako stromu**
```bash
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0 --print-ast-tree"
```

<img width="1920" height="1080" alt="AST tree visualization" src="https://github.com/user-attachments/assets/bc30d8ac-6252-4967-ac6b-12d6d3c458d8" />

**2. Čitelný výpis AST**
```bash
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0 --print-ast"
```

<img width="1920" height="1080" alt="AST pretty print" src="https://github.com/user-attachments/assets/371be80b-4838-4364-b323-dcf14473eaba" />

**3. Pouze frontend s sémantikou**
```bash
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0 --frontend-only"
```

<img width="1920" height="1080" alt="Frontend only mode" src="https://github.com/user-attachments/assets/34617f2e-34be-4361-94ab-2b329a2dcacc" />




## Instalace backendu na Windows

Pro plnou funkčnost kompilátoru (generování C → překlad → spuštění) je potřeba nainstalovat GCC/Clang. Na Windows doporučujeme MSYS2 s UCRT64 prostředím.

### Krok 1: Instalace MSYS2

1. Stáhni instalátor z [msys2.org](https://msys2.org)
2. Nainstaluj do výchozí cesty `C:\msys64`
3. Otevři **MSYS2 MSYS** terminál a aktualizuj systém:

```bash
pacman -Syu
```

> [!TIP]
> Pokud MSYS požádá o restart, zavři okno a spusť jej znovu, pak opakuj `pacman -Syu`

### Krok 2: Instalace GCC v UCRT64

Otevři **MSYS2 UCRT64** terminál (důležité - ne základní MSYS!) a nainstaluj kompilátor:

```bash
# Instalace GCC
pacman -S mingw-w64-ucrt-x86_64-gcc

# Volitelně: debugging nástroje
pacman -S mingw-w64-ucrt-x86_64-gdb mingw-w64-ucrt-x86_64-make
```

**Ověření instalace:**

```bash
gcc --version
which gcc  # mělo by vrátit: /ucrt64/bin/gcc
```

### Krok 3: Přidání do PATH

Aby Maven a IDE našly kompilátor, přidej cestu do systémových proměnných:

| Krok | Akce |
|------|------|
| 1️ | Otevři **Ovládací panely** → Systém → Upřesnit nastavení systému |
| 2️ | Klikni na **Proměnné prostředí...** |
| 3️ | V **Systémových proměnných** vyber `Path` → **Upravit** |
| 4️ | Klikni **Nový** a přidej: `C:\msys64\ucrt64\bin` |
| 5️ | Potvrď změny a **restartuj** PowerShell/IDE |


### Krok 4: Spuštění s backendem

Po instalaci můžeš spustit plnou kompilaci:

```bash
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0"
```

**Pokročilé možnosti:**

```bash
# Pouze vygenerovat C kód bez kompilace
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0 --emit-c output.c --no-run"

# Vygenerovat C a přeložit, ale nespouštět
mvn -q exec:java "-Dexec.args=examples/ok_minimal.ob0 --no-run"
```
