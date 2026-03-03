class Structure:
    def __init__(self, **dict):
        self.__dict__.update(dict)

    def __repr__(self):
        result = 'Structure('

        first = True
        for k in self.__dict__:
            if (first):
                first = False
            else:
                result += ', '
            if (k[0] != '_'):
                result += k + ' = ' + repr(self.__dict__[k])
        result += ')'
        return result

    def _print(self):
        print str(self)
