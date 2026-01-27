# 📚 SDCC 4.5.0 INSTALLATION ON TERMUX FOR ANDROID
## Small Device C Compiler for PIC

**Version for Termux Google Play**

---

## ⚠️ MANDATORY REQUIREMENT BEFORE STARTING

### 🔴 GPUTILS 1.5.2 IS MANDATORY

This tutorial requires that you have **GPUTILS 1.5.2 installed and working correctly** on your Termux. SDCC depends completely on GPUTILS as the base for compiling code for PIC.

**If you already installed GPUTILS 1.5.2 following the previous tutorial:** You can **skip directly to Step 1 of this tutorial**.

**If you have NOT installed it yet:** You must **install GPUTILS 1.5.2 first** following the specific tutorial or completing the initial steps of this document.

#### Verify if you have GPUTILS installed:

Run these commands in Termux:

```bash
gpasm --version
gplink --version
gplib --version
```

If you see something like:

```
gpasm-1.5.2 #1325 (Jan 25 2026)
gplink-1.5.2 #1325 (Jan 25 2026)
gplib-1.5.2 #1325 (Jan 25 2026)
```

✅ **GPUTILS is installed correctly! Continue with Step 1 of this tutorial.**

If you see "command not found" or similar, you must install GPUTILS first.

---

## 📋 TUTORIAL CONTENTS

1. Update Repositories (if necessary)
2. Install Base Dependencies
3. Install Advanced Dependencies
4. Verify Prerequisites
5. Download SDCC Source Code
6. Extract Files
7. Configure SDCC
8. Compile SDCC (⏱️ LONG PROCEDURE)
9. Install SDCC
10. Verify Installation
11. Compile First C Program for PIC16F628A
12. Copy HEX File to Downloads
13. Troubleshooting

---

# 🔧 GPUTILS 1.5.2 INSTALLATION (FOR USERS WHO DID NOT INSTALL IT)

If you already completed the GPUTILS installation with the previous tutorial, **SKIP THIS SECTION** and go to **STEP 1** of the SDCC tutorial.

## 📦 STEP A.1: Update Termux Repositories

Open the Termux application and update the system packages:

```bash
pkg update && pkg upgrade -y
```

**ℹ️ Important Notes:**
- This command downloads and installs the latest updates
- The `-y` parameter automatically answers "yes" to confirmations
- This ensures security and compatibility

🔐 **Configure Storage Access:**

```bash
termux-setup-storage
```

You will need to accept the storage permission on your Android device.

---

## 🔧 STEP A.2: Install GPUTILS Dependencies

Install the basic compilation tools:

```bash
pkg install build-essential clang make wget tar bzip2 -y
```

**📋 Installed Packages:**
- `build-essential`: Basic compilation tools (gcc, g++, etc.)
- `clang`: Modern C/C++ compiler
- `make`: Build automation
- `wget`: File download
- `tar/bzip2`: File extraction

Then install binary utilities:

```bash
pkg install binutils -y
```

---

## 📥 STEP A.3: Download GPUTILS 1.5.2

```bash
wget https://sourceforge.net/projects/gputils/files/gputils/1.5.0/gputils-1.5.2.tar.bz2/download -O gputils-1.5.2.tar.bz2
```

---

## 📂 STEP A.4: Extract GPUTILS

```bash
tar -xjf gputils-1.5.2.tar.bz2
cd gputils-1.5.2
```

---

## ⚙️ STEP A.5: Configure GPUTILS

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie"
```

---

## 🔨 STEP A.6: Compile GPUTILS

```bash
make -j$(nproc)
```

---

## 💾 STEP A.7: Install GPUTILS

```bash
make install
```

---

## ✅ STEP A.8: Verify GPUTILS

```bash
gpasm --version
gplink --version
gplib --version
```

You should see the version of each tool.

---

# 📚 SDCC 4.5.0 INSTALLATION TUTORIAL

## 📖 WHAT IS SDCC?

**SDCC** (Small Device C Compiler) is a retargetable optimizing standard C compiler that supports multiple architectures:

- **STM8, MCS-51, DS390, HC08, S08, Z80, Z180, R800, Rabbit, SM83, eZ80**
- **Microchip PIC16 and PIC18** ✅ (What we will use)
- **Padauk PDK13, PDK14, PDK15**
- **MOS 6502 and 6502**

### 🎯 Why use SDCC for PIC?

- **Faster development** in C vs Assembler
- **More readable and maintainable code**
- **Easy portability** between different PICs
- **Generates efficient code** for microcontrollers
- **Compatible with GPUTILS** for final assembly

---

## 📦 STEP 1: Update Termux Repositories

Open Termux and update all packages:

```bash
pkg update && pkg upgrade -y
```

**ℹ️ Explanation:**
- Ensures you have the most recent versions of dependencies
- Applies security patches
- Prevents compatibility conflicts

---

## 🔐 Configure Storage Access

If you didn't do this previously:

```bash
termux-setup-storage
```

Accept the permission when prompted.

---

## 🔧 STEP 2: Install Base Dependencies

Install essential compilers and tools:

```bash
pkg install wget tar bzip2 -y
```

Then:

```bash
pkg install clang make binutils build-essential cmake -y
```

**📋 Package Explanation:**
- `wget`: Download files from Internet
- `tar/bzip2`: Extract compressed files
- `clang`: Modern C/C++ compiler
- `make`: Build system
- `binutils`: Binary manipulation tools
- `build-essential`: GCC compilers
- `cmake`: Alternative build system

Verify that binutils is installed:

```bash
pkg install binutils -y
```

---

## 🔧 STEP 3: Install Advanced Dependencies

SDCC requires additional libraries for advanced compilation:

```bash
pkg install libandroid-execinfo -y
```

**ℹ️ `libandroid-execinfo`:** Stack tracing support on Android.

Install parsing and analysis tools:

```bash
pkg install bison flex boost zlib texinfo -y
```

**📋 Explanation:**
- `bison`: Parser generator
- `flex`: Lexer generator
- `boost`: C++ utility library needed for SDCC
- `zlib`: Data compression
- `texinfo`: Documentation and manuals

Install Boost headers:

```bash
pkg install boost-headers -y
```

Install C++ standard library:

```bash
pkg install libc++ -y
```

**ℹ️ `libc++`:** C++ Standard Library implementation compatible with Android/Termux.

---

## 📥 STEP 4: Download SDCC 4.5.0 Source Code

Download the compressed file from SourceForge:

```bash
wget https://sourceforge.net/projects/sdcc/files/sdcc/4.5.0/sdcc-src-4.5.0.tar.bz2
```

**⏱️ Download time:** 5-15 minutes depending on your connection speed.

---

## 📂 STEP 5: Extract Source Code

Once downloaded, extract the file:

```bash
tar -jxvf sdcc-src-4.5.0.tar.bz2
```

Navigate to the extracted directory:

```bash
cd sdcc-4.5.0
```

**📋 Command Explanation:**
- `tar`: Archive file manipulation command
- `-jxvf`: Combined options:
  - `-j`: Decompress using bzip2
  - `-x`: Extract files
  - `-v`: Verbose mode (shows progress)
  - `-f`: Specifies the file to process

---

## ⚙️ STEP 6: Configure SDCC

This step prepares SDCC for compilation specifically optimized for Termux/Android:

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
```

**🔍 Detailed Explanation of Options:**

- `./configure`: Script that prepares the project
- `AR=ar`: Specifies the archiver for static libraries
- `RANLIB=ranlib`: Generates library indexes
- `--prefix=$PREFIX`: Installs in `/data/data/com.termux/files/usr`

**Important Compilation Options:**

- `CFLAGS="-fPIC -fPIE"`:
  - `-fPIC`: Position Independent Code
  - `-fPIE`: Position Independent Executable
  - Necessary for security on modern Android

- `CXXFLAGS="-fPIC -fPIE"`: Same for C++ code

- `LDFLAGS="-pie"`: Linker configured for PIE

- `gcc_cv_c_no_fpie=no`: Don't disable PIE
- `gcc_cv_no_pie=no`: Don't disable PIE compilation

**⚠️ Important:**

If you see error messages, check the configuration file:

```bash
cat config.log | grep -i error
```

---

## 🔨 STEP 7: Compile SDCC

⏱️ **⚠️ IMPORTANT WARNING:**

SDCC compilation **TAKES MORE THAN 5 HOURS** on a typical mobile device.

### 📋 Critical Instructions:

1. **DO NOT CANCEL THE PROCESS** - Even if it looks frozen, it's normal
2. **CONNECT YOUR PHONE TO CHARGER** - Battery must stay at 100%
3. **BE PATIENT** - The code is very large, this is normal
4. **DO NOT CLOSE TERMUX** during compilation

Start the compilation with:

```bash
make -j$(nproc)
```

**📋 Command Explanation:**
- `make`: Reads Makefile and executes build instructions
- `-j$(nproc)`: Parallel compilation using all cores
  - `-j`: Enable parallelization
  - `$(nproc)`: Automatically detect number of cores
  - This significantly speeds up the process

**⏱️ Expected Time:**
- 4-core devices: 5-8 hours
- 8-core devices: 3-5 hours
- Varies by phone model and available RAM

**🎯 What to Expect During Compilation:**

- You'll see output lines during the first 30-60 minutes
- Then it might appear to "freeze" without new output
- **THIS IS COMPLETELY NORMAL** - The compiler is working
- There can be 30-60 minute periods without output
- The process is running, just compiling complex code

### ⚠️ If You Receive Error: `[Process completed (signal 9)]`

This error occurs when Android (the kernel) forces Termux to close. The causes are:

1. **Phantom Process Killer** (Android 12+): System closes resource-intensive apps
2. **Lack of RAM**: Process tried to use more memory than available

#### Solutions (Ordered by Effectiveness):

**Solution 1: Battery Configuration (FAST)**

1. Go to **Settings → Applications → Termux**
2. Select **Battery** or **Battery Usage**
3. Change to **"No restrictions"** or **"Don't optimize"**
4. In recent applications, long-press the Termux window
5. Select the **lock icon** to prevent automatic closing

**Solution 2: Disable Phantom Process Killer (ANDROID 12+)**

If you have Android 12, 13 or 14, this is the most important step.

**Method A: Via ADB from PC (If available):**

```bash
adb shell "/system/bin/device_config set_sync_disabled_for_tests persistent"
adb shell "/system/bin/device_config put activity_manager max_phantom_processes 2147483647"
adb shell settings put global settings_enable_monitor_phantom_procs false
```

**Method B: Via LADB (Without PC - On your phone):**

1. Download the **LADB** app from Play Store or GitHub
2. In Termux, activate **Developer Options** → **Wireless Debugging**
3. Open LADB and connect with Wireless Debugging
4. Execute the same 3 commands from above

**Method C: Feature Flags (Some phones)**

1. Go to **Settings → Developer Options**
2. Look for **"Feature Flags"** or **"Feature Flags"**
3. Find `settings_enable_monitor_phantom_procs`
4. Set it to **False** (disabled)

### ✅ If Compilation Cancels:

**Good news:** You can continue where it stopped.

Simply run again:

```bash
make -j$(nproc)
```

The system will detect what's already compiled and continue from that point. **YOU DON'T NEED TO START OVER**.

---

## 💾 STEP 8: Install SDCC

Once compilation is complete (after 5+ hours):

```bash
make install
```

**ℹ️ Note:** Installation is much faster (2-5 minutes).

**📍 Installation Locations:**
- Binaries: `/data/data/com.termux/files/usr/bin/`
  - `sdcc`: Main SDCC compiler
  - `sdcpp`: Preprocessor
  - `cppstm8`: Preprocessor for STM8
  
- Libraries: `/data/data/com.termux/files/usr/lib/`
  - SDCC libraries for different architectures
  
- Includes: `/data/data/com.termux/files/usr/share/sdcc/`
  - Headers for PIC, STM8, Z80, etc.

---

## ✅ STEP 9: Verify Installation

Verify that SDCC installed correctly:

```bash
sdcc -v
```

**Expected result (or similar):**

```
SDCC : mcs51/z80/z180/r2k/r2ka/r3ka/sm83/tlcs90/ez80_z80/z80n/r800/ds390/pic16/pic14/TININative/ds400/hc08/s08/stm8/pdk13/pdk14/pdk15/mos6502/mos65c02/f8 TD- 4.5.0 #15242 (Linux)
published under GNU General Public License (GPL)
```

✅ If you see the SDCC version, **installation was successful!**

---

## 🔄 (OPTIONAL) Clean Installation Files

If you want to free up space, you can delete the extracted folders and compressed files:

```bash
cd ~
rm -rf gputils-1.5.2
rm -rf sdcc-4.5.0
rm -f gputils-1.5.2.tar.bz2
rm -f sdcc-src-4.5.0.tar.bz2
```

**Warning:** Only do this if you confirmed that SDCC and GPUTILS work correctly.

---

# 💻 COMPILE C CODE FOR PIC16F628A WITH SDCC

Now that you have SDCC installed, compile C code for PIC.

## STEP 10: Create the C File

Create a file with your favorite editor:

```bash
nano prueba_led.c
```

---

## 📝 C CODE: LED Blink with PIC16F628A

Copy the following code in the editor:

```c
#include <pic16f628a.h>
#include <stdint.h>

// ============================================================
// FUSE CONFIGURATION FOR PIC16F628A
// ============================================================
// In SDCC 4.5.0 for PIC14, configuration is defined like this:
// - _INTRC_OSC_NOCLKOUT: Use internal oscillator without RA6 output
// - _WDT_OFF: Disable watchdog timer
// - _LVP_OFF: Disable low voltage programming (security)
// - _MCLRE_ON: MCLR pin active (Master Reset)

__code uint16_t __at (0x2007) _conf = _INTRC_OSC_NOCLKOUT & _WDT_OFF & _LVP_OFF & _MCLRE_ON;

// ============================================================
// DELAY FUNCTION
// ============================================================
// Generates an approximate delay to achieve visible
// LED blink frequency
//
// The keyword 'volatile' is CRITICAL here:
// Without it, SDCC compiler might "optimize" (eliminate)
// this empty loop as unnecessary code.
// With 'volatile', SDCC knows the loop has side effects
// and must execute completely.

void delay(void) {
    // Create an approximate delay of ~500ms at 4MHz
    // The value 10000 is adjusted based on your experimental testing
    for (volatile uint16_t i = 0; i < 10000; i++);
}

// ============================================================
// MAIN FUNCTION
// ============================================================
// This function executes once when the PIC starts

void main(void) {
    // Disable analog comparators
    // The PIC16F628A has comparators on PORTB that interfere
    // with digital output if not disabled.
    // 0x07 disables all comparators
    CMCON = 0x07;
    
    // Configure PORTB as digital outputs
    // 0x00 = all PORTB pins as outputs
    // 1 = input, 0 = output
    TRISB = 0x00;
    
    // ============================================================
    // MAIN LOOP (INFINITE LOOP)
    // ============================================================
    // This loop repeats indefinitely while the PIC is
    // powered on, alternating between turning LED on and off
    
    while(1) {
        // Turn on the LED
        // RB0 = 1 sets pin RB0 to high level (5V)
        RB0 = 1;
        
        // Wait approximately 500ms
        delay();
        
        // Turn off the LED
        // RB0 = 0 sets pin RB0 to low level (0V)
        RB0 = 0;
        
        // Wait approximately 500ms
        delay();
        
        // The loop repeats, turning LED on and off
        // Result: LED blinks with period of ~1 second
    }
}

// ============================================================
// TECHNICAL NOTE: DIFFERENCES WITH ASSEMBLER
// ============================================================
//
// In Assembler (ASM):
// - Each instruction exactly controls what the PIC does
// - You must manually manage: banks, registers, bits
// - More lines of code (50-100 lines for this program)
// - Fast compilation but difficult to maintain
//
// In C with SDCC:
// - You write high-level logic
// - SDCC generates assembler code automatically
// - Only 40 lines of C code with comments
// - Easier to read, maintain and debug
// - Compiler optimizes the generated code
//
// ============================================================
```

---

## 💾 Save the File

Press:
1. **CTRL + X** to exit the editor
2. **Y** to confirm you want to save
3. **ENTER** to confirm the filename

---

## 🔨 STEP 11: Compile with SDCC

Compile the C code for PIC16F628A:

```bash
sdcc -mpic14 -p16f628a --use-non-free prueba_led.c
```

**🔍 Explanation of Compilation Options:**

- `sdcc`: SDCC compiler
- `-mpic14`: Specifies compilation for PIC14 family
  - `pic14` = PIC16F628A, PIC16F877, etc. (14-bit architecture)
  - Note: There's also `-mpic16` for PIC18 (16-bit architecture)
- `-p16f628a`: Specifies the exact PIC model
  - Allows model-specific optimizations
  - Automatically configures available RAM
- `--use-non-free`: Uses non-free libraries and scripts from GPUTILS
  - Necessary to get the final optimized HEX file
  - Without this option, wouldn't generate compilable .hex

**📂 Generated Files:**

Verify the created files:

```bash
ls -la prueba_led.*
```

**You should see:**

- `prueba_led.asm`: Assembler code generated by SDCC
  - You can view it to understand what SDCC generates
- `prueba_led.lst`: Listing with cross references
  - Maps C instructions to assembler
- `prueba_led.cod`: COD debug file
  - Used by simulators and debuggers
- `prueba_led.hex`: ✅ **THE FILE YOU NEED**
  - Machine code in hexadecimal format for programming the PIC
  - This is the file you'll load into your programmer

**⚠️ Compilation Messages (They're Normal):**

You might see messages like:

```
prueba_led.asm:91:Message[1304] Page selection not needed for this device. No code generated.
warning: Relocation symbol "_cinit" [0x0000] has no section.
```

These are **normal warnings** in SDCC for PIC14. They don't affect the compiled program's functionality.

---

## 📋 STEP 12: Copy HEX File to Downloads

Copy the HEX file to your Downloads folder so you can access it from the programming application:

```bash
cp prueba_led.hex ~/storage/downloads/
```

**Verify the copy:**

```bash
ls -l ~/storage/downloads/prueba_led.hex
```

You should see:

```
-rw-r--r-- 1 u0_a123 u0_a123  1234 Jan 26 10:30 ~/storage/downloads/prueba_led.hex
```

---

## 🚀 STEP 13: Program Your PIC16F628A

Now you have the `.hex` file ready to program into your PIC.

### 📝 Final Steps:

1. **Connect your PIC K150 programmer** to your Android device
   - If using USB-OTG, connect: Phone → USB-OTG Adapter → Programmer
   
2. **Open the "PIC K150 Programming" application**
   
3. **Select the PIC model:**
   - Look for `PIC16F628A` in the model list
   
4. **Load the HEX file:**
   - Click "Load file" or "Load"
   - Navigate to: `Storage → Downloads → prueba_led.hex`
   
5. **Verify connection:**
   - Make sure the PIC is correctly inserted in the programmer
   - The application should detect the device
   
6. **Start programming:**
   - Click "Program" or "Program"
   - Wait for completion (usually 10-30 seconds)

7. **Verify success:**
   - The application should show a success message
   - Some programmers have a green LED when finished

---

## ✨ Expected Result

Once programmed correctly, your LED **will blink continuously**:
- **On:** ~500 milliseconds
- **Off:** ~500 milliseconds
- **Total period:** ~1 second

---

# 📚 COMPARISON: ASSEMBLER vs C WITH SDCC

To understand the advantages of using C, we compare both approaches:

## 📊 Comparison Table

| Aspect | Assembler (ASM) | C with SDCC |
|--------|-----------------|-----------|
| **Lines of code** | 80-120 lines | 30-40 lines |
| **Learning curve** | Very difficult | Moderate |
| **Development speed** | Very slow | Fast |
| **Readability** | Difficult | Easy |
| **Maintainability** | Complex | Simple |
| **Portability to another PIC** | Requires major rewrite | Change `-p16f628a` to other model |
| **Risk of errors** | High | Low |
| **Hardware control** | Total | High (sufficient for 99% of cases) |
| **Execution speed** | Maximum | 95-98% of maximum |
| **Code size (HEX)** | Minimum | Minimum-Small |

---

## 🔍 Example: Same Program in ASM

For comparison, LED blink in **Assembler** would be:

```asm
; LED Blink on PIC16F628A - Assembler
; This is equivalent to the previous C code

    LIST P=16F628A
    #include <p16f628a.inc>

; Fuse configuration
    __CONFIG _CP_OFF & _WDT_OFF & _PWRTE_ON & _INTRC_OSC_NOCLKOUT & _LVP_OFF & _BODEN_OFF & _MCLRE_ON

; Variables for delay (3 bytes of RAM)
    CBLOCK 0x20
        d1
        d2
        d3
    ENDC

; Reset vector
    ORG 0x00
    goto Inicio

; Main program
Inicio:
    ; Configure PORTB as outputs
    banksel TRISB
    movlw   b'00000000'
    movwf   TRISB

    ; Disable comparators
    banksel CMCON
    movlw   0x07
    movwf   CMCON

    banksel PORTB

; Main loop
Bucle:
    bsf     PORTB, 0        ; Turn on LED (RB0 = 1)
    call    Retardo         ; Wait 500ms
    bcf     PORTB, 0        ; Turn off LED (RB0 = 0)
    call    Retardo         ; Wait 500ms
    goto    Bucle

; Delay subroutine (~500ms at 4MHz)
Retardo:
    movlw   0x03
    movwf   d1
    movlw   0x18
    movwf   d2
    movlw   0x02
    movwf   d3

Retardo_Loop:
    decfsz  d1, f
    goto    $+2
    decfsz  d2, f
    goto    $+2
    decfsz  d3, f
    goto    Retardo_Loop

    return

    END
```

**Analysis:**
- **120 lines of code** vs 40 lines of C
- **Much manual management** of memory banks
- **Complex calculations** for exact delays
- **Difficult to modify** (changing blink time requires recalculating numbers)
- **Higher risk of errors** in bank addressing

---

## 🎯 When to Use Each

**Use ASSEMBLER when:**
- You need maximum speed in critical sections
- You have very tight memory/space constraints
- You do very specific hardware access
- You already have tested ASM code

**Use C with SDCC when (99% of the time):**
- ✅ Fast development
- ✅ More readable and maintainable code
- ✅ Easier changes and debugging
- ✅ More portable code
- ✅ Less error-prone
- ✅ Ideal for learning

---

# 🎓 DETAILED EXPLANATION OF COMPILATION FLAGS

## 🔨 Main SDCC Options

### 1. **Architecture Selection**

```bash
-mpic14      # 14-bit PIC (16F628A, 16F877, etc.)
-mpic16      # 16-bit PIC (18F2550, 18F4550, etc.)
-mstm8       # STM8 (STMicroelectronics microcontrollers)
-mz80        # Z80 (Zilog processor)
```

### 2. **Specific Device Selection**

```bash
-p16f628a    # Specific PIC
-p16f877a    # Specific PIC
-p18f2550    # Specific PIC
```

SDCC adapts the code for the exact characteristics of the chip.

### 3. **Optimization Options**

```bash
-O2          # Speed optimization
-O3          # Maximum optimization
-Os          # Size optimization (smaller .hex)
```

**Recommendation for PIC:** `-O2` is generally best.

### 4. **Output Options**

```bash
--out-fmt-ihx  # Generate .hex file (Intel HEX) - Default
--out-fmt-s19  # Generate .s19 file (Motorola S-Record)
```

### 5. **Libraries and Scripts**

```bash
--use-non-free  # Use non-free libraries (gputils)
                # NECESSARY to generate .hex correctly
```

---

## 📚 EXPLANATION OF SDCC CONFIGURE

Remember that in installation we used:

```bash
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
```

### 🔍 Explanation of each flag:

#### **CFLAGS="-fPIC -fPIE"**

- `-fPIC` (Position Independent Code):
  - Generates code that can run from any memory address
  - Necessary for dynamic library loading
  - On Android, almost mandatory for security

- `-fPIE` (Position Independent Executable):
  - Generates position-independent executables
  - Provides ASLR protection (Address Space Layout Randomization)
  - Protection against exploits

#### **LDFLAGS="-pie"**

- Linker instructions to generate PIE executables
- Complements the `-fPIE` from the compiler
- Ensures the final executable is position-independent

#### **gcc_cv_c_no_fpie=no**

- Configuration script variable
- `-no`: Don't disable PIE compilation
- Ensures that PIE support detection works correctly

#### **gcc_cv_no_pie=no**

- Similar to above but for the linking phase
- Ensures that `-pie` is used in the linker

---

## 🎯 Use Case: Example with Different Flags

### Compile with speed optimization:

```bash
sdcc -mpic14 -p16f628a -O2 --use-non-free prueba_led.c
```

### Compile with size optimization:

```bash
sdcc -mpic14 -p16f628a -Os --use-non-free prueba_led.c
```

### Compile without optimization (for debugging):

```bash
sdcc -mpic14 -p16f628a --use-non-free prueba_led.c
```

---

# 💡 SDCC LIMITATIONS FOR PIC

It's important to know the limitations when using SDCC with PIC:

## ⚠️ Technical Limitations

### 1. **Partial PIC16 Support**

- SDCC has **incomplete support** for PIC16 (pic14)
- Many features work well, but some are limited
- Support is **better** on MCS-51 and STM8

### 2. **Memory Limitations**

| Aspect | Limitation |
|--------|-----------|
| **PIC16F628A** | 2KB ROM, 224 bytes RAM |
| **Complex C code** | May not fit on very small chips |
| **Constant strings** | Take up program space |
| **Large arrays** | Limited RAM makes them difficult to use |

### 3. **Hardware Access**

- Not all peripherals are supported via libraries
- Some require manual register access
- PWM, UART, ADC: Work but require register knowledge

### 4. **Floating Point Compilation**

- `float` operations generate very large code
- On PIC16, better to use `int` or `unsigned int`
- Use fixed-point arithmetic libraries if you need decimals

### 5. **Recursion**

- Avoid recursive functions (functions that call themselves)
- PIC16 stack is very limited (~8 levels)
- Better to rewrite as iterative loops

---

## 📋 PIC Family Compatibility

**SDCC supports (with good support):**

- ✅ PIC16F628A, 16F877A (pic14 - 14-bit)
- ✅ PIC18F2550, 18F4550, 18F4620 (pic16 - 16-bit)

**SDCC supports (with limited or experimental support):**

- ⚠️ PIC24 (24-bit) - Very limited support
- ⚠️ dsPIC30/33 - Very limited support
- ⚠️ PIC32 - Better to use XC32 from Microchip

**For other microcontrollers:**

- ✅ STM8: Excellent support (better than PIC)
- ✅ Z80: Excellent support
- ✅ MCS-51: Very good support

---

## 🎯 Alternatives to SDCC

If you need better PIC support:

| Compiler | Advantages | Disadvantages |
|----------|-----------|--------------|
| **SDCC** | Free, opensource, multiplatform | Partial support on PIC |
| **XC8 (Microchip)** | Official Microchip, full support | Proprietary, limited free version |
| **CCS C** | Excellent PIC support | Very expensive, proprietary |
| **PICC18** | Good for PIC18 | Proprietary, expensive |

---

# 🆘 TROUBLESHOOTING

## ❌ Error: "command not found: sdcc"

**Cause:** SDCC not installed in Termux PATH.

**Solution:**

```bash
export PATH=$PREFIX/bin:$PATH
echo 'export PATH=$PREFIX/bin:$PATH' >> ~/.bashrc
```

Then restart Termux.

---

## ❌ Error: "configure: error: C compiler cannot create executables"

**Cause:** Missing C compiler or libraries.

**Solution:**

```bash
pkg install build-essential clang -y
cd ~/sdcc-4.5.0
./configure AR=ar RANLIB=ranlib --prefix=$PREFIX \
            CFLAGS="-fPIC -fPIE" \
            CXXFLAGS="-fPIC -fPIE" \
            LDFLAGS="-pie" \
            gcc_cv_c_no_fpie=no \
            gcc_cv_no_pie=no
make distclean
make -j$(nproc)
```

---

## ❌ Error: "[Process completed (signal 9)]" during compilation

See section **"If You Receive Error: [Process completed (signal 9)]"** in STEP 7.

**In summary:**
1. Configure battery without restrictions
2. Disable Phantom Process Killer (Android 12+)
3. Run `make -j$(nproc)` again to continue

---

## ❌ Error: "gputils: command not found"

**Cause:** GPUTILS not installed or not in PATH.

**Solution:**

Verify that GPUTILS is on the system:

```bash
ls -la $PREFIX/bin/gpasm
```

If it doesn't exist, you must install GPUTILS first.

---

## ❌ Compilation very slow or freezes

**Cause:** Device with few resources or optimized Termux.

**Solutions:**

1. Reduce parallelization:
```bash
make -j2        # Only 2 cores
```

2. Use single core (slower but more stable):
```bash
make            # Without -j
```

3. Close other applications to free RAM

---

# 📚 REFERENCES AND RESOURCES

## 🔗 Official Sites

- **SDCC Official:** https://sourceforge.net/projects/sdcc/
- **GPUTILS Official:** https://sourceforge.net/projects/gputils/
- **Termux Wiki:** https://wiki.termux.com/
- **Microchip PIC16F628A Datasheet:** https://www.microchip.com/

## 📖 Documentation

- **SDCC Manual:** https://sdcc.sourceforge.io/
- **SDCC User Guide:** https://sdcc.sourceforge.io/doc/sdccman.pdf
- **Termux Package Management:** https://wiki.termux.com/wiki/Package_Management

## 📱 Applications

- **Termux:** https://play.google.com/store/apps/details?id=com.termux
- **PIC K150 Programming:** Play Store
- **LADB (ADB from Android):** https://github.com/RikkaApps/LADB

## 🆘 Problems and Solutions

- **Phantom Process Killer:** https://docs.andronix.app/android-12/andronix-on-android-12-and-beyond
- **Termux Issues:** https://github.com/termux/termux-app/issues
- **SDCC Issues:** https://sourceforge.net/projects/sdcc/

---

# ✅ SUCCESS CHECKLIST

Mark each completed step:

- [ ] GPUTILS 1.5.2 installed and verified
- [ ] Termux repositories updated
- [ ] All dependencies installed
- [ ] SDCC 4.5.0 file downloaded
- [ ] Source code extracted without errors
- [ ] Configure completed without errors
- [ ] Compilation started and completed
- [ ] Make install completed successfully
- [ ] `sdcc -v` shows version 4.5.0
- [ ] `prueba_led.c` file created
- [ ] Compilation of prueba_led.c successful
- [ ] `prueba_led.hex` file generated
- [ ] HEX file copied to Downloads
- [ ] PIC programmed correctly
- [ ] LED blinks as expected

---

# 🎉 CONGRATULATIONS!

You have successfully installed **SDCC 4.5.0** on Termux and compiled your first C program for PIC.

**Now you can:**

✅ Compile complex C code for PIC
✅ Use all capabilities of a modern compiler
✅ Develop more complex applications
✅ Share and reuse code easily
✅ Program multiple PICs from your Android

**Suggested next steps:**

1. Experiment with different `delay()` values
2. Use multiple LEDs on different pins
3. Implement input with buttons (PORTB as input)
4. Use PWM to control LED intensity
5. Implement UART for serial communication
6. Explore interrupts in SDCC

---

## 📝 FINAL NOTES

**Important:**
- SDCC is free software but some features require GPUTILS (non-free)
- Compilation is long but only needs to be done once
- Once installed, SDCC will be available for future projects
- You can create multiple programs without reinstalling SDCC

**User Resources:**
- Keep backups of `prueba_led.hex` and other programs
- Document any modifications you make to the commands
- Create a projects folder in `~/storage/downloads/` to organize

---

**Version:** 1.0
**Date:** January 26, 2026
**Platform:** Termux Google Play on Android
**Compiler:** SDCC 4.5.0
**Dependency:** GPUTILS 1.5.2

---