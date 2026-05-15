import os

IOS_DIR = "ios/FamWake/Resources"

append_text = """
/* iOS Specific (Fallback to English) */
"member_pause" = "Pause";
"member_resume" = "Resume";
"member_not_active_today" = "Not active today";
"""

count = 0
for folder in os.listdir(IOS_DIR):
    if not folder.endswith(".lproj"):
        continue
    
    # Skip the ones we already edited
    if folder in ["en.lproj", "de.lproj"]:
        continue
        
    out_path = os.path.join(IOS_DIR, folder, "Localizable.strings")
    if not os.path.exists(out_path):
        continue
        
    with open(out_path, "a", encoding="utf-8") as f:
        f.write("\n" + append_text)
    
    count += 1
    print(f"Appended to {folder}")

print(f"Done. Updated {count} languages.")
