import os
import re
import glob

def generate_report():
    base_dir = "/Users/daniel.notthoff/GIT_Repos/_privat/familienwecker/ios/fastlane/screenshots"
    html_file = os.path.join(base_dir, "screenshots.html")
    
    # Locate all png files
    png_pattern = os.path.join(base_dir, "**", "*.png")
    png_files = glob.glob(png_pattern, recursive=True)
    
    # Parse files into nested dictionary
    # format: screenshots/<lang>/<device>-<screen>_<appearance>.png
    data = {}
    
    # Regex pattern: match anything up to last hyphen as device, then screen name, then appearance
    pattern = re.compile(r"([^/]+)-([0-9]+_[A-Za-z_]+)_(Light|Dark)\.png$")
    
    for filepath in png_files:
        rel_path = os.path.relpath(filepath, base_dir)
        parts = rel_path.split(os.sep)
        if len(parts) < 2:
            continue
        lang = parts[0]
        filename = parts[-1]
        
        match = pattern.match(filename)
        if not match:
            continue
        
        device, screen_name, appearance = match.groups()
        
        if lang not in data:
            data[lang] = {}
        if device not in data[lang]:
            data[lang][device] = {}
        if screen_name not in data[lang][device]:
            data[lang][device][screen_name] = {}
            
        data[lang][device][screen_name][appearance] = rel_path

    # Build the HTML content
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
        
        .controls {
            max-width: 1400px;
            margin: 0 auto 30px auto;
            display: flex;
            flex-wrap: wrap;
            gap: 12px;
            justify-content: center;
            background-color: var(--card-bg);
            padding: 20px;
            border-radius: 16px;
            border: 1px solid var(--border-color);
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
        
        .lang-section {
            display: none;
            animation: fadeIn 0.3s ease-in-out forwards;
        }
        
        .lang-section.active {
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
        <h1>FamWake Screenshots</h1>
        <p class="subtitle">Lokalisierte App Store Screenshots im Light & Dark Mode</p>
    </header>

    <div class="controls">
"""
    
    # Add language buttons
    for idx, lang in enumerate(languages):
        active_class = " active" if idx == 0 else ""
        html += f'        <button class="btn lang-btn{active_class}" onclick="showLanguage(\'{lang}\')">{lang.upper()}</button>\n'
        
    html += """    </div>

    <div class="gallery-container">
"""

    # Add language sections
    for idx, lang in enumerate(languages):
        active_class = " active" if idx == 0 else ""
        html += f'        <div id="lang-{lang}" class="lang-section{active_class}">\n'
        
        # Add devices within the language
        for device in sorted(list(data[lang].keys())):
            html += f'            <div class="device-group">\n'
            html += f'                <h2 class="device-title">{device}</h2>\n'
            html += f'                <div class="screenshot-grid">\n'
            
            # Add screens
            for screen in sorted(list(data[lang][device].keys())):
                clean_title = screen.split("_", 1)[-1]
                # Format to a nice readable name (e.g. 01_MainDashboard -> Main Dashboard)
                clean_title = " ".join(re.findall("[A-Z][a-z]*", clean_title))
                if not clean_title:
                    clean_title = screen
                
                html += f'                    <div class="screenshot-card">\n'
                html += f'                        <div class="card-header">\n'
                html += f'                            <span>{clean_title}</span>\n'
                html += f'                            <span class="screen-num">Screen {screen.split("_")[0]}</span>\n'
                html += f'                        </div>\n'
                html += f'                        <div class="card-body">\n'
                
                # Render Light image if exists
                if "Light" in data[lang][device][screen]:
                    light_path = data[lang][device][screen]["Light"]
                    html += f'                            <div class="image-wrapper">\n'
                    html += f'                                <span class="image-label">LIGHT</span>\n'
                    html += f'                                <img class="screenshot-img" src="{light_path}" alt="{clean_title} Light Mode" onclick="openModal(\'{light_path}\')">\n'
                    html += f'                            </div>\n'
                    
                # Render Dark image if exists
                if "Dark" in data[lang][device][screen]:
                    dark_path = data[lang][device][screen]["Dark"]
                    html += f'                            <div class="image-wrapper">\n'
                    html += f'                                <span class="image-label">DARK</span>\n'
                    html += f'                                <img class="screenshot-img" src="{dark_path}" alt="{clean_title} Dark Mode" onclick="openModal(\'{dark_path}\')">\n'
                    html += f'                            </div>\n'
                    
                html += f'                        </div>\n'
                html += f'                    </div>\n'
                
            html += f'                </div>\n'
            html += f'            </div>\n'
            
        html += f'        </div>\n'
        
    html += """    </div>

    <!-- The Modal -->
    <div id="myModal" class="modal" onclick="closeModal()">
        <img class="modal-content" id="img01">
    </div>

    <script>
        function showLanguage(langCode) {
            // Hide all sections
            var sections = document.getElementsByClassName('lang-section');
            for (var i = 0; i < sections.length; i++) {
                sections[i].classList.remove('active');
            }
            
            // Show active section
            document.getElementById('lang-' + langCode).classList.add('active');
            
            // Update active button styling
            var buttons = document.getElementsByClassName('lang-btn');
            for (var i = 0; i < buttons.length; i++) {
                buttons[i].classList.remove('active');
                if (buttons[i].textContent.toLowerCase() === langCode.toLowerCase()) {
                    buttons[i].classList.add('active');
                }
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
        
        // Support ESC key to close modal
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
    print(f"Successfully compiled premium screenshots overview: {html_file}")

if __name__ == "__main__":
    generate_report()
