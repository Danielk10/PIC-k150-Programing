#!/usr/bin/env python3
import sys
import os

# Priority to local cloned picpro repo if present
_LOCAL_PICPRO = "/home/danielpdiamon/picpro"
if os.path.isdir(_LOCAL_PICPRO) and _LOCAL_PICPRO not in sys.path:
    sys.path.insert(0, _LOCAL_PICPRO)

import serial

# Patch Serial to ignore DTR/RTS ioctls on virtual PTYs
serial.Serial._update_dtr_state = lambda self: None
serial.Serial._update_rts_state = lambda self: None

# Patch IConnection.reset to bypass hardware DTR toggling and boot banner check on PTY
import picpro.protocol.IConnection

def custom_reset(self):
    self.detected_programmer_version = 3  # Force K150 version detection
    return True

picpro.protocol.IConnection.IConnection.reset = custom_reset

from picpro.bin.picpro import main

if __name__ == '__main__':
    main()
