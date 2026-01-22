# Forth-compiler
Forth → Assembly Compiler
## Overview

This project is to **create a program(compiler) that translates a Forth program into x86-64 assembly, assembles it with `as`, links it with `ld`**, and then manually runs the resulting executable.

A Forth source file is given:

- `code.fs` – contains the Forth program.

The **program's(compiler's)** job is to:

- read the contents of `code.fs` file character by character.
- create `code.s` – an x86-64 assembly translation of `code.fs`.

**It - the program** will then assemble and link the code.s file:

```bash
as -o code.o code.s
ld -o code code.o -lc -dynamic-linker /lib64/ld-linux-x86-64.so.2
````
Only then you can manually run the executable like:

```bash
./code
````

The assembly program uses the **real CPU stack as the Forth data stack**.

---
