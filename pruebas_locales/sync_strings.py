import os
import xml.etree.ElementTree as ET
import re

def sync_strings():
    res_dir = "app/src/main/res"
    base_path = os.path.join(res_dir, "values/strings.xml")
    
    tree = ET.parse(base_path)
    base_root = tree.getroot()
    base_keys = {child.attrib.get('name'): child.text for child in base_root if child.tag == 'string'}
    
    all_string_files = []
    for root_dir, dirs, files in os.walk(res_dir):
        for file in files:
            if file == "strings.xml":
                path = os.path.join(root_dir, file)
                if path != base_path:
                    all_string_files.append(path)
                    
    for lang_path in all_string_files:
        try:
            lang_tree = ET.parse(lang_path)
            lang_root = lang_tree.getroot()
            
            # Remove keys not in base
            for child in list(lang_root):
                if child.tag == 'string' and child.attrib.get('name') not in base_keys:
                    lang_root.remove(child)
            
            # Add missing keys
            lang_keys = {child.attrib.get('name') for child in lang_root if child.tag == 'string'}
            for key, value in base_keys.items():
                if key not in lang_keys:
                    new_el = ET.SubElement(lang_root, 'string', {'name': key})
                    new_el.text = value
                
            lang_tree.write(lang_path, encoding='utf-8', xml_declaration=True)
        except Exception as e:
            print(f"Error sincronizando {lang_path}: {e}")

if __name__ == "__main__":
    sync_strings()
    print("Sincronización completada.")
