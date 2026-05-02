import os

FG_FILE = ".antigravity/generate_feature_graphics.py"
SS_FILE = ".antigravity/generate_screenshots.py"

def patch_fonts(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. Update cjk_fonts function
    old_func = """    if lang == "ja":
        return JP_FONT_PATH, 0, JP_FONT_LIGHT, 0
    elif lang == "ko":
        return KO_FONT_BOLD, 8, KO_FONT_LIGHT, 2
    elif lang == "zh-CN":
        return ZH_FONT_BOLD, 0, ZH_FONT_LIGHT, 0
    return FONT_PATH, 0, FONT_PATH, 0"""
    
    new_func = """    if lang == "ja":
        return JP_FONT_PATH, 0, JP_FONT_LIGHT, 0
    elif lang == "ko":
        return KO_FONT_BOLD, 8, KO_FONT_LIGHT, 2
    elif lang == "zh-CN":
        return ZH_FONT_BOLD, 0, ZH_FONT_LIGHT, 0
    elif lang in ("hi", "mr"):
        return "/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc", 1, "/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc", 0
    elif lang == "bn":
        return "/System/Library/Fonts/Supplemental/Bangla Sangam MN.ttc", 1, "/System/Library/Fonts/Supplemental/Bangla Sangam MN.ttc", 0
    elif lang == "vi":
        return "/System/Library/Fonts/Supplemental/Arial Bold.ttf", 0, "/System/Library/Fonts/Supplemental/Arial.ttf", 0
    return FONT_PATH, 0, FONT_PATH, 0"""
    
    content = content.replace(old_func, new_func)

    # 2. Update is_cjk checks
    old_check_fg = 'is_cjk = lang in ("ja", "ko", "zh-CN")'
    new_check_fg = 'is_cjk = lang in ("ja", "ko", "zh-CN", "hi", "mr", "bn", "vi")'
    content = content.replace(old_check_fg, new_check_fg)

    old_check_ss = 'is_cjk = (lang in ("ja", "ko", "zh-CN"))'
    new_check_ss = 'is_cjk = (lang in ("ja", "ko", "zh-CN", "hi", "mr", "bn", "vi"))'
    content = content.replace(old_check_ss, new_check_ss)

    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)

if __name__ == "__main__":
    patch_fonts(FG_FILE)
    patch_fonts(SS_FILE)
    print("Fonts patched successfully.")
