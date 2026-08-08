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
    used_strings = []
    
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
                    
        if is_used:
            used_strings.append(name)
        else:
            unused_strings.append(name)

    print(f"Total: {len(string_names)}")
    print(f"Usados: {len(used_strings)}")
    print(f"No usados: {len(unused_strings)}")
    for name in sorted(unused_strings):
        print(name)

if __name__ == "__main__":
    main()
