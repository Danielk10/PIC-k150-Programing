#!/usr/bin/env python3
import os
import pty
import select
import struct
import sys
import time
import enum
import termios

# Response enums
class ResponseEnum(enum.Enum):
    YES = b'Y'
    NO = b'N'
    INITIALIZED = b'I'
    PROGRAMING_VOLTAGE_ENABLED = b'V'
    PROGRAMING_VOLTAGE_DISABLED = b'v'
    AT_COMMAND_JUMP_TABLE = b'P'
    INCORRECT_BYTES = b'B'
    WAITING_FOR_ACTION = b'A'
    WAITING_FOR_COMMAND = b'Q'

class HeaderEnum(enum.Enum):
    CONFIGURATION = b'C'
    PROGRAMMER_VERSION = b'B'

def read_exactly(fd, n):
    data = b''
    while len(data) < n:
        r, _, _ = select.select([fd], [], [], 2.0)
        if not r:
            raise TimeoutError("Timeout reading from virtual port")
        chunk = os.read(fd, n - len(data))
        if not chunk:
            raise ConnectionAbortedError("Connection closed")
        data += chunk
    return data

def run_emulator():
    # Setup paths
    base_dir = os.path.dirname(os.path.abspath(__file__))
    symlink_path = os.path.join(base_dir, "vtty")
    
    # Initialize PIC16F628A mock memory by default
    rom_size_words = 2048
    rom_size_bytes = rom_size_words * 2 # 4096
    eeprom_size_bytes = 128
    
    rom = bytearray(b'\xff' * rom_size_bytes)
    eeprom = bytearray(b'\xff' * eeprom_size_bytes)
    fuses = [0x3fff]
    pic_id = bytearray(b'\xff\xff\xff\xff')
    calibrate = 0x3fff
    chip_id = 4192  # PIC16F628A (0x1060)

    def get_config_bytes():
        padded_id = pic_id.ljust(8, b'\xff')
        padded_fuses = fuses + [0xffff] * (7 - len(fuses))
        return struct.pack('<H8s7HH',
            chip_id,
            padded_id,
            *padded_fuses,
            calibrate
        )

    print("================ K150 PROGRAMMER EMULATOR ================")
    print("Simulating PIC microcontrollers (Dynamic Family Support)")
    print("Press Ctrl+C to exit emulator")
    print("==========================================================")

    while True:
        # Create a new pty pair
        master, slave = pty.openpty()
        slave_name = os.ttyname(slave)
        
        # Configure slave to be RAW (no ECHO, no ICANON, no flow control)
        attrs = termios.tcgetattr(slave)
        attrs[0] = attrs[0] & ~termios.IXON & ~termios.IXOFF & ~termios.IXANY
        attrs[1] = attrs[1] & ~termios.OPOST
        attrs[3] = attrs[3] & ~termios.ECHO & ~termios.ICANON & ~termios.IEXTEN & ~termios.ISIG
        termios.tcsetattr(slave, termios.TCSANOW, attrs)
        
        # Update symlink
        if os.path.exists(symlink_path):
            os.unlink(symlink_path)
        os.symlink(slave_name, symlink_path)
        
        print(f"[K150-EMULATOR] Active virtual port: {slave_name}")
        print(f"[K150-EMULATOR] Symlink updated: {symlink_path}")
        print(f"[K150-EMULATOR] Ready. Virtual port symlink: {symlink_path}")
        print("[K150-EMULATOR] Waiting for client connection...")
        
        # Block on read from master PTY
        os.set_blocking(master, True)
        
        state = "AWAITING_JUMP_TABLE"
        buf = b''
        
        try:
            while True:
                if not buf:
                    buf = os.read(master, 1)
                    if not buf:
                        print("[K150-EMULATOR] Client disconnected (EOF).")
                        break
                
                b = buf[0]
                buf = buf[1:] # Consume 1 byte
                
                # Exit command (b'\x01') always resets state and replies 'Q'
                if b == 1:
                    os.write(master, ResponseEnum.WAITING_FOR_COMMAND.value)
                    state = "AWAITING_JUMP_TABLE"
                    continue
                
                if state == "AWAITING_JUMP_TABLE":
                    if b == ord('P'):
                        os.write(master, ResponseEnum.AT_COMMAND_JUMP_TABLE.value)
                        state = "JUMP_TABLE"
                
                elif state == "JUMP_TABLE":
                    cmd = b
                    if cmd == 2:  # Echo
                        echo_byte = read_exactly(master, 1)
                        os.write(master, echo_byte)
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 21:  # programmer_protocol
                        os.write(master, b'P18A')
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 20:  # programmer_version
                        os.write(master, b'\x03')
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 18 or cmd == 19:  # detect chip in/out socket
                        os.write(master, ResponseEnum.WAITING_FOR_ACTION.value)
                        os.write(master, ResponseEnum.YES.value)
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 3:  # init programming vars
                        vars_bytes = read_exactly(master, 11)
                        rom_size_words, eeprom_size_bytes, core_type = struct.unpack('>HHB', vars_bytes[0:5])
                        rom_size_bytes = rom_size_words * 2
                        
                        # Rescale buffers only if size changes
                        if len(rom) != rom_size_bytes:
                            rom = bytearray(b'\xff' * rom_size_bytes)
                        if len(eeprom) != eeprom_size_bytes:
                            eeprom = bytearray(b'\xff' * eeprom_size_bytes)
                        
                        # Dynamically identify chip & core family
                        if core_type in [1, 2, 13]: # 16-bit cores (PIC18)
                            if chip_id != 4640:
                                chip_id = 4640 # PIC18F2550
                                fuses = [0xffff] * 7
                                pic_id = bytearray(b'\xff' * 8)
                            print(f"[K150-EMULATOR] Configured for 16-bit PIC18 (ChipID: {chip_id}, ROM: {rom_size_bytes}B, EEPROM: {eeprom_size_bytes}B)")
                        elif core_type in [4, 11]: # 12-bit cores (PIC12)
                            if chip_id != 65535:
                                chip_id = 65535 # PIC12F508
                                fuses = [0x0fff]
                                pic_id = bytearray(b'\xff' * 4)
                            print(f"[K150-EMULATOR] Configured for 12-bit PIC12 (ChipID: {chip_id}, ROM: {rom_size_bytes}B)")
                        else: # 14-bit cores (PIC12/16)
                            target_id = 4192
                            if rom_size_words == 1024:
                                target_id = 4032 # PIC12F675 (0x0FC0)
                            elif rom_size_words == 8192:
                                target_id = 3616 # PIC16F877A (0x0E20)
                            else:
                                target_id = 4192 # PIC16F628A (0x1060)
                            if chip_id != target_id:
                                chip_id = target_id
                                fuses = [0x3fff]
                                pic_id = bytearray(b'\xff' * 4)
                            print(f"[K150-EMULATOR] Configured for 14-bit PIC12/16 (ChipID: {chip_id}, ROM: {rom_size_bytes}B, EEPROM: {eeprom_size_bytes}B)")
                            
                        # Handshake initialized response
                        os.write(master, ResponseEnum.INITIALIZED.value)
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 4:  # Turn programming voltage ON
                        os.write(master, ResponseEnum.PROGRAMING_VOLTAGE_ENABLED.value)
                        state = "JUMP_TABLE"
                    elif cmd == 5:  # Turn programming voltage OFF
                        os.write(master, ResponseEnum.PROGRAMING_VOLTAGE_DISABLED.value)
                        state = "JUMP_TABLE"
                    elif cmd == 6:  # Cycle programming voltages
                        os.write(master, ResponseEnum.PROGRAMING_VOLTAGE_ENABLED.value)
                        state = "AWAITING_JUMP_TABLE"
                    elif cmd == 14:  # erase chip
                        rom = bytearray(b'\xff' * len(rom))
                        eeprom = bytearray(b'\xff' * len(eeprom))
                        if chip_id == 4640:
                            fuses = [0xffff] * len(fuses)
                        elif chip_id == 65535:
                            fuses = [0x0fff] * len(fuses)
                        else:
                            fuses = [0x3fff] * len(fuses)
                        pic_id = bytearray(b'\xff' * len(pic_id))
                        print("[K150-EMULATOR] Memory erased (mock)")
                        os.write(master, ResponseEnum.YES.value)
                        state = "JUMP_TABLE"
                    elif cmd == 7:  # program ROM
                        word_count_bytes = read_exactly(master, 2)
                        word_count, = struct.unpack('>H', word_count_bytes)
                        os.write(master, ResponseEnum.YES.value)
                        bytes_to_read = word_count * 2
                        print(f"[K150-EMULATOR] Programming ROM: writing {word_count} words ({bytes_to_read} bytes)...")
                        for i in range(0, bytes_to_read, 32):
                            chunk = read_exactly(master, min(32, bytes_to_read - i))
                            rom[i:i+len(chunk)] = chunk
                            os.write(master, ResponseEnum.YES.value)
                        os.write(master, ResponseEnum.AT_COMMAND_JUMP_TABLE.value)
                        state = "JUMP_TABLE"
                    elif cmd == 8:  # program EEPROM
                        byte_count_bytes = read_exactly(master, 2)
                        byte_count, = struct.unpack('>H', byte_count_bytes)
                        os.write(master, ResponseEnum.YES.value)
                        print(f"[K150-EMULATOR] Programming EEPROM: writing {byte_count} bytes...")
                        for i in range(0, byte_count, 2):
                            chunk = read_exactly(master, 2)
                            eeprom[i:i+2] = chunk
                            os.write(master, ResponseEnum.YES.value)
                        read_exactly(master, 2)  # Extra two bytes
                        os.write(master, ResponseEnum.AT_COMMAND_JUMP_TABLE.value)
                        state = "JUMP_TABLE"
                    elif cmd == 9:  # program ID and fuses
                        body = read_exactly(master, 24)
                        if len(fuses) > 1: # PIC18
                            pic_id = bytearray(body[2:10]) # 8 bytes ID
                            for idx in range(7):
                                fuses[idx], = struct.unpack('<H', body[10 + idx*2 : 12 + idx*2])
                        else: # PIC12/16
                            pic_id = bytearray(body[2:6]) # 4 bytes ID
                            fuse_val, = struct.unpack('<H', body[10:12])
                            fuses[0] = fuse_val
                        print(f"[K150-EMULATOR] Programmed ID: {pic_id.hex()}, Fuses: {[f'{x:04X}' for x in fuses]}")
                        os.write(master, ResponseEnum.YES.value)
                        state = "JUMP_TABLE"
                    elif cmd == 11:  # read ROM
                        print(f"[K150-EMULATOR] Reading ROM ({len(rom)} bytes)...")
                        os.write(master, rom)
                        state = "JUMP_TABLE"
                    elif cmd == 12:  # read EEPROM
                        print(f"[K150-EMULATOR] Reading EEPROM ({len(eeprom)} bytes)...")
                        os.write(master, eeprom)
                        state = "JUMP_TABLE"
                    elif cmd == 13:  # read config
                        print("[K150-EMULATOR] Reading config...")
                        os.write(master, HeaderEnum.CONFIGURATION.value)
                        os.write(master, get_config_bytes())
                        state = "JUMP_TABLE"
                    elif cmd == 24 or cmd == 25:  # program cal data for 10Fxxx
                        cal_data = read_exactly(master, 4)
                        cal, backup_cal = struct.unpack('>HH', cal_data)
                        print(f"[K150-EMULATOR] Program 10F Calibration: 0x{cal:04X}, Backup: 0x{backup_cal:04X}")
                        os.write(master, ResponseEnum.YES.value)
                        state = "JUMP_TABLE"
                    else:
                        print(f"[K150-EMULATOR] Warning: Unhandled command {cmd}")
                        state = "JUMP_TABLE"
        except (OSError, ConnectionAbortedError) as e:
            print(f"[K150-EMULATOR] Client disconnected (Error: {e}).")
        finally:
            os.close(master)
            os.close(slave)

if __name__ == '__main__':
    try:
        run_emulator()
    except KeyboardInterrupt:
        print("\n[K150-EMULATOR] Exiting emulator. Goodbye!")
        sys.exit(0)
