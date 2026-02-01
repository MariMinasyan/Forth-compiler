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

## Forth Words

The Forth program (and thus your translation) will use only the following words:

* `+`
* `dup`
* `.`
* `.s`
* `swap`
* `nip`
* `tuck`



### Stack effects (bottom → top)

Some **specifications:** 

* `+`

  ```forth
  +   ( x y -- x+y )
  ```

  Pops two values, pushes their sum.

* `dup`

  ```forth
  dup ( x -- x x )
  ```

  Duplicates the top of the stack.

* `.`

  ```forth
  .   ( x -- )
  ```

  Pops the top value and **prints it**.

* `.s`

  ```forth
  .s  ( -- )
  ```

  Prints **all** values currently on the data stack (bottom → top) but **does not change** the stack.

* `swap`

  ```forth
  swap ( x y -- y x )
  ```

  Swaps the top two stack items.

* `nip`

  ```forth
  nip ( x y -- y )
  ```

  Drops the second-from-top, keeps the top.

* `tuck`

  ```forth
  tuck ( x y -- y x y )
  ```

  Takes the top two items and rearranges them so the top is duplicated and “tucked” under the original top.
  If the stack is `… x y`, it becomes `… y x y`.

Everything **below** the top two values is left unchanged by `swap`, `nip`, and `tuck`.

---

## Translation Details

1. **The CPU stack as the Forth data stack**

   * Using `pushq` and `popq` to implement Forth pushes and pops.
   * The bottom of the logical Forth stack corresponds to deeper stack addresses; the top of the Forth stack is at `(%rsp)`.


2. **Printing (`.` and `.s`) with `printf`**

   * Using `printf` from `libc` whenever the Forth code uses `.` or `.s`.
   * Puting a format string in `.rodata`, e.g.:

     ```asm
     .section .rodata
     ```

   fmt_int:
   .asciz "%ld\n"

   ````

   - Following the System V AMD64 calling convention for `printf`:

   - `%rdi` → pointer to format string (`fmt_int`).
   - `%rsi` → value to print (for integers).
   - Clear `%rax` before calling `printf` (for variadic functions).


3. **`. (dot)`**

   * Forth `.`
   * Pop the top of the data stack into a register.
   * Call `printf` to print it.
   * After printing, that value should no longer be on the stack (just like Forth).

5. **`.s` (dot-s)**

   * Forth `.s`
   * Print **all** current stack values from **bottom to top**.
   * Do **not** change the logical contents of the stack after `.s` returns.

---
## Files

* **Input**

  * `code.fs` – Forth program.
 
* **Compiler**
  * `ForthCompiler.java` - the compiler

* **Output**

  * `code.s` – the assembly translation of `code.fs`.
  * `code` - executable obtained with the compiler by **as** and **ld** commands

A simple `Makefile` that can build with:

```bash
make
```

---

## How to run

- The compiler can be called via command-line, accepting a file path to the Forth source code:
  ```bash
  javac ForthCompiler.java
  java ForthCompiler code.fs
  ````
- First compile it, then run it on the Forth source.
- The above java compiler compilation and run is in the **Makefile**(simply can type make)


