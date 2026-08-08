#!/usr/bin/env python3
import sys
import xml.etree.ElementTree as ET
import re

def find_coords(xml_path, search_type, search_value):
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except Exception as e:
        print(f"Error parsing XML: {e}", file=sys.stderr)
        return None

    for node in root.iter('node'):
        text = node.get('text', '')
        content_desc = node.get('content-desc', '')
        resource_id = node.get('resource-id', '')
        bounds = node.get('bounds', '')
        node_class = node.get('class', '')
        checked = node.get('checked', '')

        matched = False
        if search_type == 'switch':
            # Look for the switch: class is android.view.View, checkable/checked attributes present, and on the right side of the screen
            if 'android.view.View' in node_class and checked in ['true', 'false']:
                # Parse bounds to ensure it's on the right side (x > 800)
                m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
                if m:
                    x1, y1, x2, y2 = map(int, m.groups())
                    if x1 > 800 and 800 < y1 < 1100:
                        matched = True
        elif search_type == 'text' and search_value.lower() in text.lower():
            matched = True
        elif search_type == 'desc' and search_value.lower() in content_desc.lower():
            matched = True
        elif search_type == 'id' and search_value.lower() in resource_id.lower():
            matched = True
        elif search_type == 'any':
            val = search_value.lower()
            if val in text.lower() or val in content_desc.lower() or val in resource_id.lower():
                matched = True

        if matched and bounds:
            m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', bounds)
            if m:
                x1, y1, x2, y2 = map(int, m.groups())
                x = (x1 + x2) // 2
                y = (y1 + y2) // 2
                return x, y
    return None

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: find_coords.py <xml_path> <text|desc|id|any|switch> <value>")
        sys.exit(1)
    
    xml_path = sys.argv[1]
    search_type = sys.argv[2]
    search_val = sys.argv[3]
    
    coords = find_coords(xml_path, search_type, search_val)
    if coords:
        print(f"{coords[0]} {coords[1]}")
        sys.exit(0)
    else:
        sys.exit(1)
