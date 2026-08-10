import os
import time
import shutil
from PIL import Image, ImageDraw, ImageFont, ImageFilter
from deep_translator import GoogleTranslator

# Base directories
PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEVICES_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/devices")
OUTPUT_DIR = os.path.join(PROJECT_ROOT, "docs/internal/images/screenshots/android")
BACKGROUND_PATH = os.path.join(PROJECT_ROOT, "docs/internal/images/hintergrund_hochformat.png")

# Font paths
FONT_REGULAR_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_regular.ttf")
FONT_BOLD_PATH = os.path.join(PROJECT_ROOT, "app/src/main/res/font/nunito_bold.ttf")

SYSTEM_HELVETICA = "/System/Library/Fonts/Helvetica.ttc"
SYSTEM_JAPANESE = "/System/Library/Fonts/Hiragino Sans GB.ttc"
SYSTEM_KOREAN = "/System/Library/Fonts/AppleSDGothicNeo.ttc"
SYSTEM_DEVANAGARI = "/System/Library/Fonts/Supplemental/Devanagari Sangam MN.ttc"
SYSTEM_BENGALI = "/System/Library/Fonts/Supplemental/Bangla Sangam MN.ttc"

# Design Constants
ACCENT_COLOR = "#FF8C42"  # Sunrise Orange
TEXT_COLOR = "#FFFFFF"    # White

# Output display size for Google Play Store (1080x1920)
TARGET_SIZE = (1080, 1920)

# Bounding box of the clean screen inside the 1187x2513 Android mockup.
SCREEN_CROP_BOX = (63, 195, 1124, 2390)

# Verified hand-crafted screenshot texts for all 22 languages
VERIFIED_SS = {
    "de": [
        ("Nie wieder\nBad-Stau!", "Der smarte Plan für Bad, Frühstück\nund ganz entspanntes Aufstehen."),
        ("Passend\nfür jeden", "Die App plant die Badezimmer-Zeiten\nfair und ganz ohne Wartezeit."),
        ("Guten Morgen,\nPapa!", "Persönliche Weckrufe mit dem\nsüßen Panda-Maskottchen."),
        ("Länger\nausschlafen", "Wochenende? Feiertag? FamWake lässt\ndich automatisch länger schlafen."),
        ("Blitzschnell\nvernetzt", "Code teilen. Familie einladen.\nEntspannt in den Tag starten."),
        ("Gemeinsam\nfrühstücken", "FamWake plant euren Morgen –\nvom Aufstehen bis zum Frühstück.")
    ],
    "en": [
        ("No More\nMorning Chaos!", "The smart plan for bath, breakfast\nand a relaxed wake-up."),
        ("Perfect\nfor Everyone", "The app plans bathroom times\nfairly — no more waiting."),
        ("Good Morning,\nDad!", "Personal wake-up calls with the\ncute panda mascot."),
        ("Sleep In\nLonger", "Weekends? Holidays? FamWake lets\nyou sleep in automatically."),
        ("Instantly\nConnected", "Share code. Invite family.\nStart your day relaxed."),
        ("Breakfast\nTogether", "FamWake plans your morning –\nfrom wake-up to breakfast.")
    ],
    "es": [
        ("¡Sin caos\nmatutino!", "El plan inteligente para baño,\ndesayuno y despertar relajado."),
        ("Perfecto\npara todos", "La app organiza el baño\nde forma justa y sin esperas."),
        ("¡Buenos días,\nPapá!", "Despertares personalizados con\nla linda mascota panda."),
        ("Duerme más\ntiempo", "¿Fines de semana o vacaciones?\nFamWake te permite dormir más."),
        ("Conectado\nal instante", "Comparte el código. Invita a tu familia.\nEmpieza el día relajado."),
        ("Desayunar\njuntos", "FamWake planifica tu mañana –\ndesde levantarse hasta el desayuno.")
    ],
    "fr": [
        ("Fini le chaos\ndu matin !", "Le planning intelligent pour la douche,\nle petit-déjeuner et un réveil serein."),
        ("Parfait\npour tous", "L'application planifie la salle de bain\néquitablement et sans attente."),
        ("Bonjour,\nPapa !", "Des réveils personnalisés avec\nla jolie mascotte panda."),
        ("Grasse matinée\nautomatique", "Week-ends ou vacances ? FamWake\nvous laisse dormir plus longtemps."),
        ("Connecté\nen un instant", "Partagez le code. Invitez la famille.\nCommencez la journée détendu."),
        ("Petit-déjeuner\nensemble", "FamWake planifie votre matinée –\ndu réveil au petit-déjeuner.")
    ],
    "id": [
        ("Bebas Chaos\nPagi Hari!", "Rencana pintar untuk kamar mandi,\nsarapan, dan bangun santai."),
        ("Pas untuk\nSemua", "Aplikasi mengatur waktu mandi\nsecara adil tanpa mengantre."),
        ("Selamat Pagi,\nAyah!", "Panggilan bangun pribadi dengan\nmaskot panda yang lucu."),
        ("Tidur Lebih\nLama", "Akhir pekan atau liburan? FamWake\nmembiarkan Anda tidur nyenyak."),
        ("Terhubung\nSeketika", "Bagikan kode. Undang keluarga.\nMulai hari dengan santai."),
        ("Sarapan\nBersama", "FamWake merencanakan pagi Anda –\ndari bangun tidur hingga sarapan.")
    ],
    "ja": [
        ("朝の混雑を\n解消！", "浴室、朝食、快適な目覚めのための\nスマートな朝のスケジュール。"),
        ("家族みんなに\nピッタリ", "待ち時間ゼロ！アプリが公平に\nバスタイムを計画します。"),
        ("おはよう、\nお父さん！", "かわいいパンダのマスコットによる\nパーソナルモーニングコール。"),
        ("休日もゆっくり\n二度寝", "週末や祝日？FamWakeなら\n自動でゆっくり休めます。"),
        ("コード共有で\nすぐ接続", "コードを共有して家族を招待。\nリラックスした1日をスタート。"),
        ("みんなで\n楽しく朝食", "起床から朝食まで、FamWakeが\nあなたの朝をトータルサポート。")
    ],
    "pt": [
        ("Sem Caos\nDe Manhã!", "O plano inteligente para banho,\ncafé da manhã e um despertar calmo."),
        ("Perfeito\nPara Todos", "O app organiza os horários de banho\nde forma justa e sem filas."),
        ("Bom Dia,\nPai!", "Despertadores personalizados com\no fofo mascote panda."),
        ("Dorma Mais\nTempo", "Fins de semana? FamWake deixa\nvocê dormir mais automaticamente."),
        ("Conectado\nEm Instantes", "Compartilhe o código. Convide a família.\nComece o dia sem estresse."),
        ("Café Da Manhã\nJuntos", "FamWake planeja sua manhã –\ndo despertar ao café da manhã.")
    ],
    "ru": [
        ("Без утреннего\nхаоса!", "Умный план для ванной, завтрака\nи спокойного пробуждения."),
        ("Подходит\nкаждому", "Приложение планирует ванную\nсправедливо и без очередей."),
        ("С добрым утром,\nПапа!", "Персональные будильники с\nмилым маскотом панды."),
        ("Спите дольше\nв выходные", "Выходные или праздник? FamWake\nдаст вам выспаться автоматически."),
        ("Быстрое\nподключение", "Поделитесь кодом и пригласите семью.\nНачните день без стресса."),
        ("Завтракайте\nвместе", "FamWake планирует ваше утро –\nот подъема до завтрака.")
    ],
    "tr": [
        ("Sabah Kaosuna\nSon!", "Banyo, kahvaltı ve rahat uyanış için\nakıllı sabah planı."),
        ("Herkes İçin\nMükemmel", "Uygulama banyo sürelerini\nadilce ve beklemeden planlar."),
        ("Günaydın,\nBaba!", "Sevimli panda maskotu ile\nkişisel uyanış çağrıları."),
        ("Hafta Sonu\nFazla Uyu", "Hafta sonu mu? FamWake otomatik\nolarak daha fazla uyumanızı sağlar."),
        ("Anında\nBağlanın", "Kodu paylaşın, aileyi davet edin.\nGüne huzurla başlayın."),
        ("Birlikte\nKahvaltı", "FamWake sabahınızı planlar –\nuyanıştıan kahvaltıya kadar.")
    ],
    "vi": [
        ("Không Còn\nHỗn Loạn Sáng!", "Kế hoạch thông minh cho nhà tắm,\nbữa sáng và thức dậy thoải mái."),
        ("Hoàn Hảo\nCho Mọi Người", "Ứng dụng sắp xếp giờ tắm\ncông bằng và không cần chờ đợi."),
        ("Chào Buổi Sáng,\nBố!", "Báo thức cá nhân hóa cùng\nlinh vật gấu trúc đáng yêu."),
        ("Ngủ Nướng\nThoải Mái", "Cuối tuần hay ngày lễ? FamWake\ngiúp bạn ngủ nướng tự động."),
        ("Kết Nối\nTức Thì", "Chia sẻ mã. Mời gia đình.\nBắt đầu ngày mới thư thái."),
        ("Ăn Sáng\nCùng Nhau", "FamWake lên kế hoạch buổi sáng –\ntừ lúc dậy đến bữa ăn sáng.")
    ],
    "no": [
        ("Ingen mer\nmorgenkaos!", "Den smarte planen for bad, frokost\nog en avslappet start på dagen."),
        ("Passer for\nalle", "Appen planlegger badetidene\nrettferdig – helt uten kødanning."),
        ("God morgen,\nPappa!", "Personlige vekkinger med den\nsøte pandamascoten."),
        ("Sover lenger\ni helgene", "Helg eller ferie? FamWake lar\ndeg sove lenger automatisk."),
        ("Tilkoblet\npå et blunk", "Del koden. Inviter familien.\nStart dagen uten stress."),
        ("Frokost\nsammen", "FamWake planlegger morgenen –\nfra vekking til frokost.")
    ],
    "nb": [
        ("Ingen mer\nmorgenkaos!", "Den smarte planen for bad, frokost\nog en avslappet start på dagen."),
        ("Passer for\nalle", "Appen planlegger badetidene\nrettferdig – helt uten kødanning."),
        ("God morgen,\nPappa!", "Personlige vekkinger med den\nsøte pandamascoten."),
        ("Sover lenger\ni helgene", "Helg eller ferie? FamWake lar\ndeg sove lenger automatisk."),
        ("Tilkoblet\npå et blunk", "Del koden. Inviter familien.\nStart dagen uten stress."),
        ("Frokost\nsammen", "FamWake planlegger morgenen –\nfra vekking til frokost.")
    ]
}

# Source data for translation
SLIDES_TEMPLATES = [
    {
        "device_file": "main_scrolled.png",
        "layout": "bottom"
    },
    {
        "device_file": "times.png",
        "layout": "bottom"
    },
    {
        "device_file": "alarm.png",
        "layout": "top"
    },
    {
        "device_file": "pause.png",
        "layout": "bottom"
    },
    {
        "device_file": "share.png",
        "layout": "bottom"
    },
    {
        "device_file": "main_full.png",
        "layout": "top"
    }
]

LANGUAGES = [
    "bn", "da", "de", "en", "es", "fr", "hi", "id", "it", "ja", "ko",
    "mr", "nl", "no", "nb", "pl", "pt", "ru", "sv", "tr", "uk", "vi", "zh-CN"
]

LOCALE_MAP = {
    "bn": ["bn-BD"],
    "da": ["da-DK"],
    "de": ["de-DE"],
    "en": ["en-US"],
    "es": ["es-ES", "es-419"],
    "fr": ["fr-FR"],
    "hi": ["hi-IN"],
    "id": ["id-ID"],
    "it": ["it-IT"],
    "ja": ["ja-JP"],
    "ko": ["ko-KR"],
    "mr": ["mr-IN"],
    "nl": ["nl-NL"],
    "no": ["no-NO", "nb-NO"],
    "nb": ["no-NO", "nb-NO"],
    "pl": ["pl-PL"],
    "pt": ["pt-PT", "pt-BR"],
    "ru": ["ru-RU"],
    "sv": ["sv-SE"],
    "tr": ["tr-TR"],
    "uk": ["uk"],
    "vi": ["vi"],
    "zh-CN": ["zh-CN"]
}


def translate_text(text, target_lang):
    if not text.strip(): return text
    try:
        translated = GoogleTranslator(source='en', target=target_lang).translate(text.replace("\n", " ||| "))
        res = translated.replace(" ||| ", "\n").replace("|||", "\n").replace(" || | ", "\n")
        return "\n".join([line.strip() for line in res.split("\n")])
    except Exception as e:
        print(f"Translation error to {target_lang}: {e}. Fallback to English.")
        return text

def fit_background(bg_path, target_size):
    bg = Image.open(bg_path)
    bg_w, bg_h = bg.size
    tgt_w, tgt_h = target_size
    
    ratio = max(tgt_w / bg_w, tgt_h / bg_h)
    new_w = int(bg_w * ratio)
    new_h = int(bg_h * ratio)
    bg_resized = bg.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    left = (new_w - tgt_w) // 2
    top = (new_h - tgt_h) // 2
    right = left + tgt_w
    bottom = top + tgt_h
    return bg_resized.crop((left, top, right, bottom))

def draw_bottom_vignette(img):
    w, h = TARGET_SIZE
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    
    start_y = 1200
    end_y = h
    for y in range(start_y, end_y):
        if y >= h: break
        alpha = int(170 * ((y - start_y) / (end_y - start_y)))
        draw.line([(0, y), (w, y)], fill=(11, 17, 30, alpha))
        
    return Image.alpha_composite(img.convert("RGBA"), overlay)

def draw_android_status_bar(draw, clock_font, fill_color):
    draw.text((80, 68), "10:00", font=clock_font, fill=fill_color)
    draw.rectangle([1055, 70, 1070, 86], outline=fill_color, width=2)
    draw.rectangle([1059, 66, 1066, 70], fill=fill_color)
    draw.rectangle([1058, 73, 1067, 83], fill=fill_color)
    draw.polygon([(1010, 86), (1020, 70), (1030, 86)], fill=fill_color)
    draw.polygon([(970, 86), (985, 70), (985, 86)], fill=fill_color)

def create_android_mockup(screen_img, font_bold):
    bezel_w, bezel_h = 1130, 2295
    mock = Image.new("RGBA", (bezel_w, bezel_h), (0, 0, 0, 0))
    ss_resized = screen_img.resize((1080, 2245), Image.Resampling.LANCZOS).convert("RGBA")
    
    draw = ImageDraw.Draw(mock)
    draw.rounded_rectangle([0, 0, bezel_w, bezel_h], radius=60, fill="#2C2C2C")
    draw.rounded_rectangle([4, 4, bezel_w-4, bezel_h-4], radius=56, fill="#000000")
    
    mock.paste(ss_resized, (25, 25), ss_resized)
    screen_draw = ImageDraw.Draw(mock)
    
    try:
        r, g, b = ss_resized.getpixel((50, 50))[:3]
    except Exception:
        r, g, b = 15, 23, 42
        
    brightness = 0.299 * r + 0.587 * g + 0.114 * b
    is_dark = brightness < 128
    
    bg_color = (r, g, b, 255)
    screen_draw.rectangle([25, 25, 1105, 125], fill=bg_color)
    
    fill_color = "#FFFFFF" if is_dark else "#000000"
    draw_android_status_bar(screen_draw, font_bold, fill_color)
    screen_draw.ellipse([565 - 16, 65 - 16, 565 + 16, 65 + 16], fill="#000000")
    
    try:
        bot_r, bot_g, bot_b = ss_resized.getpixel((540, 2220))[:3]
        bot_brightness = 0.299 * bot_r + 0.587 * bot_g + 0.114 * bot_b
        nav_color = "#FFFFFF" if bot_brightness < 128 else "#000000"
    except Exception:
        nav_color = "#FFFFFF"
        
    screen_draw.rounded_rectangle([565 - 140, 2257, 565 + 140, 2261], radius=2, fill=nav_color)
    return mock

def get_text_width(text, font, draw):
    if not text: return 0
    bbox = draw.textbbox((0, 0), text, font=font)
    return bbox[2] - bbox[0]

def wrap_text(text, font, max_width, draw):
    paragraphs = text.split("\n")
    wrapped_lines = []
    
    for para in paragraphs:
        if not para.strip():
            wrapped_lines.append("")
            continue
            
        is_cjk = any(ord(char) > 0x3000 or (0xAC00 <= ord(char) <= 0xD7A3) for char in para)
        
        if is_cjk:
            current_line = ""
            for char in para:
                w = get_text_width(current_line + char, font, draw)
                if w <= max_width:
                    current_line += char
                else:
                    wrapped_lines.append(current_line)
                    current_line = char
            if current_line:
                wrapped_lines.append(current_line)
        else:
            words = para.split(" ")
            current_line = ""
            for word in words:
                test_line = (current_line + " " + word).strip()
                w = get_text_width(test_line, font, draw)
                if w <= max_width:
                    current_line = test_line
                else:
                    wrapped_lines.append(current_line)
                    current_line = word
            if current_line:
                wrapped_lines.append(current_line)
                
    return "\n".join(wrapped_lines)

def build_screenshot(slide, lang, target_size):
    img = fit_background(BACKGROUND_PATH, target_size)
    
    layout = slide["layout"]
    if layout == "top":
        img = draw_bottom_vignette(img)
        
    draw = ImageDraw.Draw(img)
    
    h_size = 95
    d_size = 44
    
    def load_font(h_sz, d_sz):
        if lang in ("ja", "zh-CN"):
            fh = ImageFont.truetype(SYSTEM_JAPANESE, h_sz, index=2)
            fd = ImageFont.truetype(SYSTEM_JAPANESE, d_sz, index=0)
            fs = ImageFont.truetype(SYSTEM_JAPANESE, 34, index=2)
        elif lang == "ko":
            fh = ImageFont.truetype(SYSTEM_KOREAN, h_sz, index=6)
            fd = ImageFont.truetype(SYSTEM_KOREAN, d_sz, index=0)
            fs = ImageFont.truetype(SYSTEM_KOREAN, 34, index=6)
        elif lang in ("hi", "mr"):
            fh = ImageFont.truetype(SYSTEM_DEVANAGARI, h_sz, index=1)
            fd = ImageFont.truetype(SYSTEM_DEVANAGARI, d_sz, index=0)
            fs = ImageFont.truetype(SYSTEM_DEVANAGARI, 34, index=1)
        elif lang == "bn":
            fh = ImageFont.truetype(SYSTEM_BENGALI, h_sz, index=1)
            fd = ImageFont.truetype(SYSTEM_BENGALI, d_sz, index=0)
            fs = ImageFont.truetype(SYSTEM_BENGALI, 34, index=1)
        elif lang in ("ru", "uk", "pl", "tr", "vi"):
            fh = ImageFont.truetype(SYSTEM_HELVETICA, h_sz, index=1)
            fd = ImageFont.truetype(SYSTEM_HELVETICA, d_sz, index=0)
            fs = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)
        else:
            try:
                fh = ImageFont.truetype(FONT_BOLD_PATH, h_sz)
                fd = ImageFont.truetype(FONT_REGULAR_PATH, d_sz)
                fs = ImageFont.truetype(FONT_BOLD_PATH, 34)
            except Exception:
                fh = ImageFont.truetype(SYSTEM_HELVETICA, h_sz, index=1)
                fd = ImageFont.truetype(SYSTEM_HELVETICA, d_sz, index=0)
                fs = ImageFont.truetype(SYSTEM_HELVETICA, 34, index=1)
        return fh, fd, fs

    font_h, font_d, font_status = load_font(h_size, d_size)

    device_file = slide["device_file"]
    
    device_path = os.path.join(DEVICES_DIR, lang, device_file)
    if not os.path.exists(device_path):
        device_path = os.path.join(DEVICES_DIR, "en", device_file)
        if not os.path.exists(device_path):
            print(f"Device file {device_file} not found, skipping!")
            return None

    with Image.open(device_path) as dev_img:
        w, h = dev_img.size
        if dev_img.size == (1187, 2513):
            screen_img = dev_img.crop(SCREEN_CROP_BOX)
        elif w == 1290 and h == 2796:
            screen_img = dev_img.crop((0, 0, 1290, 2796 - 60))
        elif w == 1242 and h == 2688:
            screen_img = dev_img.crop((0, 0, 1242, 2688 - 50))
        else:
            screen_img = dev_img.crop((0, 0, w, int(h * 0.98)))
        
    phone = create_android_mockup(screen_img, font_status)
    
    phone_tgt_w = 880
    phone_ratio = phone.width / phone.height
    phone_tgt_h = int(phone_tgt_w / phone_ratio)
    phone = phone.resize((phone_tgt_w, phone_tgt_h), Image.Resampling.LANCZOS)
    
    shadow_blur_radius = 24
    shadow = Image.new('RGBA', phone.size, (0, 0, 0, 0))
    alpha = phone.split()[3]
    shadow.paste((0, 0, 0, 120), mask=alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(shadow_blur_radius))
    
    max_text_width = 920
    
    test_hl = wrap_text(slide["headline"], font_h, max_text_width, draw)
    if len(test_hl.split("\n")) > 2:
        h_size = int(h_size * 0.82)
        font_h, font_d, font_status = load_font(h_size, d_size)

    wrapped_headline = wrap_text(slide["headline"], font_h, max_text_width, draw)
    wrapped_desc = wrap_text(slide["description"], font_d, max_text_width, draw)
    
    line_spacing = 10
    if lang in ("ja", "zh-CN", "ko"):
        line_spacing = -22
    elif lang in ("hi", "mr", "bn"):
        line_spacing = 0

    text_x = 80
    
    if layout == "bottom":
        text_y = 120
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h, spacing=line_spacing)
        desc_y = bbox_h[3] + 30
        bbox_d = draw.textbbox((text_x, desc_y), wrapped_desc, font=font_d, spacing=line_spacing)
        
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=line_spacing)
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=line_spacing)
        
        paste_x = (1080 - phone.width) // 2
        min_paste_y = bbox_d[3] + 30
        paste_y = max(630, min_paste_y)
        
    else:  # layout == "top"
        paste_x = (1080 - phone.width) // 2
        paste_y = -470
        
        text_y = 1440
        bbox_h = draw.textbbox((text_x, text_y), wrapped_headline, font=font_h, spacing=line_spacing)
        desc_y = bbox_h[3] + 30
        bbox_d = draw.textbbox((text_x, desc_y), wrapped_desc, font=font_d, spacing=line_spacing)
        
        if bbox_d[3] > 1920 - 35:
            overflow = bbox_d[3] - (1920 - 35)
            text_y -= overflow
            desc_y -= overflow
            
        draw.text((text_x, text_y), wrapped_headline, font=font_h, fill=ACCENT_COLOR, spacing=line_spacing)
        draw.text((text_x, desc_y), wrapped_desc, font=font_d, fill=TEXT_COLOR, spacing=line_spacing)
        
    shadow_offset = (0, 15)
    img.paste(shadow, (paste_x + shadow_offset[0], paste_y + shadow_offset[1]), shadow)
    img.paste(phone, (paste_x, paste_y), phone)
    
    if img.mode == 'RGBA':
        img = img.convert('RGB')
        
    return img

def main():
    android_metadata_dir = os.path.join(PROJECT_ROOT, "android/fastlane/metadata/android")
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    for lang in LANGUAGES:
        if lang not in LOCALE_MAP:
            print(f"Skipping Android screenshot generation for '{lang}' (not mapped)")
            continue
            
        lang_output_dir = os.path.join(OUTPUT_DIR, lang)
        os.makedirs(lang_output_dir, exist_ok=True)
        
        if lang in VERIFIED_SS:
            slides_text = VERIFIED_SS[lang]
        else:
            print(f"Translating texts for language '{lang}' dynamically...")
            slides_text = []
            for hl, tx in VERIFIED_SS["en"]:
                t_hl = translate_text(hl, lang)
                t_tx = translate_text(tx, lang)
                slides_text.append((t_hl, t_tx))
                time.sleep(0.15)
                
        slides = []
        for idx, temp in enumerate(SLIDES_TEMPLATES):
            slides.append({
                "headline": slides_text[idx][0],
                "description": slides_text[idx][1],
                "device_file": temp["device_file"],
                "layout": temp["layout"]
            })
            
        target_locales = LOCALE_MAP[lang]
        print(f"Generating Android screenshots for '{lang}'...")
        
        for idx, slide in enumerate(slides):
            img = build_screenshot(slide, lang, TARGET_SIZE)
            if img:
                filename = f"screenshot_{idx+1}_{lang}.jpg"
                save_path = os.path.join(lang_output_dir, filename)
                img.save(save_path, "JPEG", quality=93)
                
                for locale in target_locales:
                    fastlane_lang_dir = os.path.join(android_metadata_dir, locale, "images", "phoneScreenshots")
                    os.makedirs(fastlane_lang_dir, exist_ok=True)
                    fastlane_filename = f"0{idx+1}_Slide.png"
                    fastlane_save_path = os.path.join(fastlane_lang_dir, fastlane_filename)
                    img.save(fastlane_save_path, "PNG")
                    
        print(f"  Done Android '{lang}'")

if __name__ == "__main__":
    main()
