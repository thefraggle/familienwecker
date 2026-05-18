import glob
import re

ios_res = "ios/FamWake/Resources"

for ios_file in glob.glob(f"{ios_res}/*.lproj/Localizable.strings"):
    with open(ios_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = []
    # Only keep the last occurrence of registration_disclaimer
    # Actually better: remove all registration_disclaimer and replace it with the ones that have %1$@
    
    disclaimer_line = None
    for line in lines:
        if line.startswith('"registration_disclaimer"'):
            if "%1$@" in line or "%2$@" in line:
                disclaimer_line = line
            elif "%1$s" in line:
                disclaimer_line = line.replace("%1$s", "%1$@").replace("%2$s", "%2$@")
            else:
                # the one with just %@
                pass
        else:
            new_lines.append(line)
            
    if disclaimer_line:
        new_lines.append(disclaimer_line)
        
    with open(ios_file, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
