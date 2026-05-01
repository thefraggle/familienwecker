const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const { Resend } = require("resend");
const { randomInt } = require("crypto");

admin.initializeApp();

/**
 * Escapes HTML special characters to prevent XSS.
 */
function escapeHtml(unsafe) {
  if (!unsafe || typeof unsafe !== "string") return "";
  return unsafe
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

/**
 * Normalisiert einen Sprach-String auf einen unterstützten IETF-Code.
 * Dialekte (gsw, swg, ksh) werden auf "de" gemappt; unbekannte Sprachen auf "en".
 */
function resolveLanguage(rawInput) {
  const raw = (rawInput || "de").toLowerCase();
  const requested = DIALECT_TO_LANG[raw] || raw.slice(0, 2);
  // Spezifischer Check für zh-CN
  if (raw === "zh-cn" || raw === "zh") return "zh-CN";
  return SUPPORTED_LANGS.includes(requested) ? requested : "en";
}

/**
 * Prüft ein einzelnes Rate-Limit-Fenster.
 * Gibt true zurück wenn das Limit erreicht ist, false wenn OK (und Zähler wird erhöht).
 */
async function checkSingleRateLimit(key, windowMs, maxAttempts) {
  const ref = admin.firestore().collection("_rate_limits").doc(key);
  const now = Date.now();
  return admin.firestore().runTransaction(async (tx) => {
    const doc = await tx.get(ref);
    const data = doc.exists ? doc.data() : { count: 0, windowStart: now };
    if (now - data.windowStart > windowMs) {
      tx.set(ref, { count: 1, windowStart: now });
      return false;
    }
    if (data.count >= maxAttempts) return true;
    tx.set(ref, { count: data.count + 1, windowStart: data.windowStart }, { merge: true });
    return false;
  });
}

/**
 * Dual Rate-Limit auf E-Mail-Adresse, getrennt nach Typ (reset/verify/confirm).
 * Verhindert dass Verifikations-Mails das Reset-Limit beeinflussen.
 * max. 5 Versuche pro Stunde UND max. 10 pro Tag – je Typ unabhängig.
 */
async function checkEmailRateLimit(email, type = "email") {
  // Admin-Bypass über _admins-Collection (sicher, da serverseitig)
  const adminSnapshot = await admin.firestore()
    .collection("_admins")
    .where("email", "==", email.toLowerCase().trim())
    .limit(1)
    .get();

  if (!adminSnapshot.empty) {
    console.log(`Bypassing email rate limit for admin: ${email}`);
    return;
  }

  // Typ-spezifischer Key: reset/verify/confirm haben unabhängige Zähler
  const emailKey = email.toLowerCase().replace(/[^a-z0-9]/g, "_").slice(0, 70);
  const key = `${type}_${emailKey}`;

  // Stunden-Limit
  const hourLimited = await checkSingleRateLimit(`${key}_h`, 60 * 60 * 1000, 5);
  if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");

  // Tages-Limit
  const dayLimited = await checkSingleRateLimit(`${key}_d`, 24 * 60 * 60 * 1000, 10);
  if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
}

const NOTIFY_EMAIL = "daniel.notthoff@gmail.com";
// Security: PRIMARY_ADMIN_UID aus Firebase Secret Manager – nicht hartkodiert.
const primaryAdminUidSecret = defineSecret("PRIMARY_ADMIN_UID");
const BRAND_BLUE = "#1A3A5C";

// Alle unterstützten E-Mail-Sprachen (22 Lokalisierungen)
const SUPPORTED_LANGS = [
  "de", "en", "es", "fr", "it", "da", "ja", "nl", "no", "pl",
  "pt", "ru", "sv", "tr", "uk", "id", "vi", "bn", "mr", "hi",
  "zh-CN", "ko"
];

// Dialekte → Muttersprache für E-Mail-Inhalte (formal/rechtlich → Hochdeutsch)
const DIALECT_TO_LANG = { gsw: "de", swg: "de", ksh: "de" };

const SENDER = {
  de: "FamWake Familienwecker <no-reply@familienwecker.de>",
  en: "FamWake Family Alarm <no-reply@familienwecker.de>",
  es: "FamWake Despertador Familiar <no-reply@familienwecker.de>",
  fr: "FamWake Réveil Familial <no-reply@familienwecker.de>",
  it: "FamWake Sveglia Famiglia <no-reply@familienwecker.de>",
  da: "FamWake Familievækker <no-reply@familienwecker.de>",
  ja: "FamWake ファミリーアラーム <no-reply@familienwecker.de>",
  nl: "FamWake Gezinswekker <no-reply@familienwecker.de>",
  no: "FamWake Familievekker <no-reply@familienwecker.de>",
  pl: "FamWake Budzik Rodzinny <no-reply@familienwecker.de>",
  pt: "FamWake Despertador Familiar <no-reply@familienwecker.de>",
  ru: "FamWake Семейный Будильник <no-reply@familienwecker.de>",
  sv: "FamWake Familjens Väckarklocka <no-reply@familienwecker.de>",
  tr: "FamWake Aile Alarmı <no-reply@familienwecker.de>",
  uk: "FamWake Сімейний Будильник <no-reply@familienwecker.de>",
  id: "FamWake Jam Alarm Keluarga <no-reply@familienwecker.de>",
  vi: "FamWake Báo thức gia đình <no-reply@familienwecker.de>",
  bn: "FamWake পারিবারিক অ্যালার্ম <no-reply@familienwecker.de>",
  mr: "FamWake कौटुंबिक अलार्म <no-reply@familienwecker.de>",
  hi: "FamWake पारिवारिक अलार्म <no-reply@familienwecker.de>",
  "zh-CN": "FamWake 家庭闹钟 <no-reply@familienwecker.de>",
  ko: "FamWake 가족 알람시계 <no-reply@familienwecker.de>",
};

const LINK_LABELS = {
  de: { home: "Startseite", privacy: "Datenschutzerklärung", imprint: "Impressum" },
  en: { home: "Website", privacy: "Privacy Policy", imprint: "Legal Notice" },
  es: { home: "Inicio", privacy: "Política de privacidad", imprint: "Aviso legal" },
  fr: { home: "Accueil", privacy: "Politique de confidentialité", imprint: "Mentions légales" },
  it: { home: "Home", privacy: "Informativa sulla privacy", imprint: "Note legali" },
  da: { home: "Hjemmeside", privacy: "Privatlivspolitik", imprint: "Juridisk meddelelse" },
  ja: { home: "ウェブサイト", privacy: "プライバシーポリシー", imprint: "特定商取引法" },
  nl: { home: "Website", privacy: "Privacybeleid", imprint: "Juridische kennisgeving" },
  no: { home: "Nettsted", privacy: "Personvernerklæring", imprint: "Juridisk merknad" },
  pl: { home: "Strona główna", privacy: "Polityka prywatności", imprint: "Nota prawna" },
  pt: { home: "Website", privacy: "Política de Privacidade", imprint: "Nota Legal" },
  ru: { home: "Веб-сайт", privacy: "Политика конфиденциальности", imprint: "Правовые сведения" },
  sv: { home: "Webbplats", privacy: "Integritetspolicy", imprint: "Juridisk information" },
  tr: { home: "Web Sitesi", privacy: "Gizlilik Politikası", imprint: "Yasal Uyarı" },
  uk: { home: "Веб-сайт", privacy: "Політика конфіденційності", imprint: "Правова інформація" },
  id: { home: "Situs Web", privacy: "Kebijakan Privasi", imprint: "Pemberitahuan Hukum" },
  vi: { home: "Trang web", privacy: "Chính sách bảo mật", imprint: "Thông báo pháp lý" },
  bn: { home: "ওয়েবসাইট", privacy: "গোপনীয়তা নীতি", imprint: "আইনি বিজ্ঞপ্তি" },
  mr: { home: "वेबसाइट", privacy: "गोपनीयता धोरण", imprint: "कायदेशीर सूचना" },
  hi: { home: "वेबसाइट", privacy: "गोपनीयता नीति", imprint: "कानूनी सूचना" },
  "zh-CN": { home: "网站", privacy: "隐私政策", imprint: "法律声明" },
  ko: { home: "웹사이트", privacy: "개인정보 처리방침", imprint: "법적 고지" },
};

const EMAIL_CONTENT = {
  de: {
    subject: "🔑 Passwort zurücksetzen – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Hallo!",
    intro: "Wir haben eine Anfrage erhalten, das Passwort für dein <strong>FamWake</strong> Familienwecker-Konto zurückzusetzen.",
    instruction: "Klicke auf den folgenden Button, um ein neues Passwort zu vergeben:",
    button: "Passwort zurücksetzen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    security: "⚠️ Falls du diese Anfrage <strong>nicht</strong> gestellt hast, kannst du diese E-Mail einfach ignorieren. Dein Passwort bleibt unverändert. Solltest du verdächtige Aktivitäten bemerken, wende dich bitte an: daniel.notthoff@gmail.com",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🔑 Reset your password – FamWake Family Alarm",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Hello!",
    intro: "We received a request to reset the password for your <strong>FamWake</strong> Family Alarm account.",
    instruction: "Click the button below to set a new password:",
    button: "Reset Password",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    security: "⚠️ If you did <strong>not</strong> request this, you can safely ignore this email. Your password will remain unchanged. If you notice any suspicious activity, please contact us at: daniel.notthoff@gmail.com",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
  es: {
    subject: "🔑 Restablecer contraseña – FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "¡Hola!",
    intro: "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta de <strong>FamWake</strong> Despertador Familiar.",
    instruction: "Haz clic en el botón de abajo para establecer una nueva contraseña:",
    button: "Restablecer contraseña",
    fallback: "Si el botón no funciona, copia este enlace en tu navegador:",
    security: "⚠️ Si no has solicitado esto, puedes ignorar este correo de forma segura. Tu contraseña no cambiará. Si notas alguna actividad sospechosa, ponte en contacto con nosotros en: daniel.notthoff@gmail.com",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no respondas directamente a este correo.",
  },
  fr: {
    subject: "🔑 Réinitialiser ton mot de passe – FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bonjour !",
    intro: "Nous avons reçu une demande pour réinitialiser le mot de passe de ton compte <strong>FamWake</strong> Réveil Familial.",
    instruction: "Clique sur le bouton ci-dessous pour définir un nouveau mot de passe :",
    button: "Réinitialiser le mot de passe",
    fallback: "Si le bouton ne fonctionne pas, merci de copier ce lien dans ton navigateur :",
    security: "⚠️ Si tu n'as <strong>pas</strong> fait cette demande, tu peux ignorer cet e-mail. Ton mot de passe restera inchangé. Si tu remarques une activité suspecte, contacte-nous à l'adresse : daniel.notthoff@gmail.com",
    footerNote: "Ceci est un message généré automatiquement. Merci de ne pas répondre directement à cet e-mail.",
  },
  it: {
    subject: "🔑 Reimposta la tua password – FamWake Sveglia Famiglia",
    appName: "<strong>FamWake</strong> Sveglia Famiglia",
    greeting: "Ciao!",
    intro: "Abbiamo ricevuto una richiesta di reimpostazione della password per il tuo account <strong>FamWake</strong> Sveglia Famiglia.",
    instruction: "Clicca sul pulsante qui sotto per impostare una nuova password:",
    button: "Reimposta password",
    fallback: "Se il pulsante non funziona, copia questo link nel tuo browser:",
    security: "⚠️ Se <strong>non</strong> hai richiesto tu questa modifica, puoi ignorare tranquillamente questa email. La tua password rimarrà invariata. Se noti attività sospette, contattaci all'indirizzo: daniel.notthoff@gmail.com",
    footerNote: "Questo è un messaggio generato automaticamente. Si prega di non rispondere direttamente a questa email.",
  },
  da: {
    subject: "🔑 Nulstil din adgangskode – FamWake Familievækker",
    appName: "<strong>FamWake</strong> Familievækker",
    greeting: "Hej!",
    intro: "Vi har modtaget en anmodning om at nulstille adgangskoden til din <strong>FamWake</strong> Familievækker-konto.",
    instruction: "Klik på knappen nedenfor for at angive en ny adgangskode:",
    button: "Nulstil adgangskode",
    fallback: "Hvis knappen ikke virker, skal du kopiere dette link til din browser:",
    security: "⚠️ Hvis du <strong>ikke</strong> har anmodet om dette, kan du blot ignorere denne e-mail. Din adgangskode forbliver uændret. Hvis du bemærker mistænkelig aktivitet, kontakt os på: daniel.notthoff@gmail.com",
    footerNote: "Dette er en automatisk genereret besked. Svar venligst ikke direkte på denne e-mail.",
  },
  ja: {
    subject: "🔑 パスワードのリセット – FamWake ファミリーアラーム",
    appName: "<strong>FamWake</strong> ファミリーアラーム",
    greeting: "こんにちは！",
    intro: "<strong>FamWake</strong> ファミリーアラームのアカウントのパスワードをリセットするリクエストを受け取りました。",
    instruction: "以下のボタンをクリックして、新しいパスワードを設定してください：",
    button: "パスワードをリセット",
    fallback: "ボタンが機能しない場合は、このリンクをブラウザにコピーしてください：",
    security: "⚠️ このリクエストに<strong>心当たりがない</strong>場合は、このメールを無視してください。パスワードは変更されません。不審なアクティビティに気づいた場合は、こちらまでご連絡ください：daniel.notthoff@gmail.com",
    footerNote: "これは自動生成されたメッセージです。このメールに直接返信しないでください。",
  },
  nl: {
    subject: "🔑 Wachtwoord opnieuw instellen – FamWake Gezinswekker",
    appName: "<strong>FamWake</strong> Gezinswekker",
    greeting: "Hallo!",
    intro: "We hebben een verzoek ontvangen om het wachtwoord van je <strong>FamWake</strong> Gezinswekker-account opnieuw in te stellen.",
    instruction: "Klik op de onderstaande knop om een nieuw wachtwoord in te stellen:",
    button: "Wachtwoord opnieuw instellen",
    fallback: "Als de knop niet werkt, kopieer dan deze link naar je browser:",
    security: "⚠️ Als je dit verzoek <strong>niet</strong> hebt gedaan, kun je deze e-mail gewoon negeren. Je wachtwoord blijft ongewijzigd. Als je verdachte activiteit opmerkt, neem dan contact met ons op: daniel.notthoff@gmail.com",
    footerNote: "Dit is een automatisch gegenereerd bericht. Reageer niet rechtstreeks op deze e-mail.",
  },
  no: {
    subject: "🔑 Tilbakestill passordet – FamWake Familievekker",
    appName: "<strong>FamWake</strong> Familievekker",
    greeting: "Hei!",
    intro: "Vi har mottatt en forespørsel om å tilbakestille passordet for din <strong>FamWake</strong> Familievekker-konto.",
    instruction: "Klikk på knappen nedenfor for å angi et nytt passord:",
    button: "Tilbakestill passord",
    fallback: "Hvis knappen ikke fungerer, lim inn denne lenken i nettleseren din:",
    security: "⚠️ Hvis du <strong>ikke</strong> har bedt om dette, kan du trygt ignorere denne e-posten. Passordet ditt forblir uendret. Hvis du merker mistenkelig aktivitet, kontakt oss på: daniel.notthoff@gmail.com",
    footerNote: "Dette er en automatisk generert melding. Vennligst ikke svar direkte på denne e-posten.",
  },
  pl: {
    subject: "🔑 Resetowanie hasła – FamWake Budzik Rodzinny",
    appName: "<strong>FamWake</strong> Budzik Rodzinny",
    greeting: "Cześć!",
    intro: "Otrzymaliśmy prośbę o zresetowanie hasła do Twojego konta <strong>FamWake</strong> Budzik Rodzinny.",
    instruction: "Kliknij poniższy przycisk, aby ustawić nowe hasło:",
    button: "Zresetuj hasło",
    fallback: "Jeśli przycisk nie działa, skopiuj ten link do przeglądarki:",
    security: "⚠️ Jeśli <strong>nie</strong> wysyłałeś tej prośby, możesz zignorować ten e-mail. Twoje hasło pozostanie bez zmian. Jeśli zauważysz podejrzaną aktywność, skontaktuj się z nami: daniel.notthoff@gmail.com",
    footerNote: "To jest wiadomość wygenerowana automatycznie. Prosimy nie odpowiadać bezpośrednio na ten e-mail.",
  },
  pt: {
    subject: "🔑 Redefinir senha – FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "Olá!",
    intro: "Recebemos uma solicitação para redefinir a senha da sua conta <strong>FamWake</strong> Despertador Familiar.",
    instruction: "Clique no botão abaixo para definir uma nova senha:",
    button: "Redefinir senha",
    fallback: "Se o botão não funcionar, copie este link para o seu navegador:",
    security: "⚠️ Se você <strong>não</strong> fez esta solicitação, pode ignorar este e-mail. Sua senha permanecerá inalterada. Se notar atividade suspeita, entre em contato: daniel.notthoff@gmail.com",
    footerNote: "Esta é uma mensagem gerada automaticamente. Por favor, não responda diretamente a este e-mail.",
  },
  ru: {
    subject: "🔑 Сброс пароля – FamWake Семейный Будильник",
    appName: "<strong>FamWake</strong> Семейный Будильник",
    greeting: "Привет!",
    intro: "Мы получили запрос на сброс пароля для твоей учётной записи <strong>FamWake</strong> Семейный Будильник.",
    instruction: "Нажми на кнопку ниже, чтобы установить новый пароль:",
    button: "Сбросить пароль",
    fallback: "Если кнопка не работает, скопируй эту ссылку в браузер:",
    security: "⚠️ Если ты <strong>не</strong> запрашивал сброс, просто проигнорируй это письмо. Твой пароль останется без изменений. Если заметишь подозрительную активность, свяжись с нами: daniel.notthoff@gmail.com",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечай на него напрямую.",
  },
  sv: {
    subject: "🔑 Återställ ditt lösenord – FamWake Familjens Väckarklocka",
    appName: "<strong>FamWake</strong> Familjens Väckarklocka",
    greeting: "Hej!",
    intro: "Vi har tagit emot en begäran om att återställa lösenordet för ditt <strong>FamWake</strong> Familjens Väckarklocka-konto.",
    instruction: "Klicka på knappen nedan för att ange ett nytt lösenord:",
    button: "Återställ lösenord",
    fallback: "Om knappen inte fungerar, kopiera den här länken till din webbläsare:",
    security: "⚠️ Om du <strong>inte</strong> har begärt detta kan du ignorera detta e-postmeddelande. Ditt lösenord förblir oförändrat. Om du märker misstänkt aktivitet, kontakta oss på: daniel.notthoff@gmail.com",
    footerNote: "Detta är ett automatiskt genererat meddelande. Svara inte direkt på detta e-postmeddelande.",
  },
  tr: {
    subject: "🔑 Şifreni sıfırla – FamWake Aile Alarmı",
    appName: "<strong>FamWake</strong> Aile Alarmı",
    greeting: "Merhaba!",
    intro: "<strong>FamWake</strong> Aile Alarmı hesabının şifresini sıfırlama talebi aldık.",
    instruction: "Yeni bir şifre belirlemek için aşağıdaki düğmeye tıkla:",
    button: "Şifreyi sıfırla",
    fallback: "Düğme çalışmıyorsa, bu bağlantıyı tarayıcına kopyalayabilirsin:",
    security: "⚠️ Bu talebi <strong>sen</strong> yapmadıysan, bu e-postayı görmezden gelebilirsin. Şifren değişmeden kalacak. Şüpheli bir etkinlik fark edersen bizimle iletişime geç: daniel.notthoff@gmail.com",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt verme.",
  },
  uk: {
    subject: "🔑 Скидання пароля – FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Привіт!",
    intro: "Ми отримали запит на скидання пароля для твого облікового запису <strong>FamWake</strong> Сімейний Будильник.",
    instruction: "Натисни кнопку нижче, щоб встановити новий пароль:",
    button: "Скинути пароль",
    fallback: "Якщо кнопка не працює, скопіюй це посилання у браузер:",
    security: "⚠️ Якщо ти <strong>не</strong> робив цей запит, просто ігноруй цей лист. Твій пароль залишиться без змін. Якщо помітиш підозрілу активність, звʼяжися з нами: daniel.notthoff@gmail.com",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідай на нього безпосередньо.",
  },
  id: {
    subject: "🔑 Atur ulang kata sandi kamu – FamWake Jam Alarm Keluarga",
    appName: "<strong>FamWake</strong> Jam Alarm Keluarga",
    greeting: "Halo!",
    intro: "Kami menerima permintaan untuk mengatur ulang kata sandi akun <strong>FamWake</strong> Jam Alarm Keluarga kamu.",
    instruction: "Klik tombol di bawah ini untuk menetapkan kata sandi baru:",
    button: "Atur Ulang Kata Sandi",
    fallback: "Jika tombolnya tidak berfungsi, silakan salin tautan ini ke browser kamu:",
    security: "⚠️ Jika kamu <strong>tidak</strong> meminta ini, kamu bisa mengabaikan email ini dengan aman. Kata sandi kamu tidak akan berubah. Jika kamu melihat ada aktivitas mencurigakan, hubungi kami di: daniel.notthoff@gmail.com",
    footerNote: "Ini adalah pesan otomatis. Mohon jangan langsung membalas email ini.",
  },
  vi: {
    subject: "🔑 Đặt lại mật khẩu của bạn – FamWake Báo thức gia đình",
    appName: "<strong>FamWake</strong> Báo thức gia đình",
    greeting: "Xin chào!",
    intro: "Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản <strong>FamWake</strong> Báo thức gia đình của bạn.",
    instruction: "Nhấp vào nút bên dưới để đặt mật khẩu mới:",
    button: "Đặt lại mật khẩu",
    fallback: "Nếu nút không hoạt động, vui lòng sao chép liên kết này vào trình duyệt của bạn:",
    security: "⚠️ Nếu bạn <strong>không</strong> yêu cầu điều này, bạn có thể an tâm bỏ qua email này. Mật khẩu của bạn sẽ không thay đổi. Nếu bạn nhận thấy bất kỳ hoạt động đáng ngờ nào, vui lòng liên hệ với chúng tôi tại: daniel.notthoff@gmail.com",
    footerNote: "Đây là tin nhắn tự động. Vui lòng không trả lời trực tiếp email này.",
  },
  bn: {
    subject: "🔑 আপনার পাসওয়ার্ড রিসেট করুন – FamWake পারিবারিক অ্যালার্ম",
    appName: "<strong>FamWake</strong> পারিবারিক অ্যালার্ম",
    greeting: "নমস্কার!",
    intro: "আমরা আপনার <strong>FamWake</strong> পারিবারিক অ্যালার্ম অ্যাকাউন্টের পাসওয়ার্ড রিসেট করার অনুরোধ পেয়েছি।",
    instruction: "নতুন পাসওয়ার্ড সেট করতে নিচের বোতামে ক্লিক করুন:",
    button: "পাসওয়ার্ড রিসেট করুন",
    fallback: "যদি বোতামটি কাজ না করে, অনুগ্রহ করে এই লিঙ্কটি আপনার ব্রাউজারে পেস্ট করুন:",
    security: "⚠️ যদি আপনি এই অনুরোধটি <strong>না</strong> করে থাকেন, তবে আপনি নিরাপদে এই ইমেলটি এড়িয়ে যেতে পারেন। আপনার পাসওয়ার্ড অপরিবর্তিত থাকবে। আপনি যদি কোন সন্দেহজনক কার্যকলাপ লক্ষ্য করেন, অনুগ্রহ করে যোগাযোগ করুন: daniel.notthoff@gmail.com",
    footerNote: "এটি একটি স্বয়ংক্রিয় বার্তা। দয়া করে এই ইমেলের সরাসরি উত্তর দেবেন না।",
  },
  mr: {
    subject: "🔑 तुमचा पासवर्ड रीसेट करा – FamWake कौटुंबिक अलार्म",
    appName: "<strong>FamWake</strong> कौटुंबिक अलार्म",
    greeting: "नमस्कार!",
    intro: "आम्हाला तुमच्या <strong>FamWake</strong> कौटुंबिक अलार्म खात्याचा पासवर्ड रीसेट करण्याची विनंती प्राप्त झाली आहे.",
    instruction: "नवीन पासवर्ड सेट करण्यासाठी खालील बटणावर क्लिक करा:",
    button: "पासवर्ड रीसेट करा",
    fallback: "बटण काम करत नसल्यास, कृपया ही लिंक तुमच्या ब्राउझरमध्ये पेस्ट करा:",
    security: "⚠️ जर तुम्ही ही विनंती <strong>केली नसेल</strong>, तर तुम्ही या ईमेलकडे सुरक्षितपणे दुर्लक्ष करू शकता. तुमचा पासवर्ड बदलणार नाही. तुम्हाला काही संशयास्पद क्रियाकलाप दिसल्यास, कृपया आमच्याशी संपर्क साधा: daniel.notthoff@gmail.com",
    footerNote: "हा एक स्वयंचलित संदेश आहे. कृपया या ईमेलला थेट उत्तर देऊ नका.",
  },
  hi: {
    subject: "🔑 अपना पासवर्ड रीसेट करें - FamWake पारिवारिक अलार्म",
    appName: "<strong>FamWake</strong> पारिवारिक अलार्म",
    greeting: "नमस्ते!",
    intro: "हमें आपके <strong>FamWake</strong> पारिवारिक अलार्म खाते का पासवर्ड रीसेट करने का अनुरोध प्राप्त हुआ है।",
    instruction: "नया पासवर्ड सेट करने के लिए नीचे दिए गए बटन पर क्लिक करें:",
    button: "पासवर्ड रीसेट करें",
    fallback: "यदि बटन काम नहीं करता है, तो कृपया इस लिंक को अपने ब्राउज़र में कॉपी करें:",
    security: "⚠️ अगर आपने यह अनुरोध <strong>नहीं</strong> किया है, तो आप इस ईमेल को अनदेखा कर सकते हैं। आपका पासवर्ड नहीं बदलेगा। यदि आपको कोई संदिग्ध गतिविधि दिखाई देती है, तो कृपया हमसे संपर्क करें: daniel.notthoff@gmail.com",
    footerNote: "यह एक स्वचालित संदेश है। कृपया इस ईमेल का सीधा जवाब न दें।",
  },
  "zh-CN": {
    subject: "🔑 重置您的密码 – FamWake 家庭闹钟",
    appName: "<strong>FamWake</strong> 家庭闹钟",
    greeting: "你好！",
    intro: "我们收到了重置您 <strong>FamWake</strong> 家庭闹钟帐户密码的请求。",
    instruction: "请点击下方按钮设置新密码：",
    button: "重置密码",
    fallback: "如果按钮不起作用，请将此链接复制到您的浏览器中：",
    security: "⚠️ 如果您<strong>没有</strong>提交此请求，可以安全地忽略此邮件。您的密码将保持不变。如果您发现任何可疑活动，请联系我们：daniel.notthoff@gmail.com",
    footerNote: "这是一封自动发送的邮件。请不要直接回复。",
  },
  ko: {
    subject: "🔑 비밀번호 재설정 – FamWake 가족 알람시계",
    appName: "<strong>FamWake</strong> 가족 알람시계",
    greeting: "안녕하세요!",
    intro: "귀하의 <strong>FamWake</strong> 가족 알람시계 계정 비밀번호 재설정 요청을 받았습니다.",
    instruction: "새 비밀번호를 설정하려면 아래 버튼을 클릭하세요.",
    button: "비밀번호 재설정",
    fallback: "버튼이 작동하지 않으면 다음 링크를 브라우저에 복사해 붙여넣으세요:",
    security: "⚠️ 본인이 요청하지 <strong>않은</strong> 경우 이 이메일을 무시하셔도 됩니다. 귀하의 비밀번호는 변경되지 않습니다. 의심스러운 활동을 발견한 경우 다음 주소로 문의해 주세요: daniel.notthoff@gmail.com",
    footerNote: "이것은 자동 생성된 메시지입니다. 이 이메일에 직접 회신하지 마십시오.",
  },
};

const EMAIL_CONTENT_CONFIRM = {
  de: {
    subject: "✅ Passwort erfolgreich geändert – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Hallo!",
    intro: "Dein Passwort für dein <strong>FamWake</strong> Familienwecker-Konto wurde erfolgreich geändert.",
    instruction: "Du kannst dich ab sofort mit deinem neuen Passwort in der App anmelden.",
    security: "⚠️ Falls du dein Passwort <strong>nicht</strong> geändert hast, wende dich bitte umgehend an uns: daniel.notthoff@gmail.com",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "✅ Password successfully changed – FamWake Family Alarm",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Hello!",
    intro: "The password for your <strong>FamWake</strong> Family Alarm account has been successfully changed.",
    instruction: "You can now log in to the app with your new password.",
    security: "⚠️ If you did <strong>not</strong> change your password, please contact us immediately: daniel.notthoff@gmail.com",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
  es: {
    subject: "✅ Contraseña cambiada con éxito – FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "¡Hola!",
    intro: "La contraseña de tu cuenta de <strong>FamWake</strong> Despertador Familiar se ha cambiado con éxito.",
    instruction: "Ya puedes iniciar sesión en la aplicación con tu nueva contraseña.",
    security: "⚠️ Si <strong>no</strong> fuiste tú quien cambió la contraseña, ponte en contacto con nosotros inmediatamente: daniel.notthoff@gmail.com",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no respondas directamente a este correo.",
  },
  fr: {
    subject: "✅ Mot de passe modifié avec succès – FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bonjour !",
    intro: "Le mot de passe de ton compte <strong>FamWake</strong> Réveil Familial a été modifié avec succès.",
    instruction: "Tu peux désormais te connecter à l'application avec ton nouveau mot de passe.",
    security: "⚠️ Si tu n'as <strong>pas</strong> modifié ton mot de passe, merci de nous contacter immédiatement : daniel.notthoff@gmail.com",
    footerNote: "Ceci est un message généré automatiquement. Merci de ne pas répondre directement à cet e-mail.",
  },
  it: {
    subject: "✅ Password modificata con successo – FamWake Sveglia Famiglia",
    appName: "<strong>FamWake</strong> Sveglia Famiglia",
    greeting: "Ciao!",
    intro: "La password del tuo account <strong>FamWake</strong> Sveglia Famiglia è stata modificata con successo.",
    instruction: "Ora puoi accedere all'app con la tua nuova password.",
    security: "⚠️ Se <strong>non</strong> hai modificato tu la password, contattaci immediatamente: daniel.notthoff@gmail.com",
    footerNote: "Questo è un messaggio generato automaticamente. Si prega di non rispondere direttamente a questa email.",
  },
  da: {
    subject: "✅ Adgangskode ændret – FamWake Familievækker",
    appName: "<strong>FamWake</strong> Familievækker",
    greeting: "Hej!",
    intro: "Adgangskoden til din <strong>FamWake</strong> Familievækker-konto er blevet ændret.",
    instruction: "Du kan nu logge ind på appen med din nye adgangskode.",
    security: "⚠️ Hvis du <strong>ikke</strong> har ændret din adgangskode, bedes du kontakte os straks: daniel.notthoff@gmail.com",
    footerNote: "Dette er en automatisk genereret besked. Svar venligst ikke direkte på denne e-mail.",
  },
  ja: {
    subject: "✅ パスワードが変更されました – FamWake ファミリーアラーム",
    appName: "<strong>FamWake</strong> ファミリーアラーム",
    greeting: "こんにちは！",
    intro: "<strong>FamWake</strong> ファミリーアラームのアカウントのパスワードが正常に変更されました。",
    instruction: "新しいパスワードでアプリにログインできます。",
    security: "⚠️ パスワードを<strong>変更していない</strong>場合は、すぐにご連絡ください：daniel.notthoff@gmail.com",
    footerNote: "これは自動生成されたメッセージです。このメールに直接返信しないでください。",
  },
  nl: {
    subject: "✅ Wachtwoord succesvol gewijzigd – FamWake Gezinswekker",
    appName: "<strong>FamWake</strong> Gezinswekker",
    greeting: "Hallo!",
    intro: "Het wachtwoord van je <strong>FamWake</strong> Gezinswekker-account is succesvol gewijzigd.",
    instruction: "Je kunt nu inloggen in de app met je nieuwe wachtwoord.",
    security: "⚠️ Als je je wachtwoord <strong>niet</strong> hebt gewijzigd, neem dan onmiddellijk contact met ons op: daniel.notthoff@gmail.com",
    footerNote: "Dit is een automatisch gegenereerd bericht. Reageer niet rechtstreeks op deze e-mail.",
  },
  no: {
    subject: "✅ Passord endret – FamWake Familievekker",
    appName: "<strong>FamWake</strong> Familievekker",
    greeting: "Hei!",
    intro: "Passordet for din <strong>FamWake</strong> Familievekker-konto er endret.",
    instruction: "Du kan nå logge inn i appen med ditt nye passord.",
    security: "⚠️ Hvis du <strong>ikke</strong> har endret passordet ditt, kontakt oss umiddelbart: daniel.notthoff@gmail.com",
    footerNote: "Dette er en automatisk generert melding. Vennligst ikke svar direkte på denne e-posten.",
  },
  pl: {
    subject: "✅ Hasło zostało zmienione – FamWake Budzik Rodzinny",
    appName: "<strong>FamWake</strong> Budzik Rodzinny",
    greeting: "Cześć!",
    intro: "Hasło do Twojego konta <strong>FamWake</strong> Budzik Rodzinny zostało pomyślnie zmienione.",
    instruction: "Możesz teraz zalogować się do aplikacji za pomocą nowego hasła.",
    security: "⚠️ Jeśli <strong>nie</strong> zmieniałeś hasła, skontaktuj się z nami natychmiast: daniel.notthoff@gmail.com",
    footerNote: "To jest wiadomość wygenerowana automatycznie. Prosimy nie odpowiadać bezpośrednio na ten e-mail.",
  },
  pt: {
    subject: "✅ Senha alterada com sucesso – FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "Olá!",
    intro: "A senha da sua conta <strong>FamWake</strong> Despertador Familiar foi alterada com sucesso.",
    instruction: "Agora você pode fazer login no aplicativo com sua nova senha.",
    security: "⚠️ Se <strong>não</strong> foi você que alterou a senha, entre em contato conosco imediatamente: daniel.notthoff@gmail.com",
    footerNote: "Esta é uma mensagem gerada automaticamente. Por favor, não responda diretamente a este e-mail.",
  },
  ru: {
    subject: "✅ Пароль успешно изменён – FamWake Семейный Будильник",
    appName: "<strong>FamWake</strong> Семейный Будильник",
    greeting: "Привет!",
    intro: "Пароль твоей учётной записи <strong>FamWake</strong> Семейный Будильник был успешно изменён.",
    instruction: "Теперь ты можешь войти в приложение с новым паролем.",
    security: "⚠️ Если ты <strong>не</strong> менял пароль, немедленно свяжись с нами: daniel.notthoff@gmail.com",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечай на него напрямую.",
  },
  sv: {
    subject: "✅ Lösenord ändrat – FamWake Familjens Väckarklocka",
    appName: "<strong>FamWake</strong> Familjens Väckarklocka",
    greeting: "Hej!",
    intro: "Lösenordet för ditt <strong>FamWake</strong> Familjens Väckarklocka-konto har ändrats.",
    instruction: "Du kan nu logga in i appen med ditt nya lösenord.",
    security: "⚠️ Om du <strong>inte</strong> har ändrat ditt lösenord, kontakta oss omedelbart: daniel.notthoff@gmail.com",
    footerNote: "Detta är ett automatiskt genererat meddelande. Svara inte direkt på detta e-postmeddelande.",
  },
  tr: {
    subject: "✅ Şifre başarıyla değiştirildi – FamWake Aile Alarmı",
    appName: "<strong>FamWake</strong> Aile Alarmı",
    greeting: "Merhaba!",
    intro: "<strong>FamWake</strong> Aile Alarmı hesabının şifresi başarıyla değiştirildi.",
    instruction: "Artık yeni şifrenle uygulamaya giriş yapabilirsin.",
    security: "⚠️ Şifreni <strong>sen</strong> değiştirmediysen, lütfen hemen bizimle iletişime geç: daniel.notthoff@gmail.com",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt verme.",
  },
  uk: {
    subject: "✅ Пароль успішно змінено – FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Привіт!",
    intro: "Пароль твого облікового запису <strong>FamWake</strong> Сімейний Будильник було успішно змінено.",
    instruction: "Тепер ти можеш увійти в застосунок з новим паролем.",
    security: "⚠️ Якщо ти <strong>не</strong> змінював пароль, негайно звʼяжися з нами: daniel.notthoff@gmail.com",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідай на нього безпосередньо.",
  },
  id: {
    subject: "✅ Kata sandi berhasil diubah – FamWake Jam Alarm Keluarga",
    appName: "<strong>FamWake</strong> Jam Alarm Keluarga",
    greeting: "Halo!",
    intro: "Kata sandi untuk akun <strong>FamWake</strong> Jam Alarm Keluarga kamu telah berhasil diubah.",
    instruction: "Kamu sekarang dapat masuk ke aplikasi dengan kata sandi baru.",
    security: "⚠️ Jika kamu <strong>tidak</strong> mengubah kata sandi ini, segera hubungi kami: daniel.notthoff@gmail.com",
    footerNote: "Ini adalah pesan otomatis. Mohon jangan langsung membalas email ini.",
  },
  vi: {
    subject: "✅ Đổi mật khẩu thành công – FamWake Báo thức gia đình",
    appName: "<strong>FamWake</strong> Báo thức gia đình",
    greeting: "Xin chào!",
    intro: "Mật khẩu cho tài khoản <strong>FamWake</strong> Báo thức gia đình của bạn đã được thay đổi thành công.",
    instruction: "Bây giờ bạn có thể đăng nhập vào ứng dụng bằng mật khẩu mới của mình.",
    security: "⚠️ Nếu bạn <strong>không</strong> thay đổi mật khẩu của mình, hãy liên hệ ngay với chúng tôi: daniel.notthoff@gmail.com",
    footerNote: "Đây là tin nhắn tự động. Vui lòng không trả lời trực tiếp email này.",
  },
  bn: {
    subject: "✅ পাসওয়ার্ড সফলভাবে পরিবর্তিত হয়েছে – FamWake পারিবারিক অ্যালার্ম",
    appName: "<strong>FamWake</strong> পারিবারিক অ্যালার্ম",
    greeting: "নমস্কার!",
    intro: "আপনার <strong>FamWake</strong> পারিবারিক অ্যালার্ম অ্যাকাউন্টের পাসওয়ার্ড সফলভাবে পরিবর্তন করা হয়েছে।",
    instruction: "আপনি এখন আপনার নতুন পাসওয়ার্ড দিয়ে অ্যাপে লগ ইন করতে পারেন।",
    security: "⚠️ যদি আপনি আপনার পাসওয়ার্ড পরিবর্তন <strong>না</strong> করে থাকেন, তবে অবিলম্বে আমাদের সাথে যোগাযোগ করুন: daniel.notthoff@gmail.com",
    footerNote: "এটি একটি স্বয়ংক্রিয় বার্তা। দয়া করে এই ইমেলের সরাসরি উত্তর দেবেন না।",
  },
  mr: {
    subject: "✅ पासवर्ड यशस्वीरित्या बदलला – FamWake कौटुंबिक अलार्म",
    appName: "<strong>FamWake</strong> कौटुंबिक अलार्म",
    greeting: "नमस्कार!",
    intro: "तुमच्या <strong>FamWake</strong> कौटुंबिक अलार्म खात्याचा पासवर्ड यशस्वीरित्या बदलला आहे.",
    instruction: "तुम्ही आता तुमच्या नवीन पासवर्डने ॲपमध्ये लॉग इन करू शकता.",
    security: "⚠️ जर तुम्ही तुमचा पासवर्ड <strong>बदलला नसेल</strong>, तर कृपया आमच्याशी त्वरित संपर्क साधा: daniel.notthoff@gmail.com",
    footerNote: "हा एक स्वयंचलित संदेश आहे. कृपया या ईमेलला थेट उत्तर देऊ नका.",
  },
  hi: {
    subject: "✅ पासवर्ड सफलतापूर्वक बदला गया – FamWake पारिवारिक अलार्म",
    appName: "<strong>FamWake</strong> पारिवारिक अलार्म",
    greeting: "नमस्ते!",
    intro: "आपके <strong>FamWake</strong> पारिवारिक अलार्म खाते का पासवर्ड सफलतापूर्वक बदल दिया गया है।",
    instruction: "अब आप अपने नए पासवर्ड के साथ ऐप में लॉग इन कर सकते हैं।",
    security: "⚠️ अगर आपने अपना पासवर्ड <strong>नहीं</strong> बदला है, तो कृपया तुरंत हमसे संपर्क करें: daniel.notthoff@gmail.com",
    footerNote: "यह एक स्वचालित संदेश है। कृपया इस ईमेल का सीधा जवाब न दें।",
  },
  "zh-CN": {
    subject: "✅ 密码更改成功 – FamWake 家庭闹钟",
    appName: "<strong>FamWake</strong> 家庭闹钟",
    greeting: "你好！",
    intro: "您的 <strong>FamWake</strong> 家庭闹钟帐户密码已成功更改。",
    instruction: "您现在可以使用新密码登录应用程序。",
    security: "⚠️ 如果您<strong>没有</strong>更改密码，请立即联系我们：daniel.notthoff@gmail.com",
    footerNote: "这是一封自动发送的邮件。请不要直接回复。",
  },
  ko: {
    subject: "✅ 비밀번호가 성공적으로 변경되었습니다 – FamWake 가족 알람시계",
    appName: "<strong>FamWake</strong> 가족 알람시계",
    greeting: "안녕하세요!",
    intro: "귀하의 <strong>FamWake</strong> 계정 비밀번호가 성공적으로 변경되었습니다.",
    instruction: "이제 새 비밀번호로 앱에 로그인할 수 있습니다.",
    security: "⚠️ 본인이 비밀번호를 변경하지 <strong>않은</strong> 경우 즉시 다음 주소로 연락해 주세요: daniel.notthoff@gmail.com",
    footerNote: "이것은 자동 생성된 메시지입니다. 이 이메일에 직접 회신하지 마십시오.",
  },
};

function buildEmailHtml(link, lang) {
  const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <div style="text-align: center; margin: 30px 0;">
        <a href="${link}" style="background-color: ${BRAND_BLUE}; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px;">${t.button}</a>
      </div>
      <p style="font-size: 12px; color: #666;">${t.fallback}</p>
      <p style="font-size: 12px; color: #888; word-break: break-all;">${link}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendBrandedResetEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const email = request.data?.email;
    if (request.auth && email && request.auth.token.email &&
      email.trim().toLowerCase() !== request.auth.token.email.toLowerCase()) {
      throw new HttpsError("permission-denied", "EMAIL_MISMATCH");
    }
    const lang = resolveLanguage(request.data?.language);
    console.log(`Email request for ${email?.trim()} with language: ${request.data?.language} -> mapped to: ${lang}`);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "reset");
      const linkOriginal = await admin.auth().generatePasswordResetLink(email.trim());
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");

      if (lang !== "de") {
        link = link.replace("reset-password.html", `reset-password-${lang}.html`);
      }

      const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
        to: [email.trim()],
        subject: t.subject,
        html: buildEmailHtml(link, lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send email via Resend.");
      }

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendBrandedResetEmail:", err);
      const code = String(err.code || (err.errorInfo && err.errorInfo.code) || "");
      console.log("Extracted error code:", code);

      if (code.includes("invalid-email") || code.includes("argument")) {
        throw new HttpsError("invalid-argument", "INVALID_EMAIL");
      } else if (code.includes("user-not-found") || code.includes("not-found")) {
        console.log("Mapping to USER_NOT_FOUND");
        throw new HttpsError("not-found", "USER_NOT_FOUND");
      } else if (code.includes("too-many-requests") || code.includes("resource-exhausted")) {
        throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      }
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

function buildConfirmEmailHtml(lang) {
  const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendBrandedConfirmationEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    const email = request.data?.email;
    const lang = resolveLanguage(request.data?.language);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "confirm");
      const t = EMAIL_CONTENT_CONFIRM[lang] || EMAIL_CONTENT_CONFIRM.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
        to: [email.trim()],
        subject: t.subject,
        html: buildConfirmEmailHtml(lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send confirmation email via Resend.");
      }

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendBrandedConfirmationEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

const EMAIL_CONTENT_VERIFY = {
  de: {
    subject: "🚀 Bestätige dein FamWake Familienwecker-Konto",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "Willkommen bei FamWake Familienwecker!",
    intro: "Vielen Dank für deine Registrierung bei <strong>FamWake</strong> Familienwecker. Wir freuen uns darauf, dir und deiner Familie zu einem entspannten Morgen ohne Chaos zu verhelfen!",
    instruction: "Bitte bestätige deine E-Mail-Adresse, um dein Konto zu aktivieren:",
    button: "E-Mail-Adresse bestätigen",
    fallback: "Falls der Button nicht funktioniert, kopiere bitte diesen Link in deinen Browser:",
    privacy: "<strong>Hinweis:</strong> Aus Datenschutzgründen werden dieser Link und deine unbestätigten Registrierungsdaten automatisch nach 48 Stunden gelöscht, falls keine Aktivierung erfolgt.",
    security: "Falls du dieses Konto nicht erstellt hast, kannst du diese E-Mail einfach ignorieren. Es werden keine Daten dauerhaft von dir gespeichert.",
    footerNote: "Dies ist eine automatisch generierte Nachricht. Bitte antworte nicht direkt auf diese E-Mail.",
  },
  en: {
    subject: "🚀 Confirm your FamWake Family Alarm account",
    appName: "<strong>FamWake</strong> Family Alarm",
    greeting: "Welcome to FamWake Family Alarm!",
    intro: "Thank you for registering with <strong>FamWake</strong> Family Alarm. We look forward to helping you and your family start the day stress-free!",
    instruction: "Please confirm your email address to activate your account:",
    button: "Confirm email address",
    fallback: "If the button doesn't work, please paste this link into your browser:",
    privacy: "<strong>Note:</strong> For privacy reasons, this link and your unconfirmed registration data will be automatically deleted after 48 hours if no activation occurs.",
    security: "If you did not create this account, you can safely ignore this email. No data will be permanently stored.",
    footerNote: "This is an automated message. Please do not reply directly to this email.",
  },
  es: {
    subject: "🚀 Confirma tu cuenta de FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "¡Bienvenido a FamWake Despertador Familiar!",
    intro: "Gracias por registrarte en <strong>FamWake</strong> Despertador Familiar. ¡Estamos deseando ayudaros a ti y a tu familia a tener una mañana relajada y sin caos!",
    instruction: "Por favor, confirma tu dirección de correo electrónico para activar tu cuenta:",
    button: "Confirmar correo electrónico",
    fallback: "Si el botón no funciona, copia este enlace en tu navegador:",
    privacy: "<strong>Nota:</strong> Por razones de privacidad, este enlace y tus datos de registro no confirmados se eliminarán automáticamente después de 48 horas si no se realiza la activación.",
    security: "Si no has creado esta cuenta, puedes ignorar este correo de forma segura. No se guardará ningún dato de forma permanente.",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no respondas directamente a este correo.",
  },
  fr: {
    subject: "🚀 Confirme ton compte FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bienvenue chez FamWake Réveil Familial !",
    intro: "Merci de t'être inscrit à <strong>FamWake</strong> Réveil Familial. Nous avons hâte de t'aider, toi et ta famille, à passer des matinées détendues et sans chaos !",
    instruction: "Merci de confirmer ton adresse e-mail pour activer ton compte :",
    button: "Confirmer l'adresse e-mail",
    fallback: "Si le bouton ne fonctionne pas, merci de copier ce lien dans ton navigateur :",
    privacy: "<strong>Note :</strong> Pour des raisons de confidentialité, ce lien et tes données d'inscription non confirmées seront automatiquement supprimés après 48 heures si aucune activation n'a lieu.",
    security: "Si tu n'as pas créé ce compte, tu peux ignorer cet e-mail. Aucune donnée ne sera conservée de façon permanente.",
    footerNote: "Ceci est un message généré automatiquement. Merci de ne pas répondre directement à cet e-mail.",
  },
  it: {
    subject: "🚀 Conferma il tuo account FamWake Sveglia Famiglia",
    appName: "<strong>FamWake</strong> Sveglia Famiglia",
    greeting: "Benvenuti in FamWake Sveglia Famiglia!",
    intro: "Grazie per esserti registrato a <strong>FamWake</strong> Sveglia Famiglia. Non vediamo l'ora di aiutare te e la tua famiglia a trascorrere una mattinata rilassante e senza caos!",
    instruction: "Conferma il tuo indirizzo email per attivare il tuo account:",
    button: "Conferma indirizzo email",
    fallback: "Se il pulsante non funziona, copia questo link nel tuo browser:",
    privacy: "<strong>Nota:</strong> Per motivi di privacy, questo link e i tuoi dati di registrazione non confermati verranno cancellati automaticamente dopo 48 ore se non viene effettuata l'attivazione.",
    security: "Se non hai creato tu questo account, puoi ignorare questa email. Nessun dato verrà memorizzato in modo permanente.",
    footerNote: "Questo è un messaggio generato automaticamente. Si prega di non rispondere direttamente a questa email.",
  },
  da: {
    subject: "🚀 Bekræft din FamWake Familievækker-konto",
    appName: "<strong>FamWake</strong> Familievækker",
    greeting: "Velkommen til FamWake Familievækker!",
    intro: "Tak for din tilmelding til <strong>FamWake</strong> Familievækker. Vi glæder os til at hjælpe dig og din familie til en rolig morgen uden kaos!",
    instruction: "Bekræft venligst din e-mailadresse for at aktivere din konto:",
    button: "Bekræft e-mailadresse",
    fallback: "Hvis knappen ikke virker, skal du kopiere dette link til din browser:",
    privacy: "<strong>Bemærk:</strong> Af hensyn til privatlivets fred vil dette link og dine ubekræftede registreringsdata automatisk blive slettet efter 48 timer, hvis ingen aktivering sker.",
    security: "Hvis du ikke har oprettet denne konto, kan du blot ignorere denne e-mail. Der vil ikke blive gemt nogen data permanent.",
    footerNote: "Dette er en automatisk genereret besked. Svar venligst ikke direkte på denne e-mail.",
  },
  ja: {
    subject: "🚀 FamWake ファミリーアラームのアカウントを確認してください",
    appName: "<strong>FamWake</strong> ファミリーアラーム",
    greeting: "FamWake ファミリーアラームへようこそ！",
    intro: "<strong>FamWake</strong> ファミリーアラームにご登録いただきありがとうございます。あなたとご家族が毎朝穏やかに過ごせるようお手伝いします！",
    instruction: "アカウントを有効化するには、メールアドレスを確認してください：",
    button: "メールアドレスを確認",
    fallback: "ボタンが機能しない場合は、このリンクをブラウザにコピーしてください：",
    privacy: "<strong>注意：</strong>プライバシー保護のため、このリンクと未確認の登録データは、認証が行われない場合や48時間後に自動的に削除されます。",
    security: "このアカウントを作成していない場合は、このメールを無視してください。データは永続的に保存されません。",
    footerNote: "これは自動生成されたメッセージです。このメールに直接返信しないでください。",
  },
  nl: {
    subject: "🚀 Bevestig je FamWake Gezinswekker-account",
    appName: "<strong>FamWake</strong> Gezinswekker",
    greeting: "Welkom bij FamWake Gezinswekker!",
    intro: "Bedankt voor je registratie bij <strong>FamWake</strong> Gezinswekker. We kijken ernaar uit om jou en je gezin te helpen elke ochtend ontspannen te starten!",
    instruction: "Bevestig je e-mailadres om je account te activeren:",
    button: "E-mailadres bevestigen",
    fallback: "Als de knop niet werkt, kopieer dan deze link naar je browser:",
    privacy: "<strong>Opmerking:</strong> Om privacyredenen worden deze link en je onbevestigde registratiegegevens automatisch verwijderd na 48 uur als er geen activering plaatsvindt.",
    security: "Als je dit account niet hebt aangemaakt, kun je deze e-mail gewoon negeren. Er worden geen gegevens permanent opgeslagen.",
    footerNote: "Dit is een automatisch gegenereerd bericht. Reageer niet rechtstreeks op deze e-mail.",
  },
  no: {
    subject: "🚀 Bekreft FamWake Familievekker-kontoen din",
    appName: "<strong>FamWake</strong> Familievekker",
    greeting: "Velkommen til FamWake Familievekker!",
    intro: "Takk for at du registrerte deg på <strong>FamWake</strong> Familievekker. Vi gleder oss til å hjelpe deg og familien din med å starte morgenen avslappet og stressfritt!",
    instruction: "Bekreft e-postadressen din for å aktivere kontoen din:",
    button: "Bekreft e-postadresse",
    fallback: "Hvis knappen ikke fungerer, lim inn denne lenken i nettleseren din:",
    privacy: "<strong>Merk:</strong> Av personvernhensyn vil denne lenken og dine ubekreftede registreringsdata slettes automatisk etter 48 timer hvis ingen aktivering skjer.",
    security: "Hvis du ikke opprettet denne kontoen, kan du trygt ignorere denne e-posten. Ingen data vil bli lagret permanent.",
    footerNote: "Dette er en automatisk generert melding. Vennligst ikke svar direkte på denne e-posten.",
  },
  pl: {
    subject: "🚀 Potwierdź swoje konto FamWake Budzik Rodzinny",
    appName: "<strong>FamWake</strong> Budzik Rodzinny",
    greeting: "Witaj w FamWake Budzik Rodzinny!",
    intro: "Dziękujemy za rejestrację w <strong>FamWake</strong> Budzik Rodzinny. Cieszymy się, że pomożemy Tobie i Twojej rodzinie zacząć każdy poranek spokojnie i bez chaosu!",
    instruction: "Potwierdź swój adres e-mail, aby aktywować konto:",
    button: "Potwierdź adres e-mail",
    fallback: "Jeśli przycisk nie działa, skopiuj ten link do przeglądarki:",
    privacy: "<strong>Uwaga:</strong> Ze względów prywatności ten link i Twoje niepotwierdzone dane rejestracyjne zostaną automatycznie usunięte po 48 godzinach, jeśli nie nastąpi aktywacja.",
    security: "Jeśli nie tworzyłeś tego konta, możesz zignorować ten e-mail. Żadne dane nie będą trwale przechowywane.",
    footerNote: "To jest wiadomość wygenerowana automatycznie. Prosimy nie odpowiadać bezpośrednio na ten e-mail.",
  },
  pt: {
    subject: "🚀 Confirme sua conta FamWake Despertador Familiar",
    appName: "<strong>FamWake</strong> Despertador Familiar",
    greeting: "Bem-vindo ao FamWake Despertador Familiar!",
    intro: "Obrigado por se registrar no <strong>FamWake</strong> Despertador Familiar. Estamos ansiosos para ajudar você e sua família a ter manhãs tranquilas e sem caos!",
    instruction: "Por favor, confirme seu endereço de e-mail para ativar sua conta:",
    button: "Confirmar endereço de e-mail",
    fallback: "Se o botão não funcionar, copie este link para o seu navegador:",
    privacy: "<strong>Nota:</strong> Por razões de privacidade, este link e seus dados de registro não confirmados serão excluídos automaticamente após 48 horas se nenhuma ativação ocorrer.",
    security: "Se você não criou esta conta, pode ignorar este e-mail. Nenhum dado será armazenado permanentemente.",
    footerNote: "Esta é uma mensagem gerada automaticamente. Por favor, não responda diretamente a este e-mail.",
  },
  ru: {
    subject: "🚀 Подтверди свою учётную запись FamWake Семейный Будильник",
    appName: "<strong>FamWake</strong> Семейный Будильник",
    greeting: "Добро пожаловать в FamWake Семейный Будильник!",
    intro: "Спасибо за регистрацию в <strong>FamWake</strong> Семейный Будильник. Мы рады помочь тебе и твоей семье начинать каждое утро спокойно и без хаоса!",
    instruction: "Подтверди свой адрес электронной почты для активации учётной записи:",
    button: "Подтвердить адрес электронной почты",
    fallback: "Если кнопка не работает, скопируй эту ссылку в браузер:",
    privacy: "<strong>Примечание:</strong> В целях защиты конфиденциальности эта ссылка и твои неподтверждённые данные будут автоматически удалены через 48 часов, если активация не будет выполнена.",
    security: "Если ты не создавал этот аккаунт, просто проигнорируй это письмо. Никакие данные не будут сохранены.",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечай на него напрямую.",
  },
  sv: {
    subject: "🚀 Bekräfta ditt FamWake Familjens Väckarklocka-konto",
    appName: "<strong>FamWake</strong> Familjens Väckarklocka",
    greeting: "Välkommen till FamWake Familjens Väckarklocka!",
    intro: "Tack för att du registrerade dig på <strong>FamWake</strong> Familjens Väckarklocka. Vi ser fram emot att hjälpa dig och din familj att börja varje morgon avslappnad och utan kaos!",
    instruction: "Bekräfta din e-postadress för att aktivera ditt konto:",
    button: "Bekräfta e-postadress",
    fallback: "Om knappen inte fungerar, kopiera den här länken till din webbläsare:",
    privacy: "<strong>Obs:</strong> Av integritetsskydd kommer den här länken och dina obekräftade registreringsdata att raderas automatiskt efter 48 timmar om ingen aktivering sker.",
    security: "Om du inte skapade det här kontot kan du ignorera det här e-postmeddelandet. Inga data kommer att lagras permanent.",
    footerNote: "Detta är ett automatiskt genererat meddelande. Svara inte direkt på detta e-postmeddelande.",
  },
  tr: {
    subject: "🚀 FamWake Aile Alarmı hesabını onayla",
    appName: "<strong>FamWake</strong> Aile Alarmı",
    greeting: "FamWake Aile Alarmı'na hoş geldin!",
    intro: "<strong>FamWake</strong> Aile Alarmı'na kaydolduğun için teşekkürler. Her sabah sakin ve stressiz bir başlangıç yapman için sana ve ailene yardımcı olmayı sabırsızlıkla bekliyoruz!",
    instruction: "Hesabını etkinleştirmek için lütfen e-posta adresini onayla:",
    button: "E-posta adresini onayla",
    fallback: "Düğme çalışmıyorsa, bu bağlantıyı tarayıcına kopyalayabilirsin:",
    privacy: "<strong>Not:</strong> Gizlilik nedeniyle, bu bağlantı ve onaylanmamış kayıt verilerin, aktivasyon gerçekleşmezse 48 saat sonra otomatik olarak silinecektir.",
    security: "Bu hesabı sen oluşturmadıysan, bu e-postayı görmezden gelebilirsin. Hiçbir veri kalıcı olarak saklanmayacak.",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt verme.",
  },
  uk: {
    subject: "🚀 Підтвердь свій обліковий запис FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Ласкаво просимо до FamWake Сімейний Будильник!",
    intro: "Дякуємо за реєстрацію в <strong>FamWake</strong> Сімейний Будильник. Ми раді допомогти тобі та твоїй родині починати кожен ранок спокійно та без хаосу!",
    instruction: "Підтвердь свою електронну адресу для активації облікового запису:",
    button: "Підтвердити електронну адресу",
    fallback: "Якщо кнопка не працює, скопіюй це посилання у браузер:",
    privacy: "<strong>Примітка:</strong> З міркувань конфіденційності це посилання та твої непідтверджені дані реєстрації будуть автоматично видалені через 48 годин, якщо активація не буде виконана.",
    security: "Якщо ти не створював цей обліковий запис, просто ігноруй цей лист. Жодні дані не будуть збережені постійно.",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідай на нього безпосередньо.",
  },
  id: {
    subject: "🚀 Konfirmasikan akun Jam Alarm Keluarga FamWake kamu",
    appName: "<strong>FamWake</strong> Jam Alarm Keluarga",
    greeting: "Selamat datang di Jam Alarm Keluarga FamWake!",
    intro: "Terima kasih telah mendaftar di <strong>FamWake</strong> Jam Alarm Keluarga. Kami berharap dapat membantu kamu dan keluarga memulai hari tanpa stres!",
    instruction: "Harap konfirmasi alamat email kamu untuk mengaktifkan akun:",
    button: "Konfirmasi alamat email",
    fallback: "Jika tombolnya tidak berfungsi, silakan salin tautan ini ke browser kamu:",
    privacy: "<strong>Catatan:</strong> Demi alasan privasi, tautan ini dan data pendaftaran yang belum dikonfirmasi akan otomatis terhapus setelah 48 jam jika akun tidak diaktifkan.",
    security: "Jika kamu tidak membuat akun ini, kamu bisa mengabaikan email ini dengan aman. Tidak ada data yang akan disimpan secara permanen.",
    footerNote: "Ini adalah pesan otomatis. Mohon jangan langsung membalas email ini.",
  },
  vi: {
    subject: "🚀 Xác nhận tài khoản Báo thức gia đình FamWake của bạn",
    appName: "<strong>FamWake</strong> Báo thức gia đình",
    greeting: "Chào mừng bạn đến với Báo thức gia đình FamWake!",
    intro: "Cảm ơn bạn đã đăng ký tài khoản <strong>FamWake</strong> Báo thức gia đình. Chúng tôi rất mong được giúp bạn và gia đình bắt đầu ngày mới thật thoải mái!",
    instruction: "Vui lòng xác nhận địa chỉ email của bạn để kích hoạt tài khoản:",
    button: "Xác nhận địa chỉ email",
    fallback: "Nếu nút không hoạt động, vui lòng sao chép liên kết này vào trình duyệt của bạn:",
    privacy: "<strong>Lưu ý:</strong> Vì lý do bảo mật, liên kết này và dữ liệu đăng ký chưa được xác nhận của bạn sẽ tự động bị xóa sau 48 giờ nếu tài khoản không được kích hoạt.",
    security: "Nếu bạn không tạo tài khoản này, bạn có thể an tâm bỏ qua email này. Dữ liệu của bạn sẽ không bị lưu trữ.",
    footerNote: "Đây là tin nhắn tự động. Vui lòng không trả lời trực tiếp email này.",
  },
  bn: {
    subject: "🚀 আপনার FamWake পারিবারিক অ্যালার্ম অ্যাকাউন্ট নিশ্চিত করুন",
    appName: "<strong>FamWake</strong> পারিবারিক অ্যালার্ম",
    greeting: "FamWake পারিবারিক অ্যালার্মে স্বাগতম!",
    intro: "<strong>FamWake</strong> পারিবারিক অ্যালার্মে নিবন্ধন করার জন্য ধন্যবাদ। আমরা আপনাকে এবং আপনার পরিবারকে চাপমুক্তভাবে দিন শুরু করতে সাহায্য করার জন্য উন্মুখ!",
    instruction: "আপনার অ্যাকাউন্ট সক্রিয় করতে অনুগ্রহ করে আপনার ইমেল ঠিকানা নিশ্চিত করুন:",
    button: "ইমেল ঠিকানা নিশ্চিত করুন",
    fallback: "যদি বোতামটি কাজ না করে, অনুগ্রহ করে এই লিঙ্কটি আপনার ব্রাউজারে পেস্ট করুন:",
    privacy: "<strong>দ্রষ্টব্য:</strong> গোপনীয়তার কারণে, অ্যাকাউন্ট সক্রিয় না হলে এই লিঙ্ক এবং আপনার অনিশ্চিত নিবন্ধন ডেটা ৪৮ ঘন্টা পরে স্বয়ংক্রিয়ভাবে মুছে ফেলা হবে।",
    security: "আপনি যদি এই অ্যাকাউন্টটি তৈরি না করে থাকেন তবে আপনি নিরাপদে এই ইমেলটি এড়িয়ে যেতে পারেন৷ কোন তথ্য স্থায়ীভাবে সংরক্ষণ করা হবে না।",
    footerNote: "এটি একটি স্বয়ংক্রিয় বার্তা। দয়া করে এই ইমেলের সরাসরি উত্তর দেবেন না।",
  },
  mr: {
    subject: "🚀 तुमच्या FamWake कौटुंबिक अलार्म खात्याची पुष्टी करा",
    appName: "<strong>FamWake</strong> कौटुंबिक अलार्म",
    greeting: "FamWake कौटुंबिक अलार्ममध्ये आपले स्वागत आहे!",
    intro: "<strong>FamWake</strong> कौटुंबिक अलार्ममध्ये नोंदणी केल्याबद्दल धन्यवाद. आम्ही तुम्हाला आणि तुमच्या कुटुंबाला दिवसाची तणावमुक्त सुरुवात करण्यासाठी मदत करण्यास उत्सुक आहोत!",
    instruction: "तुमचे खाते सक्रिय करण्यासाठी कृपया तुमच्या ईमेल पत्त्याची पुष्टी करा:",
    button: "ईमेल पत्त्याची पुष्टी करा",
    fallback: "बटण काम करत नसल्यास, कृपया ही लिंक तुमच्या ब्राउझरमध्ये पेस्ट करा:",
    privacy: "<strong>टीप:</strong> गोपनीयतेच्या कारणास्तव, खाते सक्रिय न केल्यास ही लिंक आणि तुमचा पुष्टी न केलेला नोंदणी डेटा ४८ तासांनंतर आपोआप हटवला जाईल.",
    security: "तुम्ही हे खाते तयार केले नसल्यास, तुम्ही या ईमेलकडे सुरक्षितपणे दुर्लक्ष करू शकता. कोणताही डेटा कायमचा संग्रहित केला जाणार नाही.",
    footerNote: "हा एक स्वयंचलित संदेश आहे. कृपया या ईमेलला थेट उत्तर देऊ नका.",
  },
  hi: {
    subject: "🚀 अपने FamWake पारिवारिक अलार्म खाते की पुष्टि करें",
    appName: "<strong>FamWake</strong> पारिवारिक अलार्म",
    greeting: "FamWake पारिवारिक अलार्म में आपका स्वागत है!",
    intro: "<strong>FamWake</strong> पारिवारिक अलार्म में पंजीकरण करने के लिए धन्यवाद। हम आपके और आपके परिवार के दिन को तनाव-मुक्त शुरू करने में मदद करने के लिए तत्पर हैं!",
    instruction: "कृपया अपना खाता सक्रिय करने के लिए अपने ईमेल पते की पुष्टि करें:",
    button: "ईमेल पते की पुष्टि करें",
    fallback: "यदि बटन काम नहीं करता है, तो कृपया इस लिंक को अपने ब्राउज़र में कॉपी करें:",
    privacy: "<strong>नोट:</strong> गोपनीयता कारणों से, यदि खाता सक्रिय नहीं किया जाता है, तो यह लिंक और आपका अपुष्ट पंजीकरण डेटा 48 घंटों के बाद स्वचालित रूप से हटा दिया जाएगा।",
    security: "यदि आपने यह खाता नहीं बनाया है, तो आप इस ईमेल को सुरक्षित रूप से अनदेखा कर सकते हैं। कोई भी डेटा स्थायी रूप से संग्रहीत नहीं किया जाएगा।",
    footerNote: "यह एक स्वचालित संदेश है। कृपया इस ईमेल का सीधा जवाब न दें।",
  },
  "zh-CN": {
    subject: "🚀 确认您的 FamWake 家庭闹钟帐户",
    appName: "<strong>FamWake</strong> 家庭闹钟",
    greeting: "欢迎来到 FamWake 家庭闹钟！",
    intro: "感谢您注册 <strong>FamWake</strong> 家庭闹钟。我们期待帮助您和您的家人无压力地开始新的一天！",
    instruction: "请确认您的电子邮件地址以激活您的帐户：",
    button: "确认电子邮箱",
    fallback: "如果按钮不起作用，请将此链接复制到您的浏览器中：",
    privacy: "<strong>注：</strong>出于隐私原因，如果没有激活帐户，此链接和您未经确认的注册数据将在 48 小时后自动删除。",
    security: "如果您没有创建此帐户，可以安全地忽略此邮件。系统不会永久存储任何数据。",
    footerNote: "这是一封自动发送的邮件。请不要直接回复。",
  },
  ko: {
    subject: "🚀 FamWake 가족 알람시계 계정을 확인하세요",
    appName: "<strong>FamWake</strong> 가족 알람시계",
    greeting: "FamWake 가족 알람시계에 오신 것을 환영합니다!",
    intro: "<strong>FamWake</strong> 가족 알람시계에 등록해 주셔서 감사합니다. 귀하와 귀하의 가족이 스트레스 없이 하루를 시작할 수 있도록 돕겠습니다!",
    instruction: "계정을 활성화하려면 이메일 주소를 확인하세요:",
    button: "이메일 주소 확인",
    fallback: "버튼이 작동하지 않으면 다음 링크를 브라우저에 복사해 붙여넣으세요:",
    privacy: "<strong>참고:</strong> 개인정보 보호를 위해 계정이 활성화되지 않은 경우 이 링크와 확인되지 않은 등록 데이터는 48시간 후에 자동으로 삭제됩니다.",
    security: "본인이 이 계정을 만들지 않은 경우 이 이메일을 무시하셔도 됩니다. 어떤 데이터도 영구적으로 저장되지 않습니다.",
    footerNote: "이것은 자동 생성된 메시지입니다. 이 이메일에 직접 회신하지 마십시오.",
  },
};

function buildVerifyEmailHtml(link, lang) {
  const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  const l = LINK_LABELS[lang] || LINK_LABELS.en;

  return `
    <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
      <h2 style="color: ${BRAND_BLUE};">${t.greeting}</h2>
      <p>${t.intro}</p>
      <p>${t.instruction}</p>
      <div style="text-align: center; margin: 30px 0;">
        <a href="${link}" style="background-color: ${BRAND_BLUE}; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 15px;">${t.button}</a>
      </div>
      <p style="font-size: 12px; color: #666;">${t.fallback}</p>
      <p style="font-size: 12px; color: #888; word-break: break-all;">${link}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 12px; color: #888; background-color: #f9f9f9; padding: 12px; border-radius: 6px;">${t.privacy}</p>
      <p style="font-size: 12px; color: #888;">${t.security}</p>
      <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
      <p style="font-size: 11px; color: #999; text-align: center;">
        <strong>${t.appName}</strong><br>
        Daniel Notthoff<br>
        Rudolf-Virchow-Str. 20, 58300 Wetter<br><br>
        <a href="https://www.familienwecker.de" style="color: #999; text-decoration: none;">${l.home}</a> &nbsp;|&nbsp;
        <a href="${privacyUrl}" style="color: #999; text-decoration: none;">${l.privacy}</a> &nbsp;|&nbsp;
        <a href="${imprintUrl}" style="color: #999; text-decoration: none;">${l.imprint}</a><br><br>
        ${t.footerNote}
      </p>
    </div>
  `;
}

exports.sendVerificationEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"],
    invoker: "public",
  },
  async (request) => {
    // Security: Nur eingeloggte User dürfen für ihre eigene E-Mail-Adresse eine Verifikations-Mail anfordern.
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const email = request.data?.email;
    // E-Mail muss mit der des eingeloggten Users übereinstimmen.
    if (email && request.auth.token.email && email.trim().toLowerCase() !== request.auth.token.email.toLowerCase()) {
      throw new HttpsError("permission-denied", "EMAIL_MISMATCH");
    }
    const lang = resolveLanguage(request.data?.language);

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim(), "verify");
      const linkOriginal = await admin.auth().generateEmailVerificationLink(email.trim());
      // Firebase uses a single global Action URL configured in the console (currently reset-password.html)
      // So we must string-replace it back to verify-email.html for verification links.
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");
      link = link.replace("reset-password.html", "verify-email.html");

      // Dynamically switch to localized HTML page
      if (lang !== "de") {
        link = link.replace("verify-email.html", `verify-email-${lang}.html`);
      }

      const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.en;
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER[lang] || SENDER.en,
        to: [email.trim()],
        subject: t.subject,
        html: buildVerifyEmailHtml(link, lang),
      });

      if (error) {
        console.error("Resend Error:", error);
        throw new HttpsError("internal", "Failed to send verification email via Resend.");
      }

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendVerificationEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

// ─── Scheduled Function: Bereinigung unbestätigter User ──────────────────────

exports.cleanupUnverifiedUsers = onSchedule(
  {
    schedule: "every day 04:00",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
    secrets: ["RESEND_API_KEY"],
  },
  async (event) => {
    const fortyEightHoursAgoMs = Date.now() - 48 * 60 * 60 * 1000;
    const staleUsers = [];

    console.log(`Running unverified users cleanup. Deleting users created before ${new Date(fortyEightHoursAgoMs).toISOString()}`);

    async function listAllUnverifiedUsers(nextPageToken) {
      const result = await admin.auth().listUsers(1000, nextPageToken);
      result.users.forEach((user) => {
        const createdAtMs = Date.parse(user.metadata.creationTime);
        if (!user.emailVerified && createdAtMs < fortyEightHoursAgoMs) {
          // Check if user has any other providers (e.g. Google) - if Google, email is usually verified automatically
          // but we check anyway to be safe. Password-only users are the main target.
          const isPasswordUser = user.providerData.some(p => p.providerId === 'password');
          if (isPasswordUser) {
            staleUsers.push(user.uid);
          }
        }
      });
      if (result.pageToken) {
        await listAllUnverifiedUsers(result.pageToken);
      }
    }

    await listAllUnverifiedUsers();

    if (staleUsers.length === 0) {
      console.log("No expired unverified users found.");
      return;
    }

    console.log(`Found ${staleUsers.length} users to delete. Starting batch deletion...`);

    // Delete in chunks of 1000 (limit of deleteUsers)
    for (let i = 0; i < staleUsers.length; i += 1000) {
      const chunk = staleUsers.slice(i, i + 1000);
      await admin.auth().deleteUsers(chunk);
    }

    console.log(`Successfully deleted ${staleUsers.length} unverified users.`);

    // Admin-Benachrichtigung senden
    const resendKey = process.env.RESEND_API_KEY;
    if (resendKey && staleUsers.length > 0) {
      try {
        const resend = new Resend(resendKey);
        await resend.emails.send({
          from: SENDER.de,
          to: [NOTIFY_EMAIL],
          subject: `🧹 User-Profile bereinigt: ${staleUsers.length} Accounts gelöscht`,
          html: `
            <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: ${BRAND_BLUE};">🧹 User-Bereinigung abgeschlossen</h2>
              <p>Der tägliche Cleanup-Job hat nicht verifizierte Benutzerkonten entfernt, die älter als 48 Stunden waren.</p>
              <p><strong>Anzahl gelöschter Accounts:</strong> ${staleUsers.length}</p>
              <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
              <p style="font-size: 11px; color: #999;">Projekt: Familienwecker App</p>
            </div>
          `,
        });
        console.log("Admin notification sent for user cleanup.");
      } catch (err) {
        console.error("Failed to send admin notification for user cleanup:", err);
      }
    }
  }
);

// ─── Scheduled Function: Bereinigung inaktiver Familien (Müll-Sammlung) ───────
exports.cleanupInactiveFamilies = onSchedule(
  {
    schedule: "every sunday 04:00",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
    secrets: ["RESEND_API_KEY"],
  },
  async (event) => {
    // 6 Monate (180 Tage) Inaktivität
    const sixMonthsAgoMs = Date.now() - 180 * 24 * 60 * 60 * 1000;
    const staleFamilies = [];

    console.log(`Running inactive families cleanup. Looking for activity before ${new Date(sixMonthsAgoMs).toISOString()}`);

    const familiesSnapshot = await admin.firestore().collection("families").get();

    for (const familyDoc of familiesSnapshot.docs) {
      // Höre auf das letzte Update eines Mitglieds
      const membersSnapshot = await familyDoc.ref.collection("members")
        .orderBy("lastUpdatedAt", "desc")
        .limit(1)
        .get();

      // Default: NICHT löschen – nur explizit als stale markieren wenn sicher veraltet
      let isStale = false;
      const familyData = familyDoc.data();

      // createdAt prüfen: Fallback Date.now() (= nie löschen) wenn Feld fehlt
      const createdAtMs = familyData.createdAt
        ? (typeof familyData.createdAt.toMillis === "function"
          ? familyData.createdAt.toMillis()
          : familyData.createdAt)
        : Date.now(); // Sicherer Fallback: kein Datum → als "jetzt" behandeln, nie löschen

      if (createdAtMs <= sixMonthsAgoMs) {
        // Familie selbst ist alt genug – prüfe ob Members aktiv waren
        if (membersSnapshot.empty) {
          // Keine Members UND Familie älter als 6 Monate → stale
          isStale = true;
        } else {
          const latestMember = membersSnapshot.docs[0].data();
          // Firestore Timestamps haben .toMillis() – direkter Vergleich mit > wäre nur bei Numbers korrekt
          // Fallback Date.now() wenn lastUpdatedAt fehlt → sicher, nicht löschen
          const lastUpdatedMs = latestMember.lastUpdatedAt
            ? (typeof latestMember.lastUpdatedAt.toMillis === "function"
              ? latestMember.lastUpdatedAt.toMillis()
              : latestMember.lastUpdatedAt)
            : Date.now();
          if (lastUpdatedMs <= sixMonthsAgoMs) {
            isStale = true;
          }
        }
      }

      if (isStale) {
        staleFamilies.push(familyDoc.ref);
      }
    }

    if (staleFamilies.length === 0) {
      console.log("No inactive families found.");
      return;
    }

    console.log(`Found ${staleFamilies.length} families to delete. Starting deletion...`);

    // In modern Admin SDKs, recursiveDelete deletes the document and all its subcollections
    if (admin.firestore().recursiveDelete) {
      for (const ref of staleFamilies) {
        await admin.firestore().recursiveDelete(ref);
      }
    } else {
      // Fallback
      for (const ref of staleFamilies) {
        const members = await ref.collection("members").get();
        const batch = admin.firestore().batch();
        members.docs.forEach(doc => batch.delete(doc.ref));
        batch.delete(ref);
        await batch.commit();
      }
    }

    console.log(`Successfully deleted ${staleFamilies.length} inactive families.`);

    // Admin-Benachrichtigung senden
    const resendKey = process.env.RESEND_API_KEY;
    if (resendKey && staleFamilies.length > 0) {
      try {
        const resend = new Resend(resendKey);
        await resend.emails.send({
          from: SENDER.de,
          to: [NOTIFY_EMAIL],
          subject: `🧹 Familien-Bereinigung: ${staleFamilies.length} inaktive Familien gelöscht`,
          html: `
            <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
              <h2 style="color: ${BRAND_BLUE};">🧹 Müll-Sammlung abgeschlossen</h2>
              <p>Der wöchentliche Cleanup-Job hat inaktive Familien entfernt, auf die seit mehr als 6 Monaten (180 Tagen) nicht mehr zugegriffen wurde.</p>
              <p><strong>Anzahl gelöschter Familien:</strong> ${staleFamilies.length}</p>
              <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
              <p style="font-size: 11px; color: #999;">Projekt: Familienwecker App</p>
            </div>
          `,
        });
        console.log("Admin notification sent for family cleanup.");
      } catch (err) {
        console.error("Failed to send admin notification for family cleanup:", err);
      }
    }
  }
);

// ─── Sicherer Join-Flow via Cloud Function ────────────────────────────────────
// Verhindert direkten Firestore-Zugriff auf alle Familien und ermöglicht
// serverseitiges Rate-Limiting gegen Brute-Force-Versuche auf Join-Codes.
exports.joinFamilyByCode = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const rawCode = request.data?.code;
    if (!rawCode || typeof rawCode !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }
    const code = rawCode.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 6);
    if (code.length !== 6) {
      throw new HttpsError("invalid-argument", "INVALID_CODE");
    }

    // Rate-Limiting: max. 5 Join-Versuche pro UID pro Minute, max. 10 pro Tag
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();

      if (!isAdmin) {
        const minuteLimited = await checkSingleRateLimit(`join_${uid}_m`, 60 * 1000, 5);
        if (minuteLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`join_${uid}_d`, 24 * 60 * 60 * 1000, 10);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing join rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden
        if (!adminDoc.exists && uid === primaryAdminUidSecret.value()) {
          await admin.firestore().collection("_admins").doc(uid).set({
            email: request.auth.token.email || "",
            promotedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    const snapshot = await admin.firestore()
      .collection("families")
      .where("joinCode", "==", code)
      .limit(1)
      .get();

    if (snapshot.empty) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }

    const familyId = snapshot.docs[0].id;
    // Security Fix: Write familyId to users collection server-side
    await admin.firestore().collection("users").doc(uid).set({ familyId }, { merge: true });
    // Keep userIds array in sync for Firestore Security Rules (read permission)
    await admin.firestore().collection("families").doc(familyId).update({
      userIds: admin.firestore.FieldValue.arrayUnion(uid)
    });

    // Feature #4: Bestehende Members über neuen Beitritt informieren (fire-and-forget)
    const displayName = request.auth.token.name || request.auth.token.email?.split("@")[0] || "Someone";
    notifyFamilyMemberJoined(familyId, displayName, uid).catch(err =>
      console.warn("notifyFamilyMemberJoined failed (non-critical):", err?.message)
    );

    return { familyId, joinCode: code };
  }
);

// ─── Sicheres Familie-Erstellen via Cloud Function ────────────────────────────
// Generiert den joinCode serverseitig und schreibt das Familie-Dokument.
// Verhindert client-seitigen Query auf die families-Collection für Eindeutigkeitsprüfung.
exports.createFamily = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const familyName = request.data?.familyName;
    if (!familyName || typeof familyName !== "string" || familyName.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_NAME");
    }
    const sanitizedName = familyName.trim().slice(0, 64);

    // Rate-Limiting: max. 3 Family-Erstellungen pro UID pro Stunde, max. 6 pro Tag
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();

      if (!isAdmin) {
        const hourLimited = await checkSingleRateLimit(`create_${uid}_h`, 60 * 60 * 1000, 3);
        if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`create_${uid}_d`, 24 * 60 * 60 * 1000, 6);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing create rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden.
        // E-Mail-Prüfung entfällt – die äußere Bedingung (PRIMARY_ADMIN_UID) ist ausreichend.
        if (!adminDoc.exists && uid === primaryAdminUidSecret.value()) {
          await admin.firestore().collection("_admins").doc(uid).set({
            email: request.auth.token.email || "",
            promotedAt: admin.firestore.FieldValue.serverTimestamp()
          });
        }
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Rate-Limit-Check fehlgeschlagen (wird ignoriert):", err);
    }

    // Eindeutigen 6-stelligen alphanumerischen Join-Code generieren (max. 5 Versuche)
    const CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // ohne 0/O und 1/I zur Verwechslungsvermeidung
    let joinCode = null;
    let attempts = 0;

    while (attempts < 5) {
      const candidate = Array.from({ length: 6 }, () =>
        CHARS.charAt(randomInt(CHARS.length))
      ).join("");

      try {
        const existing = await admin.firestore()
          .collection("families")
          .where("joinCode", "==", candidate)
          .limit(1)
          .get();

        if (existing.empty) {
          joinCode = candidate;
          break;
        }
      } catch (queryErr) {
        console.error(`Code uniqueness check failed (attempt ${attempts}):`, queryErr.message);
        // Treat as if code is taken → try again
      }
      attempts++;
    }

    if (!joinCode) {
      throw new HttpsError("internal", "CODE_GENERATION_FAILED");
    }

    // Familie-Dokument anlegen
    const familyData = {
      name: sanitizedName,
      joinCode,
      createdByUserId: uid,
      userIds: [uid],
      isAlarmEnabled: true,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const docRef = await admin.firestore().collection("families").add(familyData);
    const familyId = docRef.id;

    // Security Fix: Write familyId to users collection server-side
    await admin.firestore().collection("users").doc(uid).set({ familyId }, { merge: true });

    // Security: joinCode nicht loggen – ist ein Zugangsdaten-Äquivalent.
    console.log(`Family '${sanitizedName}' created by ${uid} with id ${familyId}`);

    return { familyId, joinCode };
  }
);

// ─── Familie verlassen ───────────────────────────────────────────────────────
exports.leaveFamily = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const { familyId, memberId } = request.data || {};
    if (!familyId || typeof familyId !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_ID");
    }

    const userDocRef = admin.firestore().collection("users").doc(uid);
    const userDoc = await userDocRef.get();

    if (!userDoc.exists || userDoc.data().familyId !== familyId) {
      throw new HttpsError("failed-precondition", "NOT_A_MEMBER_OF_THIS_FAMILY");
    }

    const familyDocRef = admin.firestore().collection("families").doc(familyId);
    const familyDoc = await familyDocRef.get();

    if (!familyDoc.exists) {
      // Family might have been deleted by another user
      await userDocRef.update({ familyId: admin.firestore.FieldValue.delete() });
      console.log(`User ${uid} left non-existent family ${familyId}.`);
      return { success: true };
    }

    const familyData = familyDoc.data();

    // Check if user is the last member
    const membersSnapshot = await familyDocRef.collection("members").get();
    const memberCount = membersSnapshot.size;

    if (memberCount === 1) {
      // Logic changed: Do NOT automatically delete families when the last member leaves,
      // to keep the joinCode valid for future members. Empty families will be cleaned up later.
      console.log(`User ${uid} is the last member of family ${familyId}. Keeping family document.`);
    }

    // If user is the creator and there are other members, reassign creator
    if (familyData.createdByUserId === uid) {
      const otherMembers = membersSnapshot.docs.filter(doc => doc.id !== (memberId || uid));
      if (otherMembers.length > 0) {
        // Use claimedByUserId (real Auth UID), NOT doc.id (which is the member doc ID, not a user UID)
        const newCreatorDoc = otherMembers.find(doc => doc.data().claimedByUserId);
        const newCreatorUid = newCreatorDoc ? newCreatorDoc.data().claimedByUserId : null;
        if (newCreatorUid) {
          await familyDocRef.update({ createdByUserId: newCreatorUid });
          console.log(`Reassigned creator of family ${familyId} from ${uid} to ${newCreatorUid}.`);
        } else {
          console.log(`No claimed member found to reassign creator of family ${familyId}. Field stays stale.`);
        }
      }
    }

    // Mit memberId vom Client: prüfe dass das Member-Dokument dem User gehört
    // (Schutz: Familienmitglied darf nicht fremde Member-Profile löschen)
    const finalMemberId = memberId || uid;
    if (memberId) {
      const memberDocRef = familyDocRef.collection("members").doc(memberId);
      const memberDoc = await memberDocRef.get();
      if (memberDoc.exists && memberDoc.data().claimedByUserId !== uid) {
        throw new HttpsError("permission-denied", "CANNOT_DELETE_OTHER_MEMBER");
      }
    }
    await familyDocRef.collection("members").doc(finalMemberId).delete();

    // Remove familyId from user's document
    await userDocRef.update({ familyId: admin.firestore.FieldValue.delete() });

    // Remove uid from family's userIds array (Firestore Security Rules read access)
    await familyDocRef.update({
      userIds: admin.firestore.FieldValue.arrayRemove(uid)
    });

    console.log(`User ${uid} (Member: ${finalMemberId}) successfully left family ${familyId}.`);

    // Feature #4: Verbleibende Members über Austritt informieren (fire-and-forget)
    // userIds aus dem bereits geladenen familyDoc (vor arrayRemove) – kein extra Read
    const existingUserIds = familyData.userIds || [];
    notifyFamilyMemberLeft(existingUserIds, uid).catch(err =>
      console.warn("notifyFamilyMemberLeft failed (non-critical):", err?.message)
    );

    return { success: true, familyDeleted: false };
  }
);

// ─── Familie löschen (Ersteller oder Global Admin) ───────────────────────────
exports.deleteFamily = onCall(
  { region: "europe-west3", secrets: ["PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    const { familyId } = request.data || {};
    if (!familyId || typeof familyId !== "string") {
      throw new HttpsError("invalid-argument", "INVALID_FAMILY_ID");
    }

    const familyDocRef = admin.firestore().collection("families").doc(familyId);
    const familyDoc = await familyDocRef.get();

    if (!familyDoc.exists) {
      throw new HttpsError("not-found", "FAMILY_NOT_FOUND");
    }

    const familyData = familyDoc.data();

    // Global Admin Check
    const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
    const isGlobalAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();

    // Only the creator OR global admin can delete the family
    if (!isGlobalAdmin && familyData.createdByUserId !== uid) {
      throw new HttpsError("permission-denied", "ONLY_CREATOR_OR_ADMIN_CAN_DELETE_FAMILY");
    }

    // Delete all member documents and remove familyId from their user profiles
    const membersSnapshot = await familyDocRef.collection("members").get();
    const batch = admin.firestore().batch();

    for (const memberDoc of membersSnapshot.docs) {
      const data = memberDoc.data();
      batch.delete(memberDoc.ref); // Delete member document

      const claimedUid = data.claimedByUserId;
      if (claimedUid) {
        // Remove familyId from user's document using claimed UID
        // Use set({familyId: delete}, {merge: true}) instead of update to avoid errors if doc doesn't exist
        batch.set(admin.firestore().collection("users").doc(claimedUid), {
          familyId: admin.firestore.FieldValue.delete()
        }, { merge: true });
      }
    }

    // Recursively delete the family document and its subcollections
    // This is the robust way to ensure everything inside matches/members is gone
    await admin.firestore().recursiveDelete(familyDocRef);
    await batch.commit(); // Commit batch for user profile updates

    console.log(`Family ${familyId} and all its members deleted by ${isGlobalAdmin ? "admin" : "creator"} ${uid}.`);
    return { success: true };
  }
);

/**
 * Täglicher Reset-Job für Familienmitglieder (alle 1h).
 * Setzt "isAwakeToday" und "isPaused" (nur bei ungeclaimten) zurück.
 * Schwellenwert: latestWakeUp + 2 Stunden.
 */
exports.scheduledMemberReset = onSchedule(
  {
    schedule: "every 1 hours",
    region: "europe-west3",
    timeZone: "Europe/Berlin",
  },
  async (event) => {
    const familiesSnapshot = await admin.firestore().collection("families").get();
    const now = new Date();

    // Berlin Zeit für den Vergleich (YYYY-MM-DD und HH:mm)
    const options = { timeZone: "Europe/Berlin", hour12: false };
    const todayStr = now.toLocaleDateString("en-CA", options); // YYYY-MM-DD
    const currentTimeStr = now.toLocaleTimeString("en-GB", options).slice(0, 5); // HH:mm

    console.log(`Running scheduled reset check at ${currentTimeStr} (${todayStr}).`);

    for (const familyDoc of familiesSnapshot.docs) {
      const membersRef = familyDoc.ref.collection("members");
      const membersSnapshot = await membersRef.get();
      const batch = admin.firestore().batch();
      let hasUpdates = false;

      membersSnapshot.forEach((memberDoc) => {
        const member = memberDoc.data();
        const latestWakeUp = member.latestWakeUp; // "HH:mm"

        if (!latestWakeUp) return;

        // Schwellenwert berechnen (latestWakeUp + 2h)
        const [hours, minutes] = latestWakeUp.split(":").map(Number);
        const resetDate = new Date();
        resetDate.setHours(hours + 2, minutes, 0, 0);
        const resetTimeStr = resetDate.toLocaleTimeString("en-GB", options).slice(0, 5);

        // Reset nur wenn:
        // 1. Aktuelle Zeit > (latestWakeUp + 2h)
        // 2. lastResetDate != today (sichert dass 1x pro Tag resettet wird)
        const isPastResetThreshold = currentTimeStr >= resetTimeStr;
        const needsReset = isPastResetThreshold && member.lastResetDate !== todayStr;

        if (needsReset) {
          const isUnclaimed = !member.claimedByUserId;
          const updates = {
            isAwakeToday: false,
            lastResetDate: todayStr,
            lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
          };

          if (isUnclaimed) {
            updates.isPaused = false;
          }

          batch.update(memberDoc.ref, updates);
          hasUpdates = true;
        }
      });

      if (hasUpdates) {
        await batch.commit();
        console.log(`Reset performed for members in family ${familyDoc.id}`);
      }
    }
    console.log("Scheduled member reset completed.");
  }
);

// ─── Feedback-E-Mail via Resend ──────────────────────────────────────────────
exports.sendFeedbackEmail = onCall(
  {
    region: "europe-west3",
    secrets: ["RESEND_API_KEY", "PRIMARY_ADMIN_UID"],
    invoker: "public",
  },
  async (request) => {
    // Nur eingeloggte Nutzer dürfen Feedback senden
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    // S8: Rate-Limiting – max. 3 Feedbacks/Stunde, max. 10/Tag pro UID
    try {
      const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
      const isAdmin = adminDoc.exists || uid === primaryAdminUidSecret.value();
      if (!isAdmin) {
        const hourLimited = await checkSingleRateLimit(`feedback_${uid}_h`, 60 * 60 * 1000, 3);
        if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`feedback_${uid}_d`, 24 * 60 * 60 * 1000, 10);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      }
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Feedback rate-limit check failed (ignored):", err);
    }

    const { category, message, email, appVersion, device } = request.data || {};

    if (!message || typeof message !== "string" || message.trim().length === 0) {
      throw new HttpsError("invalid-argument", "INVALID_MESSAGE");
    }

    // S8: E-Mail-Format serverseitig validieren (replyTo ist optional, muss aber valide sein)
    const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (email && email.trim() && !EMAIL_REGEX.test(email.trim())) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      throw new HttpsError("failed-precondition", "Email service not configured.");
    }


    const sanitizedCategory = escapeHtml((category || "Sonstiges").slice(0, 100));
    const sanitizedMessage = escapeHtml(message?.trim().slice(0, 5000) || "");
    const sanitizedDevice = escapeHtml((device || "").slice(0, 200));
    const sanitizedAppVersion = escapeHtml((appVersion || "").slice(0, 50));

    const replyTo = email && email.trim() ? email.trim().slice(0, 254) : undefined;
    const subject = `📬 FamWake Feedback: ${sanitizedCategory}`;
    const html = `
      <div style="font-family: sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
        <h2 style="color: ${BRAND_BLUE};">📬 Neues Feedback</h2>
        <table style="width:100%; border-collapse: collapse; font-size:14px;">
          <tr><td style="padding:6px 0; color:#666; width:130px;">Kategorie</td><td><strong>${sanitizedCategory}</strong></td></tr>
          <tr><td style="padding:6px 0; color:#666;">App-Version</td><td>${sanitizedAppVersion || "–"}</td></tr>
          <tr><td style="padding:6px 0; color:#666;">Gerät</td><td>${sanitizedDevice || "–"}</td></tr>
          ${replyTo ? `<tr><td style="padding:6px 0; color:#666;">Antwort-E-Mail</td><td><a href="mailto:${replyTo}">${replyTo}</a></td></tr>` : ""}
        </table>
        <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
        <h3 style="color: ${BRAND_BLUE};">Nachricht</h3>
        <p style="background:#f9f9f9; padding:12px; border-radius:6px; white-space: pre-wrap;">${sanitizedMessage}</p>
        <hr style="border: none; border-top: 1px solid #eee; margin: 16px 0;">
        <p style="font-size:11px; color:#999; text-align:center;">FamWake Familienwecker – automatisch generiert</p>
      </div>
    `;

    try {
      const resend = new Resend(resendKey);
      const { error } = await resend.emails.send({
        from: SENDER.de,
        to: [NOTIFY_EMAIL],
        subject,
        html,
        ...(replyTo ? { reply_to: replyTo } : {}),
      });

      if (error) {
        console.error("Resend Feedback Error:", error);
        throw new HttpsError("internal", "Failed to send feedback email.");
      }

      // Feedback auch in Firestore für Archiv speichern
      await admin.firestore().collection("feedback").add({
        category: category || "",
        message: message.trim(),
        email: email?.trim() || "",
        appVersion: appVersion || "",
        device: device || "",
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      return { success: true };

    } catch (err) {
      if (err instanceof HttpsError) throw err;
      console.error("Error in sendFeedbackEmail:", err);
      throw new HttpsError("internal", err.message || "INTERNAL_ERROR");
    }
  }
);

// ─── Admin-Reports ──────────────────────────────────────────────────────────

/** Hilfsfunktion zur Generierung des Statistik-Reports */
async function getStatsReport() {
  const usersResponse = await admin.auth().listUsers();
  const users = usersResponse.users;

  const familiesSnapshot = await admin.firestore().collection("families").get();
  let familiesHtml = "";

  for (const doc of familiesSnapshot.docs) {
    const family = doc.data();
    const membersSnapshot = await doc.ref.collection("members").get();
    // escapeHtml: Member-Namen könnten HTML-Sonderzeichen enthalten (XSS in Admin-Mail)
    const members = membersSnapshot.docs.map(m => escapeHtml(m.data().name || "Unbekannt")).join(", ");

    familiesHtml += `
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;"><b>${escapeHtml(family.name || family.familyName || "Unbenannt")}</b></td>
                <td style="padding: 8px; border-bottom: 1px solid #eee; font-size: 0.9em; color: #666;">${members}</td>
            </tr>
        `;
  }

  return `
        <div style="font-family: sans-serif; max-width: 600px; color: #333;">
            <h2 style="color: ${BRAND_BLUE};">FamWake Admin Report</h2>
            <p>Stand: ${new Date().toLocaleString("de-DE", { timeZone: "Europe/Berlin" })}</p>
            
            <h3 style="border-bottom: 2px solid ${BRAND_BLUE}; padding-bottom: 4px;">Zusammenfassung</h3>
            <ul>
                <li>Gesamt-Nutzer: <b>${users.length}</b></li>
                <li>Gesamt-Familien: <b>${familiesSnapshot.size}</b></li>
            </ul>

            <h3 style="border-bottom: 2px solid ${BRAND_BLUE}; padding-bottom: 4px;">Familien & Mitglieder</h3>
            <table style="width: 100%; border-collapse: collapse;">
                <thead>
                    <tr style="background: #f8f8f8;">
                        <th style="padding: 8px; text-align: left; border-bottom: 2px solid #ddd;">Name</th>
                        <th style="padding: 8px; text-align: left; border-bottom: 2px solid #ddd;">Mitglieder</th>
                    </tr>
                </thead>
                <tbody>${familiesHtml}</tbody>
            </table>
            
            <hr style="margin-top: 24px; border: 0; border-top: 1px solid #ccc;">
            <p style="font-size: 0.8em; color: #999;">Dieser Report wurde automatisch generiert (anonymisiert für DSGVO).</p>
        </div>
    `;
}

/** Manueller Report-Abruf aus der App */
exports.sendAdminStatsReport = onCall(
  { region: "europe-west3", secrets: ["RESEND_API_KEY", "PRIMARY_ADMIN_UID"] },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");

    // Admin check
    const uid = request.auth.uid;
    const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
    if (!adminDoc.exists && uid !== primaryAdminUidSecret.value()) {
      throw new HttpsError("permission-denied", "ADMIN_ONLY");
    }

    const html = await getStatsReport();
    await sendEmail(NOTIFY_EMAIL, "FamWake: Manueller Admin-Report", html);
    return { success: true };
  }
);

/** Wöchentlicher automatischer Report */
exports.scheduledAdminStatsReport = onSchedule(
  {
    schedule: "every sunday 20:00",
    timeZone: "Europe/Berlin",
    region: "europe-west3",
    secrets: ["RESEND_API_KEY"]
  },
  async (event) => {
    const html = await getStatsReport();
    await sendEmail(NOTIFY_EMAIL, "FamWake: Wöchentlicher Admin-Report", html);
    console.log("Weekly admin stats report sent.");
  }
);

/** Hilfsfunktion zum Mail-Versand via Resend (mit Secret-Handling) */
async function sendEmail(to, subject, html) {
  const resendKey = process.env.RESEND_API_KEY;
  if (!resendKey) {
    console.error("RESEND_API_KEY secret is not set.");
    return;
  }
  const resend = new Resend(resendKey);
  const { error } = await resend.emails.send({
    from: SENDER.de,
    to: [to],
    subject: subject,
    html: html,
  });
  if (error) {
    console.error(`Fehler beim Senden der E-Mail an ${to}:`, error);
  }
}

// ─── Startup-Kontext: Familie des Users in einem Batch-Read ─────────────────
// Ersetzt den 3-fachen sequenziellen Firestore-Read im Client.
// Gibt { familyId, joinCode } zurück oder null wenn kein Familien-Kontext gefunden.
exports.getUserContext = onCall(
  { region: "europe-west3" },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    }
    const uid = request.auth.uid;

    try {
      // Schritt 1: users/{uid} lesen
      const userDoc = await admin.firestore().collection("users").doc(uid).get();
      const familyId = userDoc.exists ? userDoc.data().familyId : null;

      if (!familyId) {
        return { familyId: null, joinCode: null };
      }

      // Schritt 2: families/{familyId} lesen (Batch mit users/{uid} wäre getAll, aber
      // da familyId erst aus Step 1 kommt, ist sequenziell hier unvermeidbar.
      // Gegenüber Client-Pfad: kein 3. Query-Fallback, kein Netz-Overhead pro Read.)
      const familyDoc = await admin.firestore().collection("families").doc(familyId).get();

      if (!familyDoc.exists) {
        // Familie gelöscht – users/{uid}.familyId bereinigen
        await admin.firestore().collection("users").doc(uid).update({
          familyId: admin.firestore.FieldValue.delete()
        });
        console.log(`getUserContext: Family ${familyId} not found, cleaned up user doc for ${uid}`);
        return { familyId: null, joinCode: null };
      }

      const joinCode = familyDoc.data().joinCode || null;
      return { familyId, joinCode };

    } catch (err) {
      console.error(`getUserContext error for ${uid}:`, err);
      throw new HttpsError("internal", "INTERNAL_ERROR");
    }
  }
);

/**
 * Verifiziert ein Play Integrity Token serverseitig via Google API.
 * Client-seitiges Base64-Decoding liefert immer UNKNOWN (JWT ist signiert und
 * kann client-seitig nicht zuverlässig verifiziert werden). Diese Function ruft
 * die offizielle Google Play Integrity API auf und gibt das echte Verdict zurück.
 */
exports.verifyIntegrityToken = onCall(
  {
    region: "europe-west3",
    invoker: "public",
  },
  async (request) => {
    const token = request.data?.token;
    if (!token || typeof token !== "string") {
      throw new HttpsError("invalid-argument", "Token required");
    }

    try {
      const { GoogleAuth } = require("google-auth-library");
      const auth = new GoogleAuth({
        scopes: ["https://www.googleapis.com/auth/playintegrity"],
      });
      const client = await auth.getClient();

      const packageName = "de.familienwecker.famwake";
      const url = `https://playintegrity.googleapis.com/v1/${packageName}:decodeIntegrityToken`;

      const response = await client.request({
        url,
        method: "POST",
        data: { integrity_token: token },
      });

      const payload = response.data?.tokenPayloadExternal;
      const verdictArray = payload?.deviceIntegrity?.deviceRecognitionVerdict ?? [];
      const trusted = Array.isArray(verdictArray) && verdictArray.includes("MEETS_DEVICE_INTEGRITY");

      console.log(`Play Integrity verdict: ${JSON.stringify(verdictArray)}, trusted=${trusted}`);
      return { trusted, verdict: verdictArray };

    } catch (err) {
      // Fail-open: bei API-Fehler UNKNOWN zurückgeben, nicht blocken
      console.error("verifyIntegrityToken error:", err?.message ?? err);
      return { trusted: null, verdict: [], error: "API_ERROR" };
    }
  }
);

// ═══════════════════════════════════════════════════════════════════════════
// v1.8.0 – Push Notifications Free Tier
// Features: #2 (Reihenfolge geändert), #4 (Familien-Events)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Liest alle FCM-Tokens eines Users und sendet eine Multicast-Nachricht.
 * Entfernt automatisch abgelaufene Tokens (HTTP 404 von FCM → Token ungültig).
 */
async function sendPushToUser(uid, payload) {
  const tokensSnap = await admin.firestore()
    .collection("users").doc(uid)
    .collection("fcmTokens")
    .get();

  if (tokensSnap.empty) return;

  // Token ist als Feld gespeichert (Dokument-ID ist SHA-256-Hash, da FCM-Token ':' enthält)
  const tokenDocs = tokensSnap.docs
    .map(d => ({ token: d.data().token, docId: d.id }))
    .filter(t => !!t.token);
  const tokens = tokenDocs.map(t => t.token);

  if (tokens.length === 0) return;

  const message = {
    tokens,
    data: {
      type: payload.type || "info",
      title: payload.title || "",
      body: payload.body || "",
    },
    // Stille Datennachricht: App zeigt Notification selbst an (voller Channel-Kontrolle)
    android: { priority: "high" },
  };

  const response = await admin.messaging().sendEachForMulticast(message);

  // Abgelaufene Tokens direkt löschen – reduziert Firestore-Reads beim nächsten Push
  const deleteOps = [];
  response.responses.forEach((res, idx) => {
    if (!res.success) {
      const code = res.error?.code || "";
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        deleteOps.push(
          admin.firestore()
            .collection("users").doc(uid)
            .collection("fcmTokens").doc(tokenDocs[idx].docId)
            .delete()
        );
      }
    }
  });
  if (deleteOps.length > 0) await Promise.all(deleteOps);
}

async function sendPushToUsers(uids, payload) {
  await Promise.all(uids.map(uid => sendPushToUser(uid, payload)));
}

// ─── Feature #2: Reihenfolge / Weckzeit geändert ─────────────────────────────
exports.onMemberScheduleChanged = onDocumentWritten(
  {
    document: "families/{familyId}/members/{memberId}",
    region: "europe-west3",
  },
  async (event) => {
    const before = event.data.before?.data();
    const after = event.data.after?.data();

    // Nur bei echten Updates reagieren (kein Create/Delete)
    if (!before || !after) return;

    // Änderungstyp bestimmen: Schedule-Felder vs. Status-Felder
    const scheduleFields = ["earliestWakeUp", "latestWakeUp", "sequenceOrder", "dayProfiles"];
    const scheduleChanged = scheduleFields.some(f => JSON.stringify(before[f]) !== JSON.stringify(after[f]));
    const statusChanged = before.isPaused !== after.isPaused ||
      before.deviceAlarmEnabled !== after.deviceAlarmEnabled;

    if (!scheduleChanged && !statusChanged) return;

    // Ungeclaimte Member bei reinem Reorder skippen: der Push wird durch einen
    // geclaimten Member-Trigger desselben Batch-Writes abgedeckt.
    // Status-Änderungen (z.B. Pause) auf ungeclaimten Membern verarbeiten wir weiterhin.
    if (!after.claimedByUserId && scheduleChanged && !statusChanged) return;

    const familyId = event.params.familyId;

    // Rate-Limit: nur 1 Push pro Familie alle 5 Sekunden.
    // Fängt Batch-Write-Duplikate ab (2 geclaimte Triggers kommen gleichzeitig).
    // Ungeclaimte Member werden bereits vorher gefiltert.
    const lockRef = admin.firestore().collection("_pushLocks").doc(familyId);
    const now = Date.now();
    let shouldSend = false;
    await admin.firestore().runTransaction(async t => {
      const lockSnap = await t.get(lockRef);
      if (!lockSnap.exists || (now - (lockSnap.data().lastPush || 0)) >= 5000) {
        t.set(lockRef, { lastPush: now });
        shouldSend = true;
      }
    });
    if (!shouldSend) return;

    // Alle Family-Members laden
    const membersSnap = await admin.firestore()
      .collection("families").doc(familyId)
      .collection("members")
      .get();

    // Sender-Erkennung: wer hat die Änderung ausgelöst? → kein Self-Push
    let changedBy = null;
    if (statusChanged && !scheduleChanged) {
      // Bei reinen Status-Änderungen (Pause, Alarm-Toggle) ist der Auslöser der Member-Owner
      changedBy = after.claimedByUserId || null;
    } else {
      // Bei Schedule-Änderungen (Reorder, Zeiten) wird der Auslöser über pushMeta erkannt
      const REORDER_WINDOW_MS = 15000;
      for (const doc of membersSnap.docs) {
        const uid = doc.data().claimedByUserId;
        if (!uid) continue;
        const metaSnap = await admin.firestore()
          .collection("users").doc(uid)
          .collection("pushMeta").doc("reorder")
          .get();
        const meta = metaSnap.data();
        if (meta?.familyId === familyId &&
          now - (meta?.timestamp?.toMillis?.() || 0) < REORDER_WINDOW_MS) {
          changedBy = uid;
          break;
        }
      }
    }

    // Empfängerliste: ALLE geclaimten Members außer dem Auslöser.
    // Kein Positions-Filter: der Schedule wird rückwärts berechnet, daher
    // betrifft JEDE Änderung (Reorder, Pause, Alarm-Toggle, Zeiten) alle.
    // Familienmitglieder sollen auch bei nicht-eigener Betroffenheit informiert werden,
    // damit sie ggf. eingreifen und den Plan anpassen können.
    const recipientUids = [];
    for (const doc of membersSnap.docs) {
      const d = doc.data();
      const uid = d.claimedByUserId;
      if (!uid || uid === changedBy) continue;
      recipientUids.push(uid);
    }

    if (recipientUids.length === 0) return;

    await sendPushToUsers(recipientUids, {
      type: "schedule_change",
    });
  }
);

// ─── Feature #4: Hilfsfunktionen für Join/Leave-Notifications ─────────────────
async function notifyFamilyMemberJoined(familyId, newMemberName, newUid) {
  const membersSnap = await admin.firestore()
    .collection("families").doc(familyId)
    .collection("members")
    .get();

  const recipientUids = [];
  for (const doc of membersSnap.docs) {
    const uid = doc.data().claimedByUserId;
    if (!uid || uid === newUid) continue;
    recipientUids.push(uid);
  }
  if (recipientUids.length === 0) return;

  await sendPushToUsers(recipientUids, {
    type: "family_joined",
    // Android lokalisiert self via type
  });
}

// leftUids: bereits bekannte Liste der verbleibenden UIDs (wird von Caller übergeben)
// Spart einen extra Firestore-Read nach dem arrayRemove.
async function notifyFamilyMemberLeft(recipientUids, leftUid) {
  const filtered = recipientUids.filter(uid => uid !== leftUid);
  if (filtered.length === 0) return;

  await sendPushToUsers(filtered, {
    type: "family_left",
    // Android lokalisiert self via type
  });
}