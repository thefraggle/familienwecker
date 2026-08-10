#!/bin/bash
# Updates onboarding slide texts for all 18 languages
# New copy: benefit-oriented, not feature-descriptive

BASE="app/src/main/res"

update_strings() {
  local file="$1"
  local s0_title="$2"
  local s0_body="$3"
  local s1_title="$4"
  local s1_body="$5"
  local s2_title="$6"
  local s2_body="$7"
  local s3_title="$8"
  local s3_body="$9"
  local s4_title="${10}"
  local s4_body="${11}"

  # Use perl for safe in-place replacement with special characters
  perl -i -0pe "s|(<string name=\"onboarding_slide0_title\">).*?(</string>)|\${1}${s0_title}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide0_body\">).*?(</string>)|\${1}${s0_body}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide1_title\">).*?(</string>)|\${1}${s1_title}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide1_body\">).*?(</string>)|\${1}${s1_body}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide2_title\">).*?(</string>)|\${1}${s2_title}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide2_body\">).*?(</string>)|\${1}${s2_body}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide3_title\">).*?(</string>)|\${1}${s3_title}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide3_body\">).*?(</string>)|\${1}${s3_body}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide4_title\">).*?(</string>)|\${1}${s4_title}\${2}|s" "$file"
  perl -i -0pe "s|(<string name=\"onboarding_slide4_body\">).*?(</string>)|\${1}${s4_body}\${2}|s" "$file"
}

# EN (default fallback)
update_strings "$BASE/values/strings.xml" \
  "Your morning as a team." \
  "No arguments, no chaos. FamWake calculates the perfect morning for your whole family – fully automatic." \
  "No more waiting." \
  "FamWake calculates the ideal wake-up time for every family member – coordinated around bathroom, breakfast, and departure. No more crowding." \
  "Every day is different." \
  "Monday office, Wednesday home office? Set individual times for each day of the week – the app handles the rest." \
  "Connected in seconds." \
  "Share a code and your family joins instantly. New members? The schedule adjusts automatically." \
  "Let\\'s go!" \
  "Create your family now and enjoy relaxed mornings – from day one."

# DE
update_strings "$BASE/values-de/strings.xml" \
  "Dein Morgen als Team." \
  "Keine Diskussionen, kein Chaos. FamWake berechnet den perfekten Morgen für die ganze Familie – vollautomatisch." \
  "Nie wieder warten." \
  "FamWake berechnet für jedes Familienmitglied die ideale Weckzeit – abgestimmt auf Bad, Frühstück und Abfahrt. Kein Gedrängel mehr." \
  "Jeder Tag ist anders." \
  "Montag Büro, Mittwoch Home-Office? Stelle für jeden Wochentag individuelle Zeiten ein – die App rechnet den Rest." \
  "In Sekunden verbunden." \
  "Teile einen Code und deine Familie ist sofort dabei. Neue Mitglieder? Der Plan passt sich automatisch an." \
  "Los geht\\'s!" \
  "Erstelle jetzt deine Familie und erlebe entspannte Morgen – ab dem ersten Tag."

# ES
update_strings "$BASE/values-es/strings.xml" \
  "Tu mañana en equipo." \
  "Sin discusiones, sin caos. FamWake calcula la mañana perfecta para toda la familia – automáticamente." \
  "Se acabó esperar." \
  "FamWake calcula el momento ideal para despertar a cada miembro – coordinando baño, desayuno y salida. Sin más aglomeraciones." \
  "Cada día es diferente." \
  "¿Lunes oficina, miércoles teletrabajo? Configura horarios individuales para cada día – la app se encarga del resto." \
  "Conectados en segundos." \
  "Comparte un código y tu familia se une al instante. ¿Nuevos miembros? El plan se ajusta automáticamente." \
  "¡Vamos!" \
  "Crea tu familia ahora y disfruta de mañanas relajadas – desde el primer día."

# FR
update_strings "$BASE/values-fr/strings.xml" \
  "Votre matin en équipe." \
  "Plus de disputes, plus de chaos. FamWake calcule le matin parfait pour toute la famille – entièrement automatique." \
  "Fini l\\'attente." \
  "FamWake calcule l\\'heure de réveil idéale pour chaque membre – en coordonnant salle de bain, petit-déjeuner et départ. Plus de bousculade." \
  "Chaque jour est différent." \
  "Lundi au bureau, mercredi en télétravail ? Réglez des horaires individuels pour chaque jour – l\\'appli s\\'occupe du reste." \
  "Connectés en secondes." \
  "Partagez un code et votre famille rejoint instantanément. Nouveaux membres ? Le planning s\\'adapte automatiquement." \
  "C\\'est parti !" \
  "Créez votre famille maintenant et profitez de matins détendus – dès le premier jour."

# IT
update_strings "$BASE/values-it/strings.xml" \
  "La tua mattina in squadra." \
  "Niente discussioni, niente caos. FamWake calcola la mattina perfetta per tutta la famiglia – in modo completamente automatico." \
  "Mai più attese." \
  "FamWake calcola il momento ideale per svegliare ogni membro – coordinando bagno, colazione e uscita. Niente più code." \
  "Ogni giorno è diverso." \
  "Lunedì in ufficio, mercoledì da casa? Imposta orari individuali per ogni giorno della settimana – l\\'app fa il resto." \
  "Connessi in pochi secondi." \
  "Condividi un codice e la tua famiglia si unisce all\\'istante. Nuovi membri? Il piano si adatta automaticamente." \
  "Si parte!" \
  "Crea la tua famiglia ora e goditi mattine rilassate – fin dal primo giorno."

# NL
update_strings "$BASE/values-nl/strings.xml" \
  "Je ochtend als team." \
  "Geen discussies, geen chaos. FamWake berekent de perfecte ochtend voor het hele gezin – volautomatisch." \
  "Nooit meer wachten." \
  "FamWake berekent de ideale wektijd voor elk gezinslid – afgestemd op badkamer, ontbijt en vertrek. Geen gedrang meer." \
  "Elke dag is anders." \
  "Maandag kantoor, woensdag thuiswerken? Stel voor elke dag individuele tijden in – de app regelt de rest." \
  "In seconden verbonden." \
  "Deel een code en je gezin doet direct mee. Nieuwe leden? Het schema past zich automatisch aan." \
  "Aan de slag!" \
  "Maak nu je gezin aan en geniet van ontspannen ochtenden – vanaf dag één."

# PL
update_strings "$BASE/values-pl/strings.xml" \
  "Twój poranek jako zespół." \
  "Żadnych dyskusji, żadnego chaosu. FamWake oblicza idealny poranek dla całej rodziny – w pełni automatycznie." \
  "Koniec z czekaniem." \
  "FamWake oblicza idealny czas budzenia dla każdego członka rodziny – koordynując łazienkę, śniadanie i wyjście. Koniec ze ściskiem." \
  "Każdy dzień jest inny." \
  "Poniedziałek biuro, środa praca zdalna? Ustaw indywidualne godziny na każdy dzień tygodnia – aplikacja zajmie się resztą." \
  "Połączeni w sekundy." \
  "Udostępnij kod, a Twoja rodzina dołączy natychmiast. Nowi członkowie? Plan dostosuje się automatycznie." \
  "Zaczynamy!" \
  "Utwórz swoją rodzinę i ciesz się spokojnymi porankami – od pierwszego dnia."

# PT
update_strings "$BASE/values-pt/strings.xml" \
  "A sua manhã em equipa." \
  "Sem discussões, sem caos. O FamWake calcula a manhã perfeita para toda a família – totalmente automático." \
  "Nunca mais esperar." \
  "O FamWake calcula o horário ideal para acordar cada membro – coordenando casa de banho, pequeno-almoço e saída. Sem mais apertos." \
  "Cada dia é diferente." \
  "Segunda no escritório, quarta em teletrabalho? Defina horários individuais para cada dia da semana – a app trata do resto." \
  "Ligados em segundos." \
  "Partilhe um código e a sua família junta-se instantaneamente. Novos membros? O plano ajusta-se automaticamente." \
  "Vamos lá!" \
  "Crie a sua família agora e desfrute de manhãs tranquilas – desde o primeiro dia."

# SV
update_strings "$BASE/values-sv/strings.xml" \
  "Din morgon som team." \
  "Inga diskussioner, inget kaos. FamWake beräknar den perfekta morgonen för hela familjen – helt automatiskt." \
  "Aldrig mer väntan." \
  "FamWake beräknar den ideala väckningstiden för varje familjemedlem – samordnat med badrum, frukost och avresa. Inget mer trängsel." \
  "Varje dag är annorlunda." \
  "Måndag kontor, onsdag hemmakontor? Ställ in individuella tider för varje veckodag – appen sköter resten." \
  "Uppkopplade på sekunder." \
  "Dela en kod och din familj ansluter direkt. Nya medlemmar? Schemat anpassar sig automatiskt." \
  "Nu kör vi!" \
  "Skapa din familj nu och njut av avslappnade morgnar – från dag ett."

# DA
update_strings "$BASE/values-da/strings.xml" \
  "Din morgen som team." \
  "Ingen diskussioner, intet kaos. FamWake beregner den perfekte morgen for hele familien – helt automatisk." \
  "Aldrig mere ventetid." \
  "FamWake beregner det ideelle vækketidspunkt for hvert familiemedlem – koordineret med badeværelse, morgenmad og afgang. Ikke mere trængsel." \
  "Hver dag er anderledes." \
  "Mandag kontor, onsdag hjemmekontor? Indstil individuelle tider for hver ugedag – appen klarer resten." \
  "Forbundet på sekunder." \
  "Del en kode, og din familie er med med det samme. Nye medlemmer? Planen tilpasser sig automatisk." \
  "Lad os komme i gang!" \
  "Opret din familie nu og nyd afslappede morgener – fra dag ét."

# NO
update_strings "$BASE/values-no/strings.xml" \
  "Din morgen som team." \
  "Ingen diskusjoner, ikke noe kaos. FamWake beregner den perfekte morgenen for hele familien – helt automatisk." \
  "Aldri mer venting." \
  "FamWake beregner det ideelle vekketidspunktet for hvert familiemedlem – koordinert med bad, frokost og avreise. Ikke mer trengsel." \
  "Hver dag er forskjellig." \
  "Mandag kontor, onsdag hjemmekontor? Still inn individuelle tider for hver ukedag – appen tar seg av resten." \
  "Tilkoblet på sekunder." \
  "Del en kode, og familien din er med umiddelbart. Nye medlemmer? Planen tilpasser seg automatisk." \
  "La oss kjøre!" \
  "Opprett familien din nå og nyt avslappede morgener – fra dag én."

# JA
update_strings "$BASE/values-ja/strings.xml" \
  "チームとしての朝。" \
  "議論もカオスもなし。FamWakeが家族全員の完璧な朝を自動で計算します。" \
  "もう待たなくていい。" \
  "FamWakeが家族一人ひとりの理想の起床時間を計算。バスルーム、朝食、出発を調整して、渋滞を解消します。" \
  "毎日が違う。" \
  "月曜はオフィス、水曜は在宅？曜日ごとに個別の時間を設定 – あとはアプリにおまかせ。" \
  "数秒でつながる。" \
  "コードを共有するだけで家族がすぐに参加。新メンバー？スケジュールは自動で調整されます。" \
  "さあ始めよう！" \
  "今すぐ家族を作成して、リラックスした朝を楽しもう – 初日から。"

# TR
update_strings "$BASE/values-tr/strings.xml" \
  "Takım halinde sabahınız." \
  "Tartışma yok, kaos yok. FamWake tüm aile için mükemmel sabahı hesaplar – tamamen otomatik." \
  "Artık bekleme yok." \
  "FamWake her aile üyesi için ideal uyanma zamanını hesaplar – banyo, kahvaltı ve çıkışı koordine eder. Artık kalabalık yok." \
  "Her gün farklı." \
  "Pazartesi ofis, çarşamba evden çalışma? Her gün için ayrı saatler ayarlayın – uygulama gerisini halleder." \
  "Saniyeler içinde bağlanın." \
  "Bir kod paylaşın ve aileniz anında katılsın. Yeni üyeler? Plan otomatik olarak uyum sağlar." \
  "Haydi başlayalım!" \
  "Ailenizi şimdi oluşturun ve rahat sabahların keyfini çıkarın – ilk günden itibaren."

# UK
update_strings "$BASE/values-uk/strings.xml" \
  "Ваш ранок як команда." \
  "Жодних суперечок, жодного хаосу. FamWake розраховує ідеальний ранок для всієї родини – повністю автоматично." \
  "Більше ніякого очікування." \
  "FamWake розраховує ідеальний час пробудження для кожного члена сім\\'ї – координуючи ванну, сніданок і вихід. Більше ніякої тісноти." \
  "Кожен день інший." \
  "Понеділок в офісі, середа вдома? Встановіть індивідуальний час для кожного дня тижня – додаток зробить решту." \
  "З\\'єднані за секунди." \
  "Поділіться кодом, і ваша родина приєднається миттєво. Нові учасники? Розклад адаптується автоматично." \
  "Починаємо!" \
  "Створіть свою родину зараз і насолоджуйтесь спокійними ранками – з першого дня."

# RU
update_strings "$BASE/values-ru/strings.xml" \
  "Ваше утро как команда." \
  "Никаких споров, никакого хаоса. FamWake рассчитывает идеальное утро для всей семьи – полностью автоматически." \
  "Больше никакого ожидания." \
  "FamWake рассчитывает идеальное время пробуждения для каждого члена семьи – координируя ванную, завтрак и выход. Никакой больше толкучки." \
  "Каждый день другой." \
  "Понедельник в офисе, среда дома? Установите индивидуальное время для каждого дня недели – приложение сделает остальное." \
  "На связи за секунды." \
  "Поделитесь кодом, и ваша семья присоединится мгновенно. Новые участники? Расписание адаптируется автоматически." \
  "Начинаем!" \
  "Создайте свою семью сейчас и наслаждайтесь спокойными утрами – с первого дня."

# GSW (Schweizerdeutsch)
update_strings "$BASE/values-b+gsw/strings.xml" \
  "Euie Morge als Team." \
  "Kä Diskussione, kä Chaos. FamWake berechnet de perfekt Morge für d\\'ganzi Familie – voll automatisch." \
  "Nie meh warte." \
  "FamWake berechnet für jedes Familiemitglied di ideal Weckziit – abgstimmt uf Bad, Zmorge und Abfahrt. Kä Gedrängel meh." \
  "Jede Tag isch anders." \
  "Mäntig Büro, Mittwuch Homeoffice? Stell für jede Wuchetag individuelli Ziite ii – d\\'App macht de Räscht." \
  "In Sekunde verbunde." \
  "Teil en Code und diini Familie isch sofort debii. Nöi Mitglieder? De Plan passt sich automatisch ah." \
  "Los gaht\\'s!" \
  "Erstell jetzt diini Familie und gniess entspannti Morge – ab em erschte Tag."

# SWG (Schwäbisch)
update_strings "$BASE/values-b+swg/strings.xml" \
  "Dei Morga als Team." \
  "Koi Diskussiona, koi Chaos. FamWake berechnet de perfekta Morga für d\\'ganz Familie – vollautomaadisch." \
  "Nie meh warta." \
  "FamWake berechnet für jedes Familiamitglied die ideale Weckzeit – abgestimmt auf Bad, Frühstück ond Abfahrt. Koi Gedrängel meh." \
  "Jede Tag isch anders." \
  "Montag Büro, Mittwoch Dahoim? Stell für jede Wochadag individuelli Zeita ei – d\\'App rechnet de Rest." \
  "In Sekunda verbonda." \
  "Teil en Code ond dei Familie isch sofort debei. Neue Mitglieder? Dr Plan passt sich automatisch ah." \
  "Los geht\\'s!" \
  "Erstell jetzt dei Familie ond erleb entspannde Morga – ab em erschte Tag."

# KSH (Ruhrpott)
update_strings "$BASE/values-b+ksh/strings.xml" \
  "Morgens im Team." \
  "Kein Knatsch, kein Chaos. FamWake rechnet den perfekten Morgen für die ganze Truppe aus – voll automatisch." \
  "Nie wieder warten." \
  "FamWake rechnet für jeden die ideale Weckzeit aus – abgestimmt auf Pötte, Mampfen und Abflug. Kein Gedrängel mehr." \
  "Jeder Tag is anders." \
  "Montag Maloche, Mittwoch Homeoffice? Stell für jeden Tag eigene Zeiten ein – die App regelt den Rest." \
  "In Sekunden verbunden." \
  "Teil nen Code und deine Truppe is sofort dabei. Neue Leute? Der Plan regelt sich von alleine." \
  "Los geht\\'s!" \
  "Erstell jetzt deine Truppe und freu dich auf entspannte Morgen – ab Tag eins."

echo "✅ All 18 languages updated."
