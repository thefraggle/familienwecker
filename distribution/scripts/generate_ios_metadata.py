"""
Generate multi-language App Store Connect metadata in Fastlane format.

Generates three files per locale:
  - release_notes.txt    (What's New — from changelogs, translated on-the-fly)
  - description.txt      (App description — from existing Play Store listings)
  - promotional_text.txt (Promotional text — translated from DE/EN source)

Note: App Store Connect rejects emoji characters in all metadata fields.
"""
import os
import re


def truncate_to_bytes(text, max_bytes=4000, suffix="..."):
    """App Store Connect counts UTF-8 bytes. Max 4000 per field."""
    encoded = text.encode('utf-8')
    if len(encoded) <= max_bytes:
        return text
    suffix_bytes = suffix.encode('utf-8')
    truncated = encoded[:max_bytes - len(suffix_bytes)]
    return truncated.decode('utf-8', errors='ignore') + suffix


def strip_emojis(text):
    """Remove all characters that App Store Connect rejects.
    Uses Unicode category whitelist instead of emoji blacklist regex,
    because Apple's rejection list is unpredictable and keeps growing."""
    import unicodedata
    cleaned = []
    for ch in text:
        cat = unicodedata.category(ch)
        # Exclude variation selectors (U+FE00-FE0F) — emoji modifiers with Mn category
        if '\uFE00' <= ch <= '\uFE0F':
            continue
        # Keep: Letters (L*), Numbers (N*), Punctuation (P*),
        # Separators/spaces (Zs), diacritics (Mn, Mc), currency (Sc),
        # math symbols (Sm), modifier symbols (Sk), and whitespace
        if cat[0] in ('L', 'N', 'P') or cat in ('Zs', 'Mn', 'Mc', 'Sc', 'Sm', 'Sk'):
            cleaned.append(ch)
        elif ch in '\n\r\t ':
            cleaned.append(ch)
        # Skip everything else: So (other symbols = emoji), Me (enclosing marks),
        # Cf (format chars like ZWJ), Co (private use), Cn (unassigned)
    return ''.join(cleaned)


def strip_html(text):
    """Remove HTML tags — App Store uses plain text, not HTML like Play Store."""
    return re.sub(r'<[^>]+>', '', text)


def safe_read_file(file_path):
    """Safely validate, canonicalize and read file content."""
    real_path = os.path.realpath(file_path)
    if not os.path.isfile(real_path):
        return None
    with open(real_path, 'r', encoding='utf-8') as f:
        return f.read()


def get_latest_changelog(file_path):
    """Extract the first version block from a Markdown changelog."""
    content = safe_read_file(file_path)
    if not content:
        return None

    match = re.search(r'## [\d.]+.*?\n(.*?)(?=\n## \d|$)', content, re.DOTALL)
    if not match:
        return None

    lines = match.group(1).strip().split('\n')
    cleaned = []
    for line in lines:
        line = line.strip()
        if not line or line.startswith('###'):
            continue
        # Strip Markdown formatting & bullets
        line = re.sub(r'(\*\*|\*|__|_)', '', line)
        line = re.sub(r'^[•\-\*\s]+', '', line)
        # Skip section headers like "✨ Improved:" or "Verbessert:"
        if re.match(r'^(✨\s*)?(Improved|Verbessert)\s*:\s*$', line, flags=re.IGNORECASE):
            continue
        if line:
            cleaned.append(line)

    return "\n".join(f"• {item}" for item in cleaned) if cleaned else None


def get_description_from_listing(listing_path):
    """Extract the full description from a Play Store listing markdown file.
    The description is always the 3rd ## section in the file
    (after app name and short description)."""
    content = safe_read_file(listing_path)
    if not content:
        return None

    # Find all ## section positions
    sections = list(re.finditer(r'^## .+', content, re.MULTILINE))
    if len(sections) < 3:
        return None

    # Description starts after the 3rd ## header
    desc_start = sections[2].end()
    # Description starts after the 3rd ## header
    desc_end = sections[3].start() if len(sections) > 3 else len(content)
    desc = content[desc_start:desc_end].strip()
    app_store_split = re.search(r'\n---+\s*\n#+ ', desc)
    if app_store_split:
        desc = desc[:app_store_split.start()].strip()

    return desc if desc else None


def extract_ios_sections(listing_path):
    """
    Extract App Store metadata from listing markdown file.
    Expects sections like:
    ## iOS App-Name (or Title)
    ## iOS Untertitel (or Subtitle)
    ## iOS Promo-Text (or Promotional Text)
    ## iOS Keywords (or Keywords)
    """
    content = safe_read_file(listing_path)
    if not content:
        return None, None, None, None

    # Find the App Store header or split there
    parts = re.split(r'#+ 🍎 App Store Listing', content)
    if len(parts) < 2:
        return None, None, None, None

    ios_content = parts[1]
    sections = list(re.finditer(r'^## .+', ios_content, re.MULTILINE))
    if len(sections) < 4:
        return None, None, None, None

    # Title is between 1st and 2nd heading
    title = ios_content[sections[0].end():sections[1].start()].strip()
    # Subtitle is between 2nd and 3rd heading
    subtitle = ios_content[sections[1].end():sections[2].start()].strip()
    # Promo is between 3rd and 4th heading
    promo = ios_content[sections[2].end():sections[3].start()].strip()
    # Keywords is from 4th heading to end of file or next heading
    keywords = ios_content[sections[3].end():].strip()

    return title, subtitle, promo, keywords


# Mapping: ASC locale → Android Fastlane locale directory
ASC_TO_ANDROID_LOCALE = {
    'de-DE':   'de-DE',
    'en-US':   'en-US',
    'en-GB':   'en-GB',
    'fr-FR':   'fr-FR',
    'es-ES':   'es-ES',
    'es-MX':   'es-419',
    'pt-BR':   'pt-BR',
    'pt-PT':   'pt-PT',
    'it':      'it-IT',
    'nl-NL':   'nl-NL',
    'ja':      'ja-JP',
    'ko':      'ko-KR',
    'zh-Hans': 'zh-CN',
    'ru':      'ru-RU',
    'tr':      'tr-TR',
    'pl':      'pl-PL',
    'sv':      'sv-SE',
    'da':      'da-DK',
    'no':      'no-NO',
    'id':      'id',
    'vi':      'vi',
    'hi':      'hi-IN',
    'uk':      'uk',
    'bn-BD':   'bn-BD',
    'mr-IN':   'mr-IN',
}

# Mapping: ASC locale → Play Store listing file key
ASC_TO_LISTING = {
    'de-DE':   'de',
    'en-US':   'en',
    'en-GB':   'en',
    'fr-FR':   'fr',
    'es-ES':   'es',
    'es-MX':   'es',
    'pt-BR':   'pt',
    'pt-PT':   'pt',
    'it':      'it',
    'nl-NL':   'nl',
    'ja':      'ja',
    'ko':      'ko',
    'zh-Hans': 'zh-CN',
    'ru':      'ru',
    'tr':      'tr',
    'pl':      'pl',
    'sv':      'sv',
    'da':      'da',
    'no':      'no',
    'id':      'id',
    'vi':      'vi',
    'hi':      'hi',
    'uk':      'uk',
    'bn-BD':   'bn',
    'mr-IN':   'mr',
}

# Changelog source files per locale (only DE/EN have dedicated files)
CHANGELOG_SOURCES = {
    'de-DE': 'docs/CHANGELOG.md',
    'en-US': 'docs/CHANGELOG.en.md',
    'en-GB': 'docs/CHANGELOG.en.md',
}

# Translation target codes for deep-translator
TRANSLATION_TARGETS = {
    'de-DE': 'de', 'en-US': 'en', 'en-GB': 'en',
    'fr-FR': 'fr', 'es-ES': 'es', 'es-MX': 'es',
    'pt-BR': 'pt', 'pt-PT': 'pt', 'it': 'it',
    'nl-NL': 'nl', 'ja': 'ja', 'ko': 'ko',
    'zh-Hans': 'zh-CN', 'ru': 'ru', 'tr': 'tr',
    'pl': 'pl', 'sv': 'sv', 'da': 'da',
    'no': 'no', 'id': 'id', 'vi': 'vi',
    'hi':      'hi', 'uk': 'uk',
    'bn-BD':   'bn', 'mr-IN': 'mr',
}

# Hardcoded promotional texts per locale — verified and locked in.
# Update these manually when the promo message changes.
PROMO_TEXTS = {
    'de-DE':   'Schluss mit Morgenchaos! FamWake koordiniert Weckzeiten, Badezimmer-Reihenfolge und Frühstück für die ganze Familie.',
    'en-US':   'No more morning chaos! FamWake coordinates wake-up times, bathroom turns, and breakfast for the whole family.',
    'en-GB':   'No more morning chaos! FamWake coordinates wake-up times, bathroom turns, and breakfast for the whole family.',
    'fr-FR':   'Fini le chaos matinal ! FamWake coordonne les heures de réveil, l’ordre de passage à la salle de bain et le petit-déjeuner pour toute la famille.',
    'es-ES':   '¡No más caos matutino! FamWake coordina los horarios de despertarse, ir al baño y desayunar para toda la familia.',
    'es-MX':   '¡No más caos matutino! FamWake coordina los horarios de despertarse, ir al baño y desayunar para toda la familia.',
    'pt-BR':   'Chega de caos matinal! FamWake coordena horários de despertar, horários de ir ao banheiro e café da manhã para toda a família.',
    'pt-PT':   'Chega de caos matinal! FamWake coordena horários de despertar, horários de ir ao banheiro e café da manhã para toda a família.',
    'it':      'Niente più caos mattutino! FamWake coordina gli orari di sveglia, i turni del bagno e la colazione per tutta la famiglia.',
    'nl-NL':   'Geen ochtendchaos meer! FamWake coördineert de wektijden, de badkamerbeurten en het ontbijt voor het hele gezin.',
    'ja':      '朝の混乱はもうありません！ FamWake は、家族全員の起床時間、バスルームの順番、朝食を調整します。',
    'ko':      '더 이상 아침의 혼란은 없습니다! FamWake는 온 가족의 기상 시간, 욕실 사용 순서 및 아침 식사를 조율합니다.',
    'zh-Hans': '告别早晨的混乱！FamWake 协调全家人的起床时间、洗漱顺序和早餐。',
    'ru':      'Больше никакого утреннего хаоса! FamWake координирует время пробуждения, очередь в ванную и завтрак для всей семьи.',
    'tr':      'Artık sabah kaosuna son! FamWake tüm aile için uyanma saatlerini, banyo sırasını ve kahvaltıyı koordine eder.',
    'pl':      'Nigdy więcej porannego chaosu! FamWake koordynuje godziny pobudek, kolejność w łazience i śniadanie dla całej rodziny.',
    'sv':      'Inget mer morgonkaos! FamWake koordinerar väckningstider, badrumsturer och frukost för hela familjen.',
    'da':      'Slut med morgenkaos! FamWake koordinerer vækketider, badeværelsesture og morgenmad for hele familien.',
    'no':      'Slutt på morgenkaos! FamWake koordinerer vekketider, baderomsturer og frokost for hele familien.',
    'id':      'Tidak ada lagi kekacauan pagi hari! FamWake mengoordinasikan waktu bangun, pergantian kamar mandi, dan sarapan untuk seluruh keluarga.',
    'vi':      'Không còn sự hỗn loạn buổi sáng! FamWake điều phối thời gian thức dậy, thứ tự dùng phòng tắm và bữa sáng cho cả gia đình.',
    'hi':      'अब सुबह की अव्यवस्था नहीं! फैमवेक पूरे परिवार के लिए जागने के समय, बाथरूम जाने और नाश्ते के समय का समन्वय करता है।',
    'uk':      'Більше ніякого ранкового хаосу! FamWake координує час пробудження, чергу у ванну та сніданок для всієї родини.',
    'bn-BD':   'আর সকালের বিশৃঙ্খলা নয়! FamWake পুরো পরিবারের জন্য ঘুম থেকে ওঠার সময়, বাথরুমের পালা এবং প্রাতঃরাশের সমন্বয় করে।',
    'mr-IN':   'आणखी सकाळचा गोंधळ नाही! FamWake संपूर्ण कुटुंबासाठी उठण्याची वेळ, बाथरूमची वेळ आणि नाश्ता यांचे संयोजन करते.',
}

# App Store keywords: max 100 chars, comma-separated, no spaces after commas
# Deduplicated against Title & Subtitle, policy-compliant (no price words like "free"/"kostenlos")
KEYWORDS_DE = 'kinderwecker,badplaner,zeitplan,timer,kinder,adhs,visuell,mission,aufgaben,sanft,tagesplan,kids,uhr'
KEYWORDS_EN = 'checklist,visual,first,then,autism,chore,habit,adhd,planner,daily,kids,group,shared,timer,morning'

# Hardcoded optimized keywords per locale (ASO KW 43)
KEYWORDS_BY_LOCALE = {
    'de-DE':   'kinderwecker,badplaner,zeitplan,timer,kinder,adhs,visuell,mission,aufgaben,sanft,tagesplan,kids,uhr',
    'en-US':   'checklist,visual,first,then,autism,chore,habit,adhd,planner,daily,kids,group,shared,timer,morning',
    'en-GB':   'checklist,visual,first,then,autism,chore,habit,adhd,planner,daily,kids,group,shared,timer,morning',
    'fr-FR':   'enfant,matin,routine,salle,bain,horaire,famille,partagé,groupe,visuel,minuteur,tdah,autisme,habitude',
    'es-ES':   'despertador,niños,infantil,rutina,mañana,baño,turnos,horario,visual,tdah,autismo,hábitos,alarma,app',
    'es-MX':   'despertador,niños,infantil,rutina,mañana,baño,turnos,horario,visual,tdah,autismo,hábitos,alarma,app',
    'pt-BR':   'despertador,infantil,crianças,rotina,manhã,banheiro,horário,visual,timer,tdah,autismo,hábitos,alarme',
    'pt-PT':   'despertador,infantil,crianças,rotina,manhã,banheiro,horário,visual,timer,tdah,autismo,hábitos,alarme',
    'it':   'bambini,routine,mattina,bagno,turni,orario,condivisa,visivo,timer,adhd,autismo,abitudini,sveglia',
    'nl-NL':   'kinderwekker,ochtendroutine,kinderen,badkamer,tijdschema,gedeeld,visueel,timer,adhd,autisme,taken',
    'pl':   'dzieci,poranna,rutyna,łazienka,harmonogram,wspólny,wizualny,minutnik,adhd,autyzm,zadania,nawyki,plan',
    'tr':   'çalar,saat,çocuk,rutin,banyo,sırası,program,görsel,zamanlayıcı,dehb,otizm,görev,alışkanlık,aile,plan',
    'ru':   'дети,детский,утро,рутина,ванная,очередь,расписание,таймер,визуальный,сдвг,аутизм,привычки,задачи,сон',
    'uk':   'діти,дитячий,ранок,рутина,ванна,черга,розклад,таймер,візуальний,рдвг,аутизм,звички,завдання,годинник',
    'sv':   'väckarklocka,barn,morgonrutin,badrum,schema,delad,visuell,timer,adhd,autism,uppgifter,vanor,familj',
    'da':   'vækkeur,børnevækkeur,børn,morgenrutine,badeværelse,tidsplan,visuel,timer,adhd,autisme,opgaver,vaner',
    'no':   'vekkerklokke,barnevekkerur,barn,morgenrutine,baderom,tidsplan,visuell,timer,adhd,autisme,oppgaver',
    'id':   'jam,anak,rutinitas,pagi,kamar,mandi,giliran,jadwal,bersama,visual,timer,adhd,autisme,tugas,kebiasaan',
    'vi':   'trẻ,em,lịch,trình,buổi,sáng,phòng,tắm,thời,gian,biểu,chung,hẹn,giờ,adhd,tự,kỷ,thói,quen,báo,thức',
    'ja':   '目覚まし時計,アラーム,ファミリー,子供用,朝のルーティン,スケジュール,起床時間,洗面所,共有,習慣,タイマー,スヌーズ,時間管理,生活リズム,ADHD,自閉症,視覚支援,朝活,時計,予定',
    'ko':   '알람시계,어린이,루틴,욕실,스누즈,일정,모닝콜,시간표,기상,습관,타이머,시간관리,탁상시계,기상시간,모닝루틴,공유,ADHD,자폐,시각타이머,아침루틴,스케줄,가족,알람',
    'zh-Hans':   '儿童闹钟,早晨,常规,浴室,洗漱,重响,日程表,习惯,计时器,起床,提醒,时间管理,共享,早起,生活习惯,作息时间,视觉计时器,多动症,自闭症,晨间例程,家庭闹钟,计划表,儿童时间',
    'hi':   'बच्चे,सुबह,रूटीन,बाथरूम,बारी,समय,सारणी,साझा,ग्रुप,विजुअल,टाइमर,आदत,टास्क,घड़ी,नाश्ता,ऑटिज्म,अलार्म',
    'bn-BD':   'শিশু,সকাল,রুটিন,বাথরুম,পালা,সময়সূচী,শেয়ার,গ্রুপ,ভিজ্যুয়াল,টাইমার,অভ্যাস,টাস্ক,অটিজম,অ্যালার্ম',
    'mr-IN':   'मुले,सकाळ,रुटीन,बाथरूम,वेळ,वेळापत्रक,सामायिक,ग्रुप,व्हिज्युअल,टायमर,सवय,टास्क,नाश्ता,ऑटिझम,अलार्म',
}

# App Store subtitle: max 30 chars, shown below app name.
# Hardcoded per locale for consistency.
SUBTITLES = {
    'de-DE':   'Familienwecker & Morgenroutine',
    'en-US':   'Bathroom Schedule & Routine',
    'en-GB':   'Bathroom Schedule & Routine',
    'fr-FR':   'R\u00e9veil familial & planning',
    'es-ES':   'Alarma familiar & planificador',
    'es-MX':   'Alarma familiar & planificador',
    'pt-BR':   'Alarme familiar & planejador',
    'pt-PT':   'Alarme familiar & planejador',
    'it':      'Sveglia famiglia & pianifica',
    'nl-NL':   'Gezinswekker & ochtendplanner',
    'ja':      '家族目覚まし＆朝のプランナー',
    'ko':      '가족 알람 및 아침 플래너',
    'zh-Hans': '家庭智能闹钟 & 早晨日程规划',
    'ru':      'Семейный будильник & планер',
    'tr':      'Aile alarm\u0131 & sabah planlay\u0131c\u0131',
    'pl':      'Budzik rodzinny & planer',
    'sv':      'Familjev\u00e4ckare & morgonplan',
    'da':      'Familiev\u00e6kker & morgenplan',
    'no':      'Familievekker & morgenplan',
    'id':      'Alarm keluarga & perencana',
    'vi':      'B\u00e1o th\u1ee9c gia \u0111\u00ecnh & k\u1ebf ho\u1ea1ch',
    'hi':      'परिवार अलार्म और प्लानर',
    'uk':      'Сімейний будильник & планер',
    'bn-BD':   'পারিবারিক অ্যালার্ম ও সূচি',
    'mr-IN':   'कौटुंबिक अलार्म आणि नियोजक',
}

def write_file(path, content):
    """Write content to file, creating directories as needed."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)


def main():
    listings_dir = os.path.join('docs', 'internal', 'play_store_listings')
    android_metadata_dir = os.path.join('android', 'fastlane', 'metadata', 'android')
    metadata_base = os.path.join('ios', 'fastlane', 'metadata')
    os.makedirs(metadata_base, exist_ok=True)

    changelog_en = get_latest_changelog('docs/CHANGELOG.en.md') or \
        "Maintenance update and performance improvements."

    # deep-translator for non-DE/EN locales
    try:
        from deep_translator import GoogleTranslator
        translator_available = True
    except ImportError:
        print("⚠️  deep-translator not installed, using localized Fastlane sources")
        translator_available = False

    print("=" * 60)
    print("  Generating App Store Connect metadata (Fastlane format)")
    print("=" * 60)

    for asc_locale in ASC_TO_LISTING:
        locale_dir = os.path.join(metadata_base, asc_locale)
        os.makedirs(locale_dir, exist_ok=True)
        target_lang = TRANSLATION_TARGETS[asc_locale]
        listing_key = ASC_TO_LISTING[asc_locale]
        android_locale = ASC_TO_ANDROID_LOCALE.get(asc_locale, 'en-US')

        # ── 1. RELEASE NOTES ─────────────────────────────────────
        changelog_path = CHANGELOG_SOURCES.get(asc_locale)
        notes = ""
        if changelog_path:
            notes = get_latest_changelog(changelog_path) or ""

        if not notes and translator_available and changelog_en:
            try:
                translated_lines = []
                for line in changelog_en.split("\n"):
                    clean_line = re.sub(r'^[•\-\*\s]+', '', line).strip()
                    if clean_line:
                        translated_text = GoogleTranslator(source='en', target=target_lang).translate(clean_line)
                        translated_lines.append(f"• {translated_text}")
                if translated_lines:
                    notes = "\n".join(translated_lines)
            except Exception as e:
                print(f"  ⚠️  Release notes translation failed for {asc_locale}: {e}")

        if not notes:
            notes = changelog_en

        notes = strip_emojis(notes)
        # Remove label prefixes that remain after emoji stripping
        notes = re.sub(r'^\s*(Improved|Verbessert)\s*:\s*\.?\s*', '', notes, flags=re.IGNORECASE)
        notes = notes.replace("..", ".").strip()
        notes = truncate_to_bytes(notes)
        write_file(os.path.join(locale_dir, 'release_notes.txt'), notes)

        # ── 2. DESCRIPTION ───────────────────────────────────────
        android_desc_file = os.path.join(android_metadata_dir, android_locale, 'full_description.txt')
        listing_file = os.path.join(listings_dir, f'{listing_key}.md')
        desc = safe_read_file(android_desc_file) or get_description_from_listing(listing_file)

        if not desc:
            print(f"  ⚠️  [FALLBACK] Using EN description for {asc_locale}")
            desc = safe_read_file(os.path.join(android_metadata_dir, 'en-US', 'full_description.txt')) or ""

        desc = strip_html(desc)
        desc = strip_emojis(desc)
        # Clean up artifacts from stripping: double spaces, leading spaces on lines
        desc = re.sub(r' {2,}', ' ', desc)
        desc = re.sub(r'\n ', '\n', desc)
        desc = desc.replace("..", ".").strip()
        desc = truncate_to_bytes(desc, max_bytes=3900)
        write_file(os.path.join(locale_dir, 'description.txt'), desc)

        # Try to read iOS metadata from local markdown file
        parsed_title, parsed_subtitle, parsed_promo, parsed_keywords = extract_ios_sections(listing_file)

        # ── 3. PROMOTIONAL TEXT ───────────────────────────────────
        promo = parsed_promo or PROMO_TEXTS.get(asc_locale, PROMO_TEXTS['en-US'])
        promo = strip_emojis(promo).strip()
        if len(promo) > 170:
            promo = promo[:167] + "..."
        write_file(os.path.join(locale_dir, 'promotional_text.txt'), promo)

        # ── 4. KEYWORDS ─────────────────────────────────────────────
        keywords = KEYWORDS_BY_LOCALE.get(asc_locale) or parsed_keywords
        if not keywords:
            if asc_locale.startswith('de'):
                keywords = KEYWORDS_DE
            elif asc_locale.startswith('en'):
                keywords = KEYWORDS_EN
            elif translator_available:
                try:
                    keywords = GoogleTranslator(
                        source='en', target=target_lang
                    ).translate(KEYWORDS_EN)
                except Exception:
                    keywords = KEYWORDS_EN
            else:
                keywords = KEYWORDS_EN

        keywords = strip_emojis(keywords).strip()
        # Clean: remove spaces after commas
        keywords = ','.join([kw.strip() for kw in keywords.split(',') if kw.strip()])
        # App Store limit: 100 characters for keywords
        if len(keywords) > 100:
            keywords = keywords[:100].rsplit(',', 1)[0]
        write_file(os.path.join(locale_dir, 'keywords.txt'), keywords)

        # ── 5. SUBTITLE ────────────────────────────────────────────
        subtitle = parsed_subtitle or SUBTITLES.get(asc_locale, SUBTITLES['en-US'])
        subtitle = strip_emojis(subtitle).strip()
        if len(subtitle) > 30:
            subtitle = subtitle[:27] + "..."
        write_file(os.path.join(locale_dir, 'subtitle.txt'), subtitle)

        # ── 6. SUPPORT & MARKETING URLS ────────────────────────────
        if asc_locale == 'de-DE':
            support_url = "https://familienwecker.de/imprint.html"
            marketing_url = "https://familienwecker.de/index.html"
            web_lang = "de"
        else:
            web_lang = target_lang
            support_url = f"https://familienwecker.de/imprint-{web_lang}.html"
            marketing_url = f"https://familienwecker.de/index-{web_lang}.html"

        write_file(os.path.join(locale_dir, 'support_url.txt'), support_url)
        write_file(os.path.join(locale_dir, 'marketing_url.txt'), marketing_url)

        desc_bytes = len(desc.encode('utf-8'))
        notes_bytes = len(notes.encode('utf-8'))
        promo_len = len(promo)
        kw_len = len(keywords)
        sub_len = len(subtitle)
        print(f"  ✅ {asc_locale:10s} | notes: {notes_bytes:4d}B | desc: {desc_bytes:4d}B | promo: {promo_len:3d}/170 | kw: {kw_len:3d}/100 | sub: {sub_len:2d}/30 | urls: {web_lang}")

    print(f"\n✅ {len(ASC_TO_LISTING)} locales generated in {metadata_base}/")
    print("   Files per locale: release_notes.txt, description.txt, promotional_text.txt, keywords.txt, subtitle.txt, support_url.txt, marketing_url.txt")


if __name__ == "__main__":
    main()
