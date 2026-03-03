import struct

class Buffer(object):
    def __init__(self, src_string):
        self._data = src_string

    def __repr__(self):
        result_list = ["'"]

        for c in self._data:
            hex_char = hex(struct.unpack('B', c)[0])[2:]
            result_list.append(r'\x' + hex_char)
        result_list.append("'")

        return ''.join(result_list)
