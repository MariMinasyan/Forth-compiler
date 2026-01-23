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
