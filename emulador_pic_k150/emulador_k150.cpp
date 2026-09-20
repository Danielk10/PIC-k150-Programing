#include <iostream>
#include <vector>
#include <string>
#include <cstring>
#include <algorithm>
#include <unistd.h>
#include <fcntl.h>
#include <termios.h>
#include <sys/select.h>
#include <cstdint>
#include <climits>

enum State {
    AWAITING_JUMP_TABLE,
    JUMP_TABLE
};

bool read_exactly(int fd, uint8_t* buf, size_t n) {
    size_t total = 0;
    while (total < n) {
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(fd, &rfds);
        
        struct timeval tv;
        tv.tv_sec = 2;
        tv.tv_usec = 0;
        
        int s = select(fd + 1, &rfds, nullptr, nullptr, &tv);
        if (s <= 0) {
            return false; // Timeout or error
        }
        
        ssize_t r = read(fd, buf + total, n - total);
        if (r <= 0) {
            return false; // EOF or error
        }
        total += r;
    }
    return true;
}

void run_emulator(const std::string& symlink_path) {
    // Initialize mock memory by default (PIC16F628A)
    size_t rom_size_words = 2048;
    size_t rom_size_bytes = rom_size_words * 2; // 4096
    size_t eeprom_size_bytes = 128;
    
    std::vector<uint8_t> rom(rom_size_bytes, 0xFF);
    std::vector<uint8_t> eeprom(eeprom_size_bytes, 0xFF);
    std::vector<uint16_t> fuses_vector(1, 0x3FFF);
    std::vector<uint8_t> pic_id(4, 0xFF);
    uint16_t calibrate = 0x3FFF;
    uint16_t chip_id = 4192;  // PIC16F628A (0x1060)
 
    std::cout << "================ K150 PROGRAMMER EMULATOR (C++) ================" << std::endl;
    std::cout << "Simulating PIC microcontrollers (Dynamic Family Support)" << std::endl;
    std::cout << "Press Ctrl+C to exit emulator" << std::endl;
    std::cout << "================================================================" << std::endl;
 
    while (true) {
        // Create a new pty pair
        int master_fd = posix_openpt(O_RDWR | O_NOCTTY);
        if (master_fd < 0) {
            std::cerr << "Error: posix_openpt failed." << std::endl;
            break;
        }
        if (grantpt(master_fd) < 0 || unlockpt(master_fd) < 0) {
            std::cerr << "Error: grantpt or unlockpt failed." << std::endl;
            close(master_fd);
            break;
        }
        
        char* slave_name = ptsname(master_fd);
        if (!slave_name) {
            std::cerr << "Error: ptsname failed." << std::endl;
            close(master_fd);
            break;
        }
        // Configure slave to be RAW (no ECHO, no ICANON, no flow control)
        int slave_fd = open(slave_name, O_RDWR | O_NOCTTY);
        if (slave_fd >= 0) {
            struct termios attrs;
            if (tcgetattr(slave_fd, &attrs) == 0) {
                attrs.c_iflag &= ~(IXON | IXOFF | IXANY);
                attrs.c_lflag &= ~(ECHO | ICANON | IEXTEN | ISIG);
                attrs.c_oflag &= ~OPOST;
                tcsetattr(slave_fd, TCSANOW, &attrs);
            }
        } else {
            std::cerr << "Warning: Could not open slave to set attributes." << std::endl;
        }
        
        // Update symlink
        unlink(symlink_path.c_str());
        if (symlink(slave_name, symlink_path.c_str()) < 0) {
            std::cerr << "Error: Creating symlink failed." << std::endl;
            close(master_fd);
            break;
        }
        
        std::cout << "\n[K150-EMULATOR-C++] Active virtual port: " << slave_name << std::endl;
        std::cout << "[K150-EMULATOR-C++] Symlink updated: " << symlink_path << std::endl;
        std::cout << "[K150-EMULATOR-C++] Ready. Virtual port symlink: " << symlink_path << std::endl;
        std::cout << "[K150-EMULATOR-C++] Waiting for client connection..." << std::endl;
        
        State state = AWAITING_JUMP_TABLE;
        
        try {
            while (true) {
                uint8_t b;
                ssize_t r = read(master_fd, &b, 1);
                if (r <= 0) {
                    std::cout << "[K150-EMULATOR-C++] Client disconnected (EOF)." << std::endl;
                    break;
                }
                
                // Exit command (b'\x01') always resets state and replies 'Q'
                if (b == 1) {
                    uint8_t resp = 'Q';
                    write(master_fd, &resp, 1);
                    state = AWAITING_JUMP_TABLE;
                    continue;
                }
                
                if (state == AWAITING_JUMP_TABLE) {
                    if (b == 'P') {
                        uint8_t resp = 'P';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    }
                }
                else if (state == JUMP_TABLE) {
                    uint8_t cmd = b;
                    if (cmd == 2) {  // Echo
                        uint8_t echo_byte;
                        if (!read_exactly(master_fd, &echo_byte, 1)) break;
                        write(master_fd, &echo_byte, 1);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 21) {  // programmer_protocol
                        write(master_fd, "P18A", 4);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 20) {  // programmer_version
                        uint8_t resp = 3;
                        write(master_fd, &resp, 1);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 18 || cmd == 19) {  // detect chip in/out socket
                        uint8_t resp_a = 'A';
                        uint8_t resp_y = 'Y';
                        write(master_fd, &resp_a, 1);
                        write(master_fd, &resp_y, 1);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 3) {  // init programming vars
                        uint8_t vars[11];
                        if (!read_exactly(master_fd, vars, 11)) break;
                        
                        rom_size_words = (vars[0] << 8) | vars[1];
                        eeprom_size_bytes = (vars[2] << 8) | vars[3];
                        uint8_t core_type = vars[4];
                        rom_size_bytes = rom_size_words * 2;
                        
                        // Rescale buffers only if size changes
                        if (rom.size() != rom_size_bytes) {
                            rom.assign(rom_size_bytes, 0xFF);
                        }
                        if (eeprom.size() != eeprom_size_bytes) {
                            eeprom.assign(eeprom_size_bytes, 0xFF);
                        }
                        
                        // Dynamically identify chip & core family
                        if (core_type == 1 || core_type == 2 || core_type == 13) { // 16-bit PIC18
                            if (chip_id != 4640) {
                                chip_id = 4640; // PIC18F2550
                                fuses_vector.assign(7, 0xFFFF);
                                pic_id.assign(8, 0xFF);
                            }
                            std::cout << "[K150-EMULATOR-C++] Configured for 16-bit PIC18 (ChipID: " << chip_id 
                                      << ", ROM: " << rom_size_bytes << "B, EEPROM: " << eeprom_size_bytes << "B)" << std::endl;
                        } else if (core_type == 4 || core_type == 11) { // 12-bit PIC12
                            if (chip_id != 65535) {
                                chip_id = 65535; // PIC12F508
                                fuses_vector.assign(1, 0x0FFF);
                                pic_id.assign(4, 0xFF);
                            }
                            std::cout << "[K150-EMULATOR-C++] Configured for 12-bit PIC12 (ChipID: " << chip_id 
                                      << ", ROM: " << rom_size_bytes << "B)" << std::endl;
                        } else { // 14-bit PIC12/16
                            uint16_t target_id = 4192;
                            if (rom_size_words == 1024) {
                                target_id = 4032; // PIC12F675 (0x0FC0)
                            } else if (rom_size_words == 8192) {
                                target_id = 3616; // PIC16F877A (0x0E20)
                            } else {
                                target_id = 4192; // PIC16F628A (0x1060)
                            }
                            if (chip_id != target_id) {
                                chip_id = target_id;
                                fuses_vector.assign(1, 0x3FFF);
                                pic_id.assign(4, 0xFF);
                            }
                            std::cout << "[K150-EMULATOR-C++] Configured for 14-bit PIC12/16 (ChipID: " << chip_id 
                                      << ", ROM: " << rom_size_bytes << "B, EEPROM: " << eeprom_size_bytes << "B)" << std::endl;
                        }
                        
                        uint8_t resp = 'I';
                        write(master_fd, &resp, 1);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 4) {  // Turn programming voltage ON
                        uint8_t resp = 'V';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 5) {  // Turn programming voltage OFF
                        uint8_t resp = 'v';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 6) {  // Cycle programming voltages
                        uint8_t resp = 'V';
                        write(master_fd, &resp, 1);
                        state = AWAITING_JUMP_TABLE;
                    } else if (cmd == 14) {  // erase chip
                        std::fill(rom.begin(), rom.end(), 0xFF);
                        std::fill(eeprom.begin(), eeprom.end(), 0xFF);
                        uint16_t default_fuse = 0x3FFF;
                        if (chip_id == 4640) default_fuse = 0xFFFF;
                        else if (chip_id == 65535) default_fuse = 0x0FFF;
                        std::fill(fuses_vector.begin(), fuses_vector.end(), default_fuse);
                        std::fill(pic_id.begin(), pic_id.end(), 0xFF);
                        std::cout << "[K150-EMULATOR-C++] Memory erased (mock)" << std::endl;
                        uint8_t resp = 'Y';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 7) {  // program ROM
                        uint8_t wcount_bytes[2];
                        if (!read_exactly(master_fd, wcount_bytes, 2)) break;
                        uint16_t word_count = (wcount_bytes[0] << 8) | wcount_bytes[1];
                        uint8_t resp = 'Y';
                        write(master_fd, &resp, 1);
                        size_t bytes_to_read = word_count * 2;
                        std::cout << "[K150-EMULATOR-C++] Programming ROM: writing " << word_count << " words (" << bytes_to_read << " bytes)..." << std::endl;
                        for (size_t i = 0; i < bytes_to_read; i += 32) {
                            size_t chunk_size = std::min((size_t)32, bytes_to_read - i);
                            uint8_t chunk[32];
                            if (!read_exactly(master_fd, chunk, chunk_size)) break;
                            std::memcpy(&rom[i], chunk, chunk_size);
                            write(master_fd, &resp, 1);
                        }
                        uint8_t table_resp = 'P';
                        write(master_fd, &table_resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 8) {  // program EEPROM
                        uint8_t bcount_bytes[2];
                        if (!read_exactly(master_fd, bcount_bytes, 2)) break;
                        uint16_t byte_count = (bcount_bytes[0] << 8) | bcount_bytes[1];
                        uint8_t resp = 'Y';
                        write(master_fd, &resp, 1);
                        std::cout << "[K150-EMULATOR-C++] Programming EEPROM: writing " << byte_count << " bytes..." << std::endl;
                        for (size_t i = 0; i < byte_count; i += 2) {
                            uint8_t chunk[2];
                            if (!read_exactly(master_fd, chunk, 2)) break;
                            std::memcpy(&eeprom[i], chunk, 2);
                            write(master_fd, &resp, 1);
                        }
                        uint8_t extra[2];
                        if (!read_exactly(master_fd, extra, 2)) break;
                        uint8_t table_resp = 'P';
                        write(master_fd, &table_resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 9) {  // program ID and fuses
                        uint8_t body[24];
                        if (!read_exactly(master_fd, body, 24)) break;
                        if (fuses_vector.size() > 1) { // PIC18
                            std::memcpy(pic_id.data(), &body[2], 8);
                            for (size_t i = 0; i < 7; ++i) {
                                fuses_vector[i] = body[10 + i * 2] | (body[11 + i * 2] << 8);
                            }
                        } else { // PIC12/16
                            std::memcpy(pic_id.data(), &body[2], 4);
                            fuses_vector[0] = body[10] | (body[11] << 8);
                        }
                        std::cout << "[K150-EMULATOR-C++] Programmed ID: ";
                        for (size_t i = 0; i < pic_id.size(); ++i) printf("%02X", pic_id[i]);
                        printf(", Fuses: ");
                        for (size_t i = 0; i < fuses_vector.size(); ++i) printf("%04X ", fuses_vector[i]);
                        printf("\n");
                        uint8_t resp = 'Y';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    } else if (cmd == 11) {  // read ROM
                        std::cout << "[K150-EMULATOR-C++] Reading ROM (" << rom.size() << " bytes)..." << std::endl;
                        write(master_fd, rom.data(), rom.size());
                        state = JUMP_TABLE;
                    } else if (cmd == 12) {  // read EEPROM
                        std::cout << "[K150-EMULATOR-C++] Reading EEPROM (" << eeprom.size() << " bytes)..." << std::endl;
                        write(master_fd, eeprom.data(), eeprom.size());
                        state = JUMP_TABLE;
                    } else if (cmd == 13) {  // read config
                        std::cout << "[K150-EMULATOR-C++] Reading config..." << std::endl;
                        uint8_t config_header = 'C';
                        write(master_fd, &config_header, 1);
                        
                        // Build config bytes (26 bytes)
                        uint8_t config_bytes[26];
                        config_bytes[0] = chip_id & 0xFF;
                        config_bytes[1] = (chip_id >> 8) & 0xFF;
                        
                        // Copy ID (up to 8 bytes, padded with 0xFF)
                        for (size_t i = 0; i < 8; ++i) {
                            config_bytes[2 + i] = (i < pic_id.size()) ? pic_id[i] : 0xFF;
                        }
                        
                        // Copy 7 fuses (14 bytes, padded with 0xFFFF)
                        for (size_t i = 0; i < 7; ++i) {
                            uint16_t val = (i < fuses_vector.size()) ? fuses_vector[i] : 0xFFFF;
                            config_bytes[10 + i * 2] = val & 0xFF;
                            config_bytes[11 + i * 2] = (val >> 8) & 0xFF;
                        }
                        
                        config_bytes[24] = calibrate & 0xFF;
                        config_bytes[25] = (calibrate >> 8) & 0xFF;
                        
                        write(master_fd, config_bytes, 26);
                        state = JUMP_TABLE;
                    } else if (cmd == 24 || cmd == 25) {  // program cal data for 10Fxxx
                        uint8_t cal_data[4];
                        if (!read_exactly(master_fd, cal_data, 4)) break;
                        uint16_t cal = cal_data[1] | (cal_data[0] << 8);
                        uint16_t backup_cal = cal_data[3] | (cal_data[2] << 8);
                        std::cout << "[K150-EMULATOR-C++] Program 10F Calibration: 0x" << std::hex << cal 
                                  << ", Backup: 0x" << backup_cal << std::dec << std::endl;
                        uint8_t resp = 'Y';
                        write(master_fd, &resp, 1);
                        state = JUMP_TABLE;
                    } else {
                        std::cout << "[K150-EMULATOR-C++] Warning: Unhandled command " << (int)cmd << std::endl;
                        state = JUMP_TABLE;
                    }
                }
            }
        } catch (...) {
            std::cout << "[K150-EMULATOR-C++] Exception occurred. Resetting connection." << std::endl;
        }
        
        if (slave_fd >= 0) {
            close(slave_fd);
        }
        close(master_fd);
    }
}

int main(int argc, char* argv[]) {
    // Default symlink path
    std::string symlink_path = "./vtty";
    if (argc > 1) {
        symlink_path = argv[1];
    } else {
        // Resolve absolute path in directory of executable
        char result[PATH_MAX];
        ssize_t count = readlink("/proc/self/exe", result, PATH_MAX);
        if (count != -1) {
            std::string path(result, count);
            size_t pos = path.find_last_of("/");
            if (pos != std::string::npos) {
                symlink_path = path.substr(0, pos) + "/vtty";
            }
        }
    }
    
    run_emulator(symlink_path);
    return 0;
}
