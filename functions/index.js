const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
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
 * Dual Rate-Limit auf E-Mail-Adresse:
 * max. 5 Versuche pro Stunde UND max. 10 pro Tag.
 */
async function checkEmailRateLimit(email) {
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

  const key = `email_${email.toLowerCase().replace(/[^a-z0-9]/g, "_").slice(0, 80)}`;

  // Stunden-Limit
  const hourLimited = await checkSingleRateLimit(`${key}_h`, 60 * 60 * 1000, 5);
  if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");

  // Tages-Limit (2× stündliches Limit)
  const dayLimited = await checkSingleRateLimit(`${key}_d`, 24 * 60 * 60 * 1000, 10);
  if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
}

const NOTIFY_EMAIL = "daniel.notthoff@gmail.com";
const PRIMARY_ADMIN_UID = "yqmtXyDNQCa5ajCvL9LEWbVgJmF2";
const BRAND_BLUE = "#1A3A5C";

// Dialekte → Muttersprache für E-Mail-Inhalte (formal/rechtlich → Hochdeutsch)
const DIALECT_TO_LANG = { gsw: "de", swg: "de", ksh: "de" };

const SENDER = {
  de: "FamWake Familienwecker <no-reply@familienwecker.de>",
  en: "FamWake Family Alarm <no-reply@familienwecker.de>",
  es: "FamWake Familienwecker <no-reply@familienwecker.de>",
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
    subject: "🔑 Restablecer contraseña – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "¡Hola!",
    intro: "Hemos recibido una solicitud para restablecer la contraseña de su cuenta de <strong>FamWake</strong> Familienwecker.",
    instruction: "Haga clic en el botón de abajo para establecer una nueva contraseña:",
    button: "Restablecer contraseña",
    fallback: "Si el botón no funciona, copie este enlace en su navegador:",
    security: "⚠️ Si no ha solicitado esto, puede ignorar este correo de forma segura. Su contraseña no cambiará. Si nota alguna actividad sospechosa, póngase en contacto con nosotros en: daniel.notthoff@gmail.com",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no responda directamente a este correo.",
  },
  fr: {
    subject: "🔑 Réinitialiser votre mot de passe – FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bonjour !",
    intro: "Nous avons reçu une demande de réinitialisation du mot de passe de votre compte <strong>FamWake</strong> Réveil Familial.",
    instruction: "Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe :",
    button: "Réinitialiser le mot de passe",
    fallback: "Si le bouton ne fonctionne pas, veuillez copier ce lien dans votre navigateur :",
    security: "⚠️ Si vous n'en êtes pas l'auteur, vous pouvez ignorer cet e-mail. Votre mot de passe restera inchangé. Si vous remarquez une activité suspecte, contactez-nous à l'adresse : daniel.notthoff@gmail.com",
    footerNote: "Ceci est un message généré automatiquement. Veuillez ne pas répondre directement à cet e-mail.",
  },
  it: {
    subject: "🔑 Reimposta la tua password – FamWake Sveglia Famiglia",
    appName: "<strong>FamWake</strong> Sveglia Famiglia",
    greeting: "Ciao!",
    intro: "Abbiamo ricevuto una richiesta di reimpostazione della password per il tuo account <strong>FamWake</strong> Sveglia Famiglia.",
    instruction: "Clicca sul pulsante qui sotto per impostare una nuova password:",
    button: "Reimposta password",
    fallback: "Se il pulsante non funziona, copia questo link nel tuo browser:",
    security: "⚠️ Se non hai richiesto tu questa modifica, puoi ignorare tranquillamente questa email. La tua password rimarrà invariata. Se noti attività sospette, contattaci all'indirizzo: daniel.notthoff@gmail.com",
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
    security: "⚠️ Hvis du <strong>ikke</strong> har anmodet om dette, kan du blot ignorere denne e-mail. Din adgangskode forbliver uændret. Hvis du bemærker mistanke om aktivitet, kontakt os på: daniel.notthoff@gmail.com",
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
    security: "⚠️ Als je dit verzoek <strong>niet</strong> hebt gedaan, kun je deze e-mail gewoon negeren. Je wachtwoord blijft ongewijzigd. Als je verdáchte activiteit opmerkt, neem dan contact met ons op: daniel.notthoff@gmail.com",
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
    security: "⚠️ Jeśli <strong>nie</strong> wysyłałeś tej prośby, możesz zignorować ten e-mail. Twoje hasło pozostanie bez zmian. Jeśli zauważysz podrzaną aktywność, skontaktuj się z nami: daniel.notthoff@gmail.com",
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
    intro: "Мы получили запрос на сброс пароля для вашей учётной записи <strong>FamWake</strong> Семейный Будильник.",
    instruction: "Нажмите на кнопку ниже, чтобы установить новый пароль:",
    button: "Сбросить пароль",
    fallback: "Если кнопка не работает, скопируйте эту ссылку в браузер:",
    security: "⚠️ Если вы <strong>не</strong> запрашивали это, просто проигнорируйте это письмо. Ваш пароль останется без изменений. Если вы заметили подозрительную активность, свяжитесь с нами: daniel.notthoff@gmail.com",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечайте на него напрямую.",
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
    subject: "🔑 Şifrenizi sıfırlayın – FamWake Aile Alarmı",
    appName: "<strong>FamWake</strong> Aile Alarmı",
    greeting: "Merhaba!",
    intro: "<strong>FamWake</strong> Aile Alarmı hesabınızın şifresini sıfırlama talebi aldık.",
    instruction: "Yeni bir şifre belirlemek için aşağıdaki düğmeye tıklayın:",
    button: "Şifreyi sıfırla",
    fallback: "Düğme çalışmıyorsa, bu bağlantıyı tarayıcınıza kopyalayın:",
    security: "⚠️ Bu talebi siz <strong>yapmadıysanız</strong>, bu e-postayı görmezden gelebilirsiniz. Şifreniz değişmeden kalacak. Şüpheli bir etkinlik fark ederseniz bizimle iletişime geçin: daniel.notthoff@gmail.com",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt vermeyin.",
  },
  uk: {
    subject: "🔑 Скидання пароля – FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Привіт!",
    intro: "Ми отримали запит на скидання пароля для вашого облікового запису <strong>FamWake</strong> Сімейний Будильник.",
    instruction: "Натисніть кнопку нижче, щоб встановити новий пароль:",
    button: "Скинути пароль",
    fallback: "Якщо кнопка не працює, скопіюйте це посилання у браузер:",
    security: "⚠️ Якщо ви <strong>не</strong> робили цей запит, просто ігноруйте цей лист. Ваш пароль залишиться без змін. Якщо ви помітили підозрілу активність, звʼяжіться з нами: daniel.notthoff@gmail.com",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідайте на нього безпосередньо.",
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
    subject: "✅ Contraseña cambiada con éxito – FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "¡Hola!",
    intro: "La contraseña de su cuenta de <strong>FamWake</strong> Familienwecker se ha cambiado con éxito.",
    instruction: "Ya puede iniciar sesión en la aplicación con su nueva contraseña.",
    security: "⚠️ Si <strong>no</strong> ha cambiado su contraseña, póngase en contacto con nosotros inmediatamente: daniel.notthoff@gmail.com",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no responda directamente a este correo.",
  },
  fr: {
    subject: "✅ Mot de passe modifié avec succès – FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bonjour !",
    intro: "Le mot de passe de votre compte <strong>FamWake</strong> Réveil Familial a été modifié avec succès.",
    instruction: "Vous pouvez désormais vous connecter à l'application avec votre nouveau mot de passe.",
    security: "⚠️ Si vous <strong>n'avez pas</strong> modifié votre mot de passe, veuillez nous contacter immédiatement : daniel.notthoff@gmail.com",
    footerNote: "Ceci est un message généré automatiquement. Veuillez ne pas répondre directement à cet e-mail.",
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
    intro: "Пароль вашей учётной записи <strong>FamWake</strong> Семейный Будильник был успешно изменён.",
    instruction: "Теперь вы можете войти в приложение с новым паролем.",
    security: "⚠️ Если вы <strong>не</strong> меняли пароль, немедленно свяжитесь с нами: daniel.notthoff@gmail.com",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечайте на него напрямую.",
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
    intro: "<strong>FamWake</strong> Aile Alarmı hesabınızın şifresi başarıyla değiştirildi.",
    instruction: "Artık yeni şifrenizle uygulamaya giriş yapabilirsiniz.",
    security: "⚠️ Şifrenizi <strong>siz değiştirmediyseniz</strong>, lütfen hemen bizimle iletişime geçin: daniel.notthoff@gmail.com",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt vermeyin.",
  },
  uk: {
    subject: "✅ Пароль успішно змінено – FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Привіт!",
    intro: "Пароль вашого облікового запису <strong>FamWake</strong> Сімейний Будильник було успішно змінено.",
    instruction: "Тепер ви можете увійти в застосунок з новим паролем.",
    security: "⚠️ Якщо ви <strong>не</strong> змінювали пароль, негайно звʼяжіться з нами: daniel.notthoff@gmail.com",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідайте на нього безпосередньо.",
  },
};

function buildEmailHtml(link, lang) {
  const t = EMAIL_CONTENT[lang] || EMAIL_CONTENT.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  
  // Link labels
  const labels = {
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
    tr: { home: "Web Sitesi", privacy: "Gizlilik Politikası", imprint: "Yasal Uyardı" },
    uk: { home: "Веб-сайт", privacy: "Політика конфіденційності", imprint: "Правова інформація" },
  };
  const l = labels[lang] || labels.en;

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
    const rawLang = (request.data?.language || "de").toLowerCase();
    const requestedLang = DIALECT_TO_LANG[rawLang] || rawLang.slice(0, 2);
    const supportedLangs = ["de", "en", "es", "fr", "it", "da", "ja", "nl", "no", "pl", "pt", "ru", "sv", "tr", "uk"];
    const lang = supportedLangs.includes(requestedLang) ? requestedLang : "en";
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
      await checkEmailRateLimit(email.trim());
      const linkOriginal = await admin.auth().generatePasswordResetLink(email.trim());
      let link = linkOriginal.replace("deine-domain.de", "www.familienwecker.de");

      // Dynamically switch to localized HTML page
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
      // Accessing error code from Firebase Admin SDK error
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
  
  const labels = {
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
    tr: { home: "Web Sitesi", privacy: "Gizlilik Politikası", imprint: "Yasal Uyardı" },
    uk: { home: "Веб-сайт", privacy: "Політика конфіденційності", imprint: "Правова інформація" },
  };
  const l = labels[lang] || labels.en;

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
    const rawLang = (request.data?.language || "de").toLowerCase();
    const requestedLang = DIALECT_TO_LANG[rawLang] || rawLang.slice(0, 2);
    const supportedLangs = ["de", "en", "es", "fr", "it", "da", "ja", "nl", "no", "pl", "pt", "ru", "sv", "tr", "uk"];
    const lang = supportedLangs.includes(requestedLang) ? requestedLang : "en";

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim());
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
    subject: "🚀 Confirma tu cuenta de FamWake Familienwecker",
    appName: "<strong>FamWake</strong> Familienwecker",
    greeting: "¡Bienvenido a FamWake Familienwecker!",
    intro: "Gracias por registrarte en <strong>FamWake</strong> Familienwecker. ¡Estamos deseando ayudarte a ti y a tu familia a tener una mañana relajada y sin caos!",
    instruction: "Por favor, confirma tu dirección de correo electrónico para activar tu cuenta:",
    button: "Confirmar correo electrónico",
    fallback: "Si el botón no funciona, copia este enlace en tu navegador:",
    privacy: "<strong>Nota:</strong> Por razones de privacidad, este enlace y tus datos de registro no confirmados se eliminarán automáticamente después de 48 horas si no se realiza la activación.",
    security: "Si no has creado esta cuenta, puedes ignorar este correo. No se guardará ningún dato de forma permanente.",
    footerNote: "Este es un mensaje generado automáticamente. Por favor, no respondas directamente a este correo.",
  },
  fr: {
    subject: "🚀 Confirmez votre compte FamWake Réveil Familial",
    appName: "<strong>FamWake</strong> Réveil Familial",
    greeting: "Bienvenue chez FamWake Réveil Familial !",
    intro: "Merci de vous être inscrit à <strong>FamWake</strong> Réveil Familial. Nous avons hâte de vous aider, vous et votre famille, à passer une matinée détendue et sans chaos !",
    instruction: "Veuillez confirmer votre adresse e-mail pour activer votre compte :",
    button: "Confirmer l'adresse e-mail",
    fallback: "Si le bouton ne fonctionne pas, veuillez copier ce lien dans votre navigateur :",
    privacy: "<strong>Note :</strong> Pour des raisons de confidentialité, ce lien et vos données d'inscription non confirmées seront automatiquement supprimés après 48 heures si aucune activation n'a lieu.",
    security: "Si vous n'avez pas créé ce compte, vous pouvez ignorer cet e-mail. Aucune donnée ne sera conservée de façon permanente.",
    footerNote: "Ceci est un message généré automatiquement. Veuillez ne pas répondre directement à cet e-mail.",
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
    subject: "🚀 Bekæft din FamWake Familievækker-konto",
    appName: "<strong>FamWake</strong> Familievækker",
    greeting: "Velkommen til FamWake Familievækker!",
    intro: "Tak for din tilmelding til <strong>FamWake</strong> Familievækker. Vi glæder os til at hjælpe dig og din familie til en rolig morgen uden kaos!",
    instruction: "Bekæft venligst din e-mailadresse for at aktivere din konto:",
    button: "Bekæft e-mailadresse",
    fallback: "Hvis knappen ikke virker, skal du kopiere dette link til din browser:",
    privacy: "<strong>Bemærk:</strong> Af hensyn til privatlivets fred vil dette link og dine ubekæftede registreringsdata automatisk blive slettet efter 48 timer, hvis ingen aktivering sker.",
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
    subject: "🚀 Подтвердите свою учётную запись FamWake Семейный Будильник",
    appName: "<strong>FamWake</strong> Семейный Будильник",
    greeting: "Добро пожаловать в FamWake Семейный Будильник!",
    intro: "Спасибо за регистрацию в <strong>FamWake</strong> Семейный Будильник. Мы рады помочь вам и вашей семье начинать каждое утро спокойно и без хаоса!",
    instruction: "Подтвердите свой адрес электронной почты для активации учётной записи:",
    button: "Подтвердить адрес электронной почты",
    fallback: "Если кнопка не работает, скопируйте эту ссылку в браузер:",
    privacy: "<strong>Примечание:</strong> В целях защиты конфиденциальности эта ссылка и ваши неподтверждённые данные будут автоматически удалены через 48 часов, если активация не будет выполнена.",
    security: "Если вы не создавали этот аккаунт, просто проигнорируйте это письмо. Никакие данные не будут сохранены постоянно.",
    footerNote: "Это автоматически сгенерированное сообщение. Пожалуйста, не отвечайте на него напрямую.",
  },
  sv: {
    subject: "🚀 Bekäfta ditt FamWake Familjens Väckarklocka-konto",
    appName: "<strong>FamWake</strong> Familjens Väckarklocka",
    greeting: "Välkommen till FamWake Familjens Väckarklocka!",
    intro: "Tack för att du registrerade dig på <strong>FamWake</strong> Familjens Väckarklocka. Vi ser fram emot att hjälpa dig och din familj att börja varje morgon avslappnad och utan kaos!",
    instruction: "Bekäfta din e-postadress för att aktivera ditt konto:",
    button: "Bekäfta e-postadress",
    fallback: "Om knappen inte fungerar, kopiera den här länken till din webbläsare:",
    privacy: "<strong>Obs:</strong> Av integritetsskydd kommer den här länken och dina obekäftade registreringsdata att raderas automatiskt efter 48 timmar om ingen aktivering sker.",
    security: "Om du inte skapade det här kontot kan du ignorera det här e-postmeddelandet. Inga data kommer att lagras permanent.",
    footerNote: "Detta är ett automatiskt genererat meddelande. Svara inte direkt på detta e-postmeddelande.",
  },
  tr: {
    subject: "🚀 FamWake Aile Alarmı hesabınızı onaylayın",
    appName: "<strong>FamWake</strong> Aile Alarmı",
    greeting: "FamWake Aile Alarmı'na hoş geldiniz!",
    intro: "<strong>FamWake</strong> Aile Alarmı'na kaydolduğunuz için teşekkür ederiz. Sizi ve ailenizi her sabah sakin ve stressiz bir şekilde başlatmanıza yardımcı olmak için sabırlanıyoruz!",
    instruction: "Hesabınızı etkinleştirmek için e-posta adresinizi onaylayın:",
    button: "E-posta adresini onayla",
    fallback: "Düğme çalışmıyorsa, bu bağlantıyı tarayıcınıza kopyalayın:",
    privacy: "<strong>Not:</strong> Gizlilik nedeniyle, bu bağlantı ve onaylanmamış kayıt verileriniz, aktivasyon gerçekleşmezse 48 saat sonra otomatik olarak silinecektir.",
    security: "Bu hesabı siz oluşturmadıysanız, bu e-postayı görmezden gelebilirsiniz. Hiçbir veri kalıcı olarak depolanmayacak.",
    footerNote: "Bu otomatik olarak oluşturulmuş bir mesajdır. Lütfen bu e-postaya doğrudan yanıt vermeyin.",
  },
  uk: {
    subject: "🚀 Підтвердьте обліковий запис FamWake Сімейний Будильник",
    appName: "<strong>FamWake</strong> Сімейний Будильник",
    greeting: "Ласкаво просимо до FamWake Сімейний Будильник!",
    intro: "Дякуємо за реєстрацію в <strong>FamWake</strong> Сімейний Будильник. Ми раді допомогти вам і вашій родині починати кожен ранок спокійно та без хаосу!",
    instruction: "Підтвердьте свою електронну адресу для активації облікового запису:",
    button: "Підтвердити електронну адресу",
    fallback: "Якщо кнопка не працює, скопіюйте це посилання у браузер:",
    privacy: "<strong>Примітка:</strong> З міркувань конфіденційності це посилання та ваші непідтверджені дані реєстрації будуть автоматично видалені через 48 годин, якщо активація не буде виконана.",
    security: "Якщо ви не створювали цей обліковий запис, просто ігноруйте цей лист. Жодні дані не будуть збережені постійно.",
    footerNote: "Це автоматично згенероване повідомлення. Будь ласка, не відповідайте на нього безпосередньо.",
  },
};

function buildVerifyEmailHtml(link, lang) {
  const t = EMAIL_CONTENT_VERIFY[lang] || EMAIL_CONTENT_VERIFY.en;
  const privacyUrl = `https://www.familienwecker.de/privacy-policy${lang === "de" ? "" : "-" + lang}.html`;
  const imprintUrl = `https://www.familienwecker.de/imprint${lang === "de" ? "" : "-" + lang}.html`;
  
  const labels = {
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
    tr: { home: "Web Sitesi", privacy: "Gizlilik Politikası", imprint: "Yasal Uyardı" },
    uk: { home: "Веб-сайт", privacy: "Політика конфіденційності", imprint: "Правова інформація" },
  };
  const l = labels[lang] || labels.en;

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
    const email = request.data?.email;
    const rawLang = (request.data?.language || "de").toLowerCase();
    const requestedLang = DIALECT_TO_LANG[rawLang] || rawLang.slice(0, 2);
    const lang = ["de", "en", "es", "fr", "it", "da", "ja", "nl", "no", "pl", "pt", "ru", "sv", "tr", "uk"].includes(requestedLang) ? requestedLang : "en";

    if (!email) {
      throw new HttpsError("invalid-argument", "INVALID_EMAIL");
    }

    const resendKey = process.env.RESEND_API_KEY;
    if (!resendKey) {
      console.error("RESEND_API_KEY secret is not set.");
      throw new HttpsError("failed-precondition", "Email service is not configured.");
    }

    try {
      await checkEmailRateLimit(email.trim());
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
  { region: "europe-west3" },
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
      const isAdmin = adminDoc.exists || uid === PRIMARY_ADMIN_UID;
      
      if (!isAdmin) {
        const minuteLimited = await checkSingleRateLimit(`join_${uid}_m`, 60 * 1000, 5);
        if (minuteLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`join_${uid}_d`, 24 * 60 * 60 * 1000, 10);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing join rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden
        if (!adminDoc.exists && uid === PRIMARY_ADMIN_UID) {
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
  { region: "europe-west3" },
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
      const isAdmin = adminDoc.exists || uid === PRIMARY_ADMIN_UID;
      
      if (!isAdmin) {
        const hourLimited = await checkSingleRateLimit(`create_${uid}_h`, 60 * 60 * 1000, 3);
        if (hourLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
        const dayLimited = await checkSingleRateLimit(`create_${uid}_d`, 24 * 60 * 60 * 1000, 6);
        if (dayLimited) throw new HttpsError("resource-exhausted", "TOO_MANY_REQUESTS");
      } else {
        console.log(`Bypassing create rate limit for admin UID: ${uid}`);
        // Bootstrap: _admins-Eintrag anlegen falls noch nicht vorhanden.
        // E-Mail-Prüfung entfällt – die äußere Bedingung (PRIMARY_ADMIN_UID) ist ausreichend.
        if (!adminDoc.exists && uid === PRIMARY_ADMIN_UID) {
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

    console.log(`Family '${sanitizedName}' created by ${uid} with id ${familyId} and code ${joinCode}`);

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
  { region: "europe-west3" },
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
    const isGlobalAdmin = adminDoc.exists || uid === PRIMARY_ADMIN_UID;

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

    // Feature #4: Alle Members über die Auflösung der Familie informieren (vor recursiveDelete)
    const allUserIds = (familyData.userIds || []).filter(id => id !== uid);
    notifyFamilyMemberLeft(allUserIds, uid).catch(err =>
      console.warn("notifyFamilyDeleted push failed (non-critical):", err?.message)
    );

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
    secrets: ["RESEND_API_KEY"],
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
      const isAdmin = adminDoc.exists || uid === PRIMARY_ADMIN_UID;
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
        const members = membersSnapshot.docs.map(m => m.data().name || "Unbekannt").join(", ");
        
        familiesHtml += `
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #eee;"><b>${family.name || family.familyName || "Unbenannt"}</b></td>
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
  { region: "europe-west3", secrets: ["RESEND_API_KEY"] },
  async (request) => {
    if (!request.auth) throw new HttpsError("unauthenticated", "LOGIN_REQUIRED");
    
    // Admin check
    const uid = request.auth.uid;
    const adminDoc = await admin.firestore().collection("_admins").doc(uid).get();
    if (!adminDoc.exists && uid !== PRIMARY_ADMIN_UID) {
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

  const tokens = tokensSnap.docs.map(d => d.id);

  const message = {
    tokens,
    data: {
      type:  payload.type  || "info",
      title: payload.title || "",
      body:  payload.body  || "",
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
            .collection("fcmTokens").doc(tokens[idx])
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
    const after  = event.data.after?.data();

    // Nur bei echten Updates reagieren (kein Create/Delete)
    if (!before || !after) return;

    // Relevante Felder prüfen: Weckzeiten + Reihenfolge + Tagesprofile
    const watchedFields = ["earliestWakeUp", "latestWakeUp", "order", "dayProfiles"];
    const changed = watchedFields.some(f => JSON.stringify(before[f]) !== JSON.stringify(after[f]));
    if (!changed) return;

    const familyId  = event.params.familyId;
    const changedBy = after.claimedByUserId || null;

    const membersSnap = await admin.firestore()
      .collection("families").doc(familyId)
      .collection("members")
      .get();

    const recipientUids = [];
    for (const doc of membersSnap.docs) {
      const uid = doc.data().claimedByUserId;
      if (!uid || uid === changedBy) continue;
      recipientUids.push(uid);
    }

    if (recipientUids.length === 0) return;

    const changerName = after.name || "Someone";
    await sendPushToUsers(recipientUids, {
      type:  "schedule_change",
      // Android lokalisiert self via type – kein EN-Text vom Server
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
