import xml.etree.ElementTree as ET
import os

def parse_strings(path):
    if not os.path.exists(path):
        return {}
    try:
        tree = ET.parse(path)
        root = tree.getroot()
        strings = {}
        for child in root:
            if child.tag == 'string':
                name = child.attrib.get('name')
                strings[name] = child.text
        return strings
    except Exception as e:
        print(f"Error parseando {path}: {e}")
        return {}

def main():
    base_path = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/res/values/strings.xml"
    es_path = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/res/values-es-rES/strings.xml"
    ve_path = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/res/values-es-rVE/strings.xml"

    base_strings = parse_strings(base_path)
    es_strings = parse_strings(es_path)
    ve_strings = parse_strings(ve_path)

    print(f"Total Strings base (default): {len(base_strings)}")
    print(f"Total Strings Español (es-rES): {len(es_strings)}")
    print(f"Total Strings Español Venezuela (es-rVE): {len(ve_strings)}")

    # Claves en base que no están en es
    missing_in_es = set(base_strings.keys()) - set(es_strings.keys())
    # Claves en base que no están en ve
    missing_in_ve = set(base_strings.keys()) - set(ve_strings.keys())
    # Claves en es que no están en base
    extra_in_es = set(es_strings.keys()) - set(base_strings.keys())
    # Claves en ve que no están en base
    extra_in_ve = set(ve_strings.keys()) - set(base_strings.keys())

    print("\n=== REPORTE DE SINCRONIZACIÓN DE TRADUCCIONES ===")
    
    if not missing_in_es and not missing_in_ve and not extra_in_es and not extra_in_ve:
        print("✔ ¡Las claves de los archivos de recursos están 100% sincronizadas!")
    else:
        if missing_in_es:
            print(f"❌ Faltan traducciones en es-rES ({len(missing_in_es)}): {sorted(list(missing_in_es))}")
        else:
            print("✔ Sin claves faltantes en es-rES.")
            
        if missing_in_ve:
            print(f"❌ Faltan traducciones en es-rVE ({len(missing_in_ve)}): {sorted(list(missing_in_ve))}")
        else:
            print("✔ Sin claves faltantes en es-rVE.")
            
        if extra_in_es:
            print(f"⚠ Claves huérfanas en es-rES (no existen en base) ({len(extra_in_es)}): {sorted(list(extra_in_es))}")
        else:
            print("✔ Sin claves huérfanas en es-rES.")
            
        if extra_in_ve:
            print(f"⚠ Claves huérfanas en es-rVE (no existen en base) ({len(extra_in_ve)}): {sorted(list(extra_in_ve))}")
        else:
            print("✔ Sin claves huérfanas en es-rVE.")

if __name__ == '__main__':
    main()
