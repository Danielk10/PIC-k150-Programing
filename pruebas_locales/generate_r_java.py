import re

def main():
    strings_xml_path = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/res/values/strings.xml"
    r_java_path = "/home/danielpdiamon/PIC-k150-Programing/pruebas_locales/com/diamon/pic/R.java"

    with open(strings_xml_path, "r", encoding="utf-8") as f:
        content = f.read()

    # Extraer todos los atributos name de las etiquetas <string>
    names = re.findall(r'<string\s+name="([^"]+)"', content)

    r_java_content = """package com.diamon.pic;

public class R {
    public static class string {
"""
    for i, name in enumerate(sorted(set(names))):
        r_java_content += f"        public static final int {name} = {i + 1};\n"

    r_java_content += """    }
}
"""

    with open(r_java_path, "w", encoding="utf-8") as f:
        f.write(r_java_content)

    print(f"R.java generado exitosamente con {len(names)} constantes de string en: {r_java_path}")

if __name__ == "__main__":
    main()
