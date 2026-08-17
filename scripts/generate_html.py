import os
import re
import glob

def generate_report():
    project_root = "/Users/daniel.notthoff/GIT_Repos/_privat/familienwecker"
    ios_base_dir = os.path.join(project_root, "ios", "fastlane", "screenshots")
    android_base_dir = os.path.join(project_root, "android", "fastlane", "metadata", "android")
    html_file = os.path.join(ios_base_dir, "screenshots.html")
    
    # Nested dictionary to hold parsed images
    # Format: data[lang][platform][device][screen][appearance] = relative_path_to_html_dir
    data = {}
    
    # 1. Parse iOS Screenshots
    ios_pattern = os.path.join(ios_base_dir, "**", "*.png")
    ios_files = glob.glob(ios_pattern, recursive=True)
    ios_regex = re.compile(r"([^/]+)-([0-9]+)(?:_[A-Za-z_]+)?_(Light|Dark|Slide)\.png$")
    
    for filepath in ios_files:
        rel_path = os.path.relpath(filepath, ios_base_dir)
        parts = rel_path.split(os.sep)
        if len(parts) < 2 or filepath == html_file:
            continue
        lang_raw = parts[0]
        filename = parts[-1]
        
        match = ios_regex.match(filename)
        if not match:
            continue
        
        device, screen, appearance = match.groups()
        lang = lang_raw.split("-")[0].lower()
        if lang == "no": lang = "nb"
        
        if lang not in data:
            data[lang] = {}
        if "ios" not in data[lang]:
            data[lang]["ios"] = {}
        if device not in data[lang]["ios"]:
            data[lang]["ios"][device] = {}
        if screen not in data[lang]["ios"][device]:
            data[lang]["ios"][device][screen] = {}
            
        data[lang]["ios"][device][screen][appearance] = rel_path

    # 2. Parse Android Screenshots
    android_pattern = os.path.join(android_base_dir, "**", "*.png")
    android_files = glob.glob(android_pattern, recursive=True)
    android_regex = re.compile(r"([0-9]+)(?:_[A-Za-z_]+)?_(Light|Dark|Slide)\.png$")
    
    for filepath in android_files:
        rel_path = "../../../android/fastlane/metadata/android/" + os.path.relpath(filepath, android_base_dir)
        parts = os.path.relpath(filepath, android_base_dir).split(os.sep)
        if len(parts) < 2:
            continue
        lang_raw = parts[0]
        filename = parts[-1]
        
        match = android_regex.match(filename)
        if not match:
            continue
        
        screen, appearance = match.groups()
        lang = lang_raw.split("-")[0].lower()
        if lang == "no": lang = "nb"
        device = "Android Phone"
        
        if lang not in data:
            data[lang] = {}
        if "android" not in data[lang]:
            data[lang]["android"] = {}
        if device not in data[lang]["android"]:
            data[lang]["android"][device] = {}
        if screen not in data[lang]["android"][device]:
            data[lang]["android"][device][screen] = {}
            
        data[lang]["android"][device][screen][appearance] = rel_path

    # 3. Parse Android Feature Graphics
    fg_base_dir = os.path.join(project_root, "docs", "internal", "images", "feature_graphics")
    fg_pattern = os.path.join(fg_base_dir, "*.png")
    fg_files = glob.glob(fg_pattern)
    
    for filepath in fg_files:
        filename = os.path.basename(filepath)
        if not filename.startswith("feature_graphic_"):
            continue
        lang = filename.replace("feature_graphic_", "").replace(".png", "").lower()
        if lang == "no": lang = "nb"
        
        rel_path = "../../../docs/internal/images/feature_graphics/" + filename
        device = "Android Play Store"
        screen = "01_Feature Graphic"
        
        if lang not in data:
            data[lang] = {}
        if "feature" not in data[lang]:
            data[lang]["feature"] = {}
        if device not in data[lang]["feature"]:
            data[lang]["feature"][device] = {}
        if screen not in data[lang]["feature"][device]:
            data[lang]["feature"][device][screen] = {}
            
        data[lang]["feature"][device][screen]["Banner"] = rel_path

    languages = sorted(list(data.keys()))
    if not languages:
        print("No screenshots found to generate HTML report.")
        return
        
    html = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FamWake - Screenshots Overview</title>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #0f172a;
            --card-bg: #1e293b;
            --text-color: #f8fafc;
            --accent-color: #6366f1;
            --accent-hover: #4f46e5;
            --border-color: #334155;
        }
        
        * {
            box-sizing: border-box;
            font-family: 'Outfit', sans-serif;
            margin: 0;
            padding: 0;
        }
        
        body {
            background-color: var(--bg-color);
            color: var(--text-color);
            padding: 40px 20px;
            min-height: 100vh;
        }
        
        header {
            max-width: 1400px;
            margin: 0 auto 30px auto;
            text-align: center;
        }
        
        h1 {
            font-size: 2.5rem;
            font-weight: 800;
            background: linear-gradient(135deg, #a5b4fc, #6366f1);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 10px;
        }
        
        p.subtitle {
            color: #94a3b8;
            font-size: 1.1rem;
        }
        
        .controls-container {
            max-width: 1400px;
            margin: 0 auto 30px auto;
            display: flex;
            flex-direction: column;
            gap: 15px;
            align-items: center;
            background-color: var(--card-bg);
            padding: 20px;
            border-radius: 16px;
            border: 1px solid var(--border-color);
        }
        
        .control-row {
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: center;
            align-items: center;
        }

        .control-label {
            font-weight: 600;
            color: #94a3b8;
            margin-right: 8px;
            text-transform: uppercase;
            font-size: 0.85rem;
            letter-spacing: 0.05em;
        }
        
        .btn {
            background-color: #0f172a;
            border: 1px solid var(--border-color);
            color: #94a3b8;
            padding: 8px 16px;
            border-radius: 9999px;
            cursor: pointer;
            font-weight: 600;
            transition: all 0.2s ease;
        }
        
        .btn:hover {
            color: white;
            border-color: var(--accent-color);
        }
        
        .btn.active {
            background-color: var(--accent-color);
            color: white;
            border-color: var(--accent-color);
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
        }
        
        .gallery-container {
            max-width: 1400px;
            margin: 0 auto;
        }
        
        .gallery-section {
            display: none;
            animation: fadeIn 0.3s ease-in-out forwards;
        }
        
        .gallery-section.active {
            display: block;
        }
        
        .device-group {
            margin-bottom: 50px;
        }
        
        .device-title {
            font-size: 1.5rem;
            margin-bottom: 20px;
            border-left: 4px solid var(--accent-color);
            padding-left: 12px;
            font-weight: 600;
            color: #cbd5e1;
        }
        
        .screenshot-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
            gap: 30px;
        }
        
        .screenshot-card {
            background-color: var(--card-bg);
            border-radius: 20px;
            overflow: hidden;
            border: 1px solid var(--border-color);
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
        
        .screenshot-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 24px rgba(0, 0, 0, 0.3);
        }
        
        .card-header {
            padding: 16px;
            background-color: rgba(15, 23, 42, 0.4);
            border-bottom: 1px solid var(--border-color);
            font-weight: 600;
            font-size: 0.95rem;
            color: #94a3b8;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .screen-num {
            background-color: var(--accent-color);
            color: white;
            font-size: 0.8rem;
            padding: 2px 8px;
            border-radius: 9999px;
        }
        
        .card-body {
            padding: 20px;
            display: flex;
            gap: 15px;
            justify-content: center;
            background-color: #0b0f19;
        }
        
        .image-wrapper {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 8px;
            max-width: 160px;
        }
        
        .image-label {
            font-size: 0.8rem;
            color: #64748b;
            font-weight: 600;
        }
        
        .screenshot-img {
            width: 100%;
            border-radius: 10px;
            box-shadow: 0 4px 10px rgba(0, 0, 0, 0.4);
            cursor: pointer;
            transition: filter 0.2s ease;
        }
        
        .screenshot-img:hover {
            filter: brightness(1.1);
        }
        
        /* Modal for fullscreen view */
        .modal {
            display: none;
            position: fixed;
            z-index: 1000;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: rgba(15, 23, 42, 0.95);
            backdrop-filter: blur(10px);
            justify-content: center;
            align-items: center;
            cursor: zoom-out;
        }
        
        .modal-content {
            max-height: 90vh;
            max-width: 90vw;
            border-radius: 16px;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
            animation: zoomIn 0.25s ease-out;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        @keyframes zoomIn {
            from { transform: scale(0.9); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
        }
    </style>
</head>
<body>

    <header>
        <h1>FamWake Screenshots Overview</h1>
        <p class="subtitle">Lokalisierte App Store & Play Store Assets für alle 22 Sprachen</p>
    </header>

    <div class="controls-container">
        <div class="control-row">
            <span class="control-label">Asset-Typ:</span>
            <button class="btn plat-btn active" onclick="setPlatform('ios')">iOS Screenshots</button>
            <button class="btn plat-btn" onclick="setPlatform('android')">Android Screenshots</button>
            <button class="btn plat-btn" onclick="setPlatform('feature')">Feature Graphics</button>
        </div>
        <div class="control-row">
            <span class="control-label">Sprache:</span>
"""
    
    default_lang = languages[0]
    for idx, lang in enumerate(languages):
        active_class = " active" if idx == 0 else ""
        html += f'            <button class="btn lang-btn{active_class}" onclick="setLanguage(\'{lang}\')">{lang.upper()}</button>\n'
        
    html += """        </div>
    </div>

    <div class="gallery-container">
"""

    for platform in ["ios", "android", "feature"]:
        for lang in languages:
            is_active = (platform == "ios" and lang == default_lang)
            active_class = " active" if is_active else ""
            
            html += f'        <div id="gallery-{platform}-{lang}" class="gallery-section{active_class}">\n'
            
            if lang in data and platform in data[lang]:
                for device in sorted(list(data[lang][platform].keys())):
                    html += f'            <div class="device-group">\n'
                    html += f'                <h2 class="device-title">{device}</h2>\n'
                    
                    if platform == "feature":
                        # Render Banner in wide 1-column layout
                        html += f'                <div class="screenshot-grid" style="grid-template-columns: 1fr;">\n'
                        for screen in sorted(list(data[lang][platform][device].keys())):
                            banner_path = data[lang][platform][device][screen]["Banner"]
                            html += f'                    <div class="screenshot-card" style="max-width: 800px; margin: 0 auto;">\n'
                            html += f'                        <div class="card-header">\n'
                            html += f'                            <span>Android Play Store Banner</span>\n'
                            html += f'                            <span class="screen-num">1024x500</span>\n'
                            html += f'                        </div>\n'
                            html += f'                        <div class="card-body" style="padding: 20px;">\n'
                            html += f'                            <div class="image-wrapper" style="max-width: 100%; width: 100%;">\n'
                            html += f'                                <span class="image-label">FEATURE GRAPHIC</span>\n'
                            html += f'                                <img class="screenshot-img" src="{banner_path}" alt="Feature Graphic" style="aspect-ratio: 1024/500; width: 100%; max-width: 760px; border-radius: 12px;" onclick="openModal(\'{banner_path}\')">\n'
                            html += f'                            </div>\n'
                            html += f'                        </div>\n'
                            html += f'                    </div>\n'
                    else:
                        # Render normal screenshots grid
                        html += f'                <div class="screenshot-grid">\n'
                        for screen in sorted(list(data[lang][platform][device].keys())):
                            screen_num = screen.split("_")[0]
                            clean_title = screen.split("_", 1)[-1] if "_" in screen else ""
                            if clean_title:
                                clean_title = " ".join(re.findall("[A-Z][a-z_]*", clean_title)).replace("_", " ")
                            if not clean_title:
                                if screen_num == "01": clean_title = "Dashboard (Empty)"
                                elif screen_num == "02": clean_title = "Dashboard (Active)"
                                elif screen_num == "03": clean_title = "Edit Member"
                                elif screen_num == "04": clean_title = "Share Settings"
                                else: clean_title = f"Screen {screen_num}"
                                
                            html += f'                    <div class="screenshot-card">\n'
                            html += f'                        <div class="card-header">\n'
                            html += f'                            <span>{clean_title}</span>\n'
                            html += f'                            <span class="screen-num">Screen {screen_num}</span>\n'
                            html += f'                        </div>\n'
                            html += f'                        <div class="card-body">\n'
                            
                            if "Slide" in data[lang][platform][device][screen]:
                                slide_path = data[lang][platform][device][screen]["Slide"]
                                html += f'                            <div class="image-wrapper">\n'
                                html += f'                                <span class="image-label">SLIDE</span>\n'
                                html += f'                                <img class="screenshot-img" src="{slide_path}" alt="Slide" onclick="openModal(\'{slide_path}\')">\n'
                                html += f'                            </div>\n'
                            if "Light" in data[lang][platform][device][screen]:
                                light_path = data[lang][platform][device][screen]["Light"]
                                html += f'                            <div class="image-wrapper">\n'
                                html += f'                                <span class="image-label">LIGHT</span>\n'
                                html += f'                                <img class="screenshot-img" src="{light_path}" alt="Light" onclick="openModal(\'{light_path}\')">\n'
                                html += f'                            </div>\n'
                            if "Dark" in data[lang][platform][device][screen]:
                                dark_path = data[lang][platform][device][screen]["Dark"]
                                html += f'                            <div class="image-wrapper">\n'
                                html += f'                                <span class="image-label">DARK</span>\n'
                                html += f'                                <img class="screenshot-img" src="{dark_path}" alt="Dark" onclick="openModal(\'{dark_path}\')">\n'
                                html += f'                            </div>\n'
                            html += f'                        </div>\n'
                            html += f'                    </div>\n'
                            
                    html += f'                </div>\n'
                    html += f'            </div>\n'
            else:
                html += f'            <div class="device-group" style="text-align: center; padding: 40px; color: #64748b;">\n'
                html += f'                <h3>Keine Assets für {platform.upper()} in {lang.upper()} vorhanden.</h3>\n'
                html += f'                <p style="margin-top: 10px; font-size: 0.9rem;">Führe die Generierungsskripte aus.</p>\n'
                html += f'            </div>\n'
                
            html += f'        </div>\n'
            
    html += """    </div>

    <!-- The Modal -->
    <div id="myModal" class="modal" onclick="closeModal()">
        <img class="modal-content" id="img01">
    </div>

    <script>
        var currentLang = '""" + default_lang + """';
        var currentPlatform = 'ios';
        
        function setLanguage(langCode) {
            currentLang = langCode;
            
            var buttons = document.getElementsByClassName('lang-btn');
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.remove('active');
                if (buttons[i].textContent.toLowerCase() === langCode.toLowerCase()) {
                    buttons[i].classList.add('active');
                }
            }
            
            updateGallery();
        }
        
        function setPlatform(platform) {
            currentPlatform = platform;
            
            var buttons = document.getElementsByClassName('plat-btn');
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.remove('active');
                if (buttons[i].textContent.toLowerCase().includes(platform) || 
                    (platform === 'feature' && buttons[i].textContent.toLowerCase().includes('feature'))) {
                    buttons[i].classList.add('active');
                }
            }
            
            updateGallery();
        }
        
        function updateGallery() {
            var sections = document.getElementsByClassName('gallery-section');
            for (var i = 0; i < sections.length; i++) {
                sections[i].classList.remove('active');
            }
            
            var targetId = 'gallery-' + currentPlatform + '-' + currentLang;
            var target = document.getElementById(targetId);
            if (target) {
                target.classList.add('active');
            }
        }
        
        function openModal(src) {
            var modal = document.getElementById('myModal');
            var modalImg = document.getElementById('img01');
            modal.style.display = "flex";
            modalImg.src = src;
        }
        
        function closeModal() {
            var modal = document.getElementById('myModal');
            modal.style.display = "none";
        }
        
        document.addEventListener('keydown', function(event) {
            if (event.key === "Escape") {
                closeModal();
            }
        });
    </script>
</body>
</html>
"""
    
    with open(html_file, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"Successfully compiled multi-platform screenshots overview: {html_file}")

if __name__ == "__main__":
    generate_report()
