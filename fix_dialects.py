import os

IOS_DIR = "ios/FamWake/Resources"

# The English block that was appended
eng_text = """
/* iOS Specific (Fallback to English) */
"member_pause" = "Pause";
"member_resume" = "Resume";
"member_not_active_today" = "Not active today";
"""

# The German block to replace it with
ger_text = """
/* iOS Specific (Fallback to German) */
"member_pause" = "Pausieren";
"member_resume" = "Aktivieren";
"member_not_active_today" = "Heute nicht aktiv";
"""

dialects = ["gsw.lproj", "ksh.lproj", "swg.lproj"]

for dialect in dialects:
    path = os.path.join(IOS_DIR, dialect, "Localizable.strings")
    if os.path.exists(path):
        with open(path, "r", encoding="utf-8") as f:
            content = f.read()
            
        content = content.replace(eng_text, ger_text)
        
        with open(path, "w", encoding="utf-8") as f:
            f.write(content)
        print(f"Fixed {dialect}")

print("Done.")
