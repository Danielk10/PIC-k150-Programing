import os
import re
import xml.etree.ElementTree as ET

def find_all_files(directory, extensions):
    matched_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                matched_files.append(os.path.join(root, file))
    return matched_files

def main():
    res_dir = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/res"
    java_dir = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/java"
    manifest_path = "/home/danielpdiamon/PIC-k150-Programing/app/src/main/AndroidManifest.xml"
    
    base_strings_path = os.path.join(res_dir, "values/strings.xml")
    if not os.path.exists(base_strings_path):
        print(f"Error: {base_strings_path} no existe.")
        return

    tree = ET.parse(base_strings_path)
    root = tree.getroot()
    string_names = []
    for child in root:
        if child.tag == 'string':
            name = child.attrib.get('name')
            if name:
                string_names.append(name)

    java_files = find_all_files(java_dir, [".java"])
    xml_files = find_all_files(res_dir, [".xml"])
    xml_files.append(manifest_path)

    file_contents = []
    for file_path in java_files:
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                file_contents.append((file_path, "java", f.read()))
        except Exception as e:
            pass

    for file_path in xml_files:
        if "strings.xml" in file_path:
            continue
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                file_contents.append((file_path, "xml", f.read()))
        except Exception as e:
            pass

    unused_strings = []
    
    for name in string_names:
        java_pattern = re.compile(r'\bR\.string\.' + re.escape(name) + r'\b')
        xml_pattern = re.compile(r'@string/' + re.escape(name) + r'\b')
        
        is_used = False
        for path, file_type, content in file_contents:
            if file_type == "java":
                if java_pattern.search(content):
                    is_used = True
                    break
            elif file_type == "xml":
                if xml_pattern.search(content):
                    is_used = True
                    break
                    
        if not is_used:
            unused_strings.append(name)

    print(f"Encontradas {len(unused_strings)} strings sin usar.")

    strings_files = []
    for root_dir, dirs, files in os.walk(res_dir):
        for file in files:
            if file == "strings.xml":
                strings_files.append(os.path.join(root_dir, file))

    print(f"Procesando {len(strings_files)} archivos strings.xml...")

    for strings_file in strings_files:
        try:
            with open(strings_file, "r", encoding="utf-8") as f:
                lines = f.readlines()
            
            cleaned_lines = []
            removed_count = 0
            for line in lines:
                should_remove = False
                for name in unused_strings:
                    if f'name="{name}"' in line:
                        should_remove = True
                        removed_count += 1
                        break
                if not should_remove:
                    cleaned_lines.append(line)
            
            with open(strings_file, "w", encoding="utf-8") as f:
                f.writelines(cleaned_lines)
            
            rel_path = os.path.relpath(strings_file, res_dir)
            print(f" - {rel_path}: {removed_count} strings eliminadas.")
            
        except Exception as e:
            print(f"Error procesando {strings_file}: {e}")

    print("¡Limpieza de strings sin usar completada!")

if __name__ == "__main__":
    main()
