#!/usr/bin/python
import getopt
import sys

def handle_command_line(args):
    (getopt_opts, getopt_args) = getopt.getopt(args[1:], 'a:bcde:',
                                               ['narf', 'zort', 'poit='])
    print (getopt_opts, getopt_args)


if (__name__ == '__main__'):
    handle_command_line(sys.argv)


