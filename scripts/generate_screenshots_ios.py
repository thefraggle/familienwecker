import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

# Base directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB_IMAGES_DIR = "/Users/daniel.notthoff/GIT_Repos/_privat/familienwecker-web/images"
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/ios_generated")
BACKGROUND_PATH = os.path.join(PROJECT_ROOT, "docs/internal/images/hintergrund_hochformat.png")

# Font paths
FONT_REGULAR_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_regular.ttf")
FONT_BOLD_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_bold.ttf")

# Design Constants
ACCENT_COLOR = "#FF8C42"  # Sunrise Orange
TEXT_COLOR = "#FFFFFF"    # White

# Output display sizes
SIZES = {
    "6.5": (1242, 2688),
    "6.7": (1290, 2796)
}

# The 6 slides configuration
SLIDES = {
    "de": [
        {
            "headline": "Nie wieder\nBad-Stau!",
            "description": "Der smarte Plan für Bad, Frühstück\nund ganz entspanntes Aufstehen.",
            "screenshot": "screenshot_6_main_dark.png"
        },
        {
            "headline": "Perfekt\nfür jeden",
            "description": "Die App plant Badezimmer-Zeiten\ngerecht — kein Warten mehr.",
            "screenshot": "screenshot_5_config_dark.png"
        },
        {
            "headline": "Guten Morgen,\nPapa!",
            "description": "Persönliche Weckrufe mit dem\nsüßen Panda-Maskottchen.",
            "screenshot": "screenshot_3_sleep_dark.png"
        },
        {
            "headline": "Länger\nschlafen",
            "description": "Wochenenden? Feiertage? FamWake lässt\ndich automatisch ausschlafen.",
            "screenshot": "screenshot_2_setup_dark.png"
        },
        {
            "headline": "Sofort\nverbunden",
            "description": "Teile einen Code. Lade deine Familie ein.\nStarte entspannt in den Tag.",
            "screenshot": "screenshot_7_settings_dark.png"
        },
        {
            "headline": "Gemeinsam\nfrühstücken",
            "description": "FamWake plant deinen Morgen –\nvom Aufstehen bis zum Frühstück.",
            "screenshot": "screenshot_6_main_dark.png"
        }
    ],
    "en": [
        {
            "headline": "No More\nMorning Chaos!",
            "description": "The smart plan for bath, breakfast\nand a relaxed wake-up.",
            "screenshot": "screenshot_6_main_dark_en.png"
        },
        {
            "headline": "Perfect\nfor Everyone",
            "description": "The app plans bathroom times\nfairly — no more waiting.",
            "screenshot": "screenshot_5_config_dark_en.png"
        },
        {
            "headline": "Good Morning,\nDad!",
            "description": "Personal wake-up calls with the\ncute panda mascot.",
            "screenshot": "screenshot_3_sleep_dark_en.png"
        },
        {
            "headline": "Sleep In\nLonger",
            "description": "Weekends? Holidays? FamWake lets\nyou sleep in automatically.",
            "screenshot": "screenshot_2_setup_dark_en.png"
        },
        {
            "headline": "Instantly\nConnected",
            "description": "Share code. Invite family.\nStart your day relaxed.",
            "screenshot": "screenshot_7_settings_dark_en.png"
        },
        {
            "headline": "Breakfast\nTogether",
            "description": "FamWake plans your morning –\nfrom wake-up to breakfast.",
            "screenshot": "screenshot_6_main_dark_en.png"
        }
    ]
}

def fit_background(bg_path, target_size):
    bg = Image.open(bg_path)
    bg_w, bg_h = bg.size
    tgt_w, tgt_h = target_size
    
    # Scale to cover target size
    ratio = max(tgt_w / bg_w, tgt_h / bg_h)
    new_w = int(bg_w * ratio)
    new_h = int(bg_h * ratio)
    bg_resized = bg.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    # Crop center
    left = (new_w - tgt_w) // 2
    top = (new_h - tgt_h) // 2
    right = left + tgt_w
    bottom = top + tgt_h
    return bg_resized.crop((left, top, right, bottom))

def draw_iphone_status_bar(draw, clock_font):
    # Left clock
    draw.text((120, 68), "9:41", font=clock_font, fill="#FFFFFF")
    
    # Right icons: simple shapes to mock WiFi, signal, battery
    # Signal bars
    draw.rectangle([980, 78, 984, 88], fill="#FFFFFF")
    draw.rectangle([988, 74, 992, 88], fill="#FFFFFF")
    draw.rectangle([996, 70, 1000, 88], fill="#FFFFFF")
    draw.rectangle([1004, 66, 1008, 88], fill="#FFFFFF")
    
    # WiFi (simple arcs mocked with circles/sectors or lines)
    # We can draw three simple horizontal/curved shapes or just a WiFi icon-like triangle
    draw.polygon([(1025, 86), (1035, 70), (1045, 86)], fill="#FFFFFF")
    
    # Battery body
    draw.rounded_rectangle([1060, 70, 1095, 86], radius=4, outline="#FFFFFF", width=2)
    # Battery tip
    draw.rectangle([1095, 74, 1098, 82], fill="#FFFFFF")
    # Battery level
    draw.rectangle([1063, 73, 1088, 83], fill="#FFFFFF")

def create_iphone_mockup(screenshot_path):
    # Neutral screenshot is 1080x2245
    ss = Image.open(screenshot_path).convert("RGBA")
    
    # Base canvas for mock with bezel: 1130x2295
    bezel_w, bezel_h = 1130, 2295
    mock = Image.new("RGBA", (bezel_w, bezel_h), (0, 0, 0, 0))
    
    # Create phone outline bezel
    draw = ImageDraw.Draw(mock)
    
    # Bezel shadow / dark metal rim
    draw.rounded_rectangle([0, 0, bezel_w, bezel_h], radius=75, fill="#1F1F1F")
    # Bezel inner black
    draw.rounded_rectangle([5, 5, bezel_w-5, bezel_h-5], radius=70, fill="#000000")
    
    # Paste screenshot inside bezel (leave 25px border for bezel)
    # screen area: 1080x2245 starting at (25, 25)
    mock.paste(ss, (25, 25), ss)
    
    # Draw iOS components on top of the screen area
    screen_draw = ImageDraw.Draw(mock)
    
    # Clock font
    try:
        clock_font = ImageFont.truetype(FONT_BOLD_PATH, 34)
    except:
        clock_font = ImageFont.load_default()
        
    # Draw iOS status bar inside screenshot space
    draw_iphone_status_bar(screen_draw, clock_font)
    
    # Dynamic Island pill centered at X=565, Y=65 (width=220, height=55)
    screen_draw.rounded_rectangle([565 - 110, 52, 565 + 110, 88], radius=18, fill="#000000")
    
    # Home indicator at the bottom (centered, Y=2260, width=320, height=6)
    screen_draw.rounded_rectangle([565 - 160, 2256, 565 + 160, 2262], radius=3, fill="#FFFFFF")
    
    return mock

def build_screenshot(slide, lang, size_name, target_size):
    # Fit background
    img = fit_background(BACKGROUND_PATH, target_size)
    draw = ImageDraw.Draw(img)
    
    # Setup fonts
    tgt_w, tgt_h = target_size
    scale = tgt_w / 1242.0
    
    h_size = int(92 * scale)
    d_size = int(40 * scale)
    
    try:
        font_h = ImageFont.truetype(FONT_BOLD_PATH, h_size)
        font_d = ImageFont.truetype(FONT_REGULAR_PATH, d_size)
    except Exception as e:
        print(f"Font loading failed: {e}. Using default.")
        font_h = ImageFont.load_default()
        font_d = ImageFont.load_default()
        
    # Render Text Headers (Left aligned at X=100)
    text_x = int(100 * scale)
    text_y = int(150 * scale)
    
    # Draw Headline (Orange / Accent)
    draw.text((text_x, text_y), slide["headline"], font=font_h, fill=ACCENT_COLOR, spacing=12)
    
    # Calculate headline height
    bbox_h = draw.textbbox((text_x, text_y), slide["headline"], font=font_h)
    desc_y = bbox_h[3] + int(45 * scale)
    
    # Draw Description (White)
    draw.text((text_x, desc_y), slide["description"], font=font_d, fill=TEXT_COLOR, spacing=15)
    
    # Load and process screenshot
    screenshot_name = slide["screenshot"]
    screenshot_path = os.path.join(WEB_IMAGES_DIR, screenshot_name)
    if not os.path.exists(screenshot_path):
        # Try finding non-dark or fallback
        fallback_name = screenshot_name.replace("_dark", "")
        screenshot_path = os.path.join(WEB_IMAGES_DIR, fallback_name)
        if not os.path.exists(screenshot_path):
            print(f"Screenshot {screenshot_name} not found, skipping!")
            return None
            
    # Create framed phone
    phone = create_iphone_mockup(screenshot_path)
    
    # Scale phone to target size: phone size in 1242x2688 is approx width=880 px
    phone_tgt_w = int(880 * scale)
    phone_ratio = phone.width / phone.height
    phone_tgt_h = int(phone_tgt_w / phone_ratio)
    phone = phone.resize((phone_tgt_w, phone_tgt_h), Image.Resampling.LANCZOS)
    
    # Rotate phone by -10 degrees
    rotation_angle = -10
    phone_rotated = phone.rotate(rotation_angle, expand=True, fillcolor=(0, 0, 0, 0))
    
    # Create drop shadow
    shadow_blur_radius = int(24 * scale)
    shadow = Image.new('RGBA', phone_rotated.size, (0, 0, 0, 0))
    alpha = phone_rotated.split()[3]
    # Paste a blurred black mask for soft shadow
    shadow.paste((0, 0, 0, 110), mask=alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(shadow_blur_radius))
    
    # Paste coordinates for rotated phone (center-bottom)
    # X coord centers the rotated image horizontally, Y coord sticks it slightly above bottom
    paste_x = (tgt_w - phone_rotated.width) // 2 + int(40 * scale)
    paste_y = tgt_h - phone_rotated.height + int(200 * scale) # partially cut off at bottom
    
    # Offset shadow
    shadow_offset = (int(20 * scale), int(35 * scale))
    
    # Composite shadow and phone
    img.paste(shadow, (paste_x + shadow_offset[0], paste_y + shadow_offset[1]), shadow)
    img.paste(phone_rotated, (paste_x, paste_y), phone_rotated)
    
    # Convert to RGB to save as high-quality JPEG
    if img.mode == 'RGBA':
        img = img.convert('RGB')
        
    return img

def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    print(f"Target directory: {OUTPUT_DIR}")
    
    for lang, slides_list in SLIDES.items():
        for size_name, size in SIZES.items():
            print(f"Generating size {size_name} ({size[0]}x{size[1]}) for language '{lang}'...")
            for idx, slide in enumerate(slides_list):
                img = build_screenshot(slide, lang, size_name, size)
                if img:
                    filename = f"screenshot_{idx+1}_{lang}_{size_name}.jpg"
                    save_path = os.path.join(OUTPUT_DIR, filename)
                    img.save(save_path, "JPEG", quality=92)
                    print(f"  Saved {filename}")

if __name__ == "__main__":
    main()
