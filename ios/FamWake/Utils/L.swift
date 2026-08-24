import Foundation

/// Lokalisierungs-Shortcut – analog zu Android stringResource(R.string.xxx)
/// Nutzung: L.loginButton statt NSLocalizedString("login_button", ...)
enum L {
    static func s(_ key: String, _ args: CVarArg...) -> String {
        let bundle = LanguageManager.shared.bundle
        var format = bundle.localizedString(forKey: key, value: nil, table: "Localizable")
        if format == key || format.isEmpty {
            format = bundle.localizedString(forKey: key, value: nil, table: nil)
        }
        if format == key || format.isEmpty {
            if let enPath = Bundle.main.path(forResource: "en", ofType: "lproj"),
               let enBundle = Bundle(path: enPath) {
                format = enBundle.localizedString(forKey: key, value: nil, table: "Localizable")
            }
        }
        if format == key || format.isEmpty {
            format = Bundle.main.localizedString(forKey: key, value: key, table: "Localizable")
        }
        if args.isEmpty { return format }
        return String(format: format, arguments: args)
    }

    // MARK: - Auth
    static var loginButton: String { s("login_button") }
    static var registerButton: String { s("register_button") }
    static var emailLabel: String { s("email_label") }
    static var passwordLabel: String { s("password_label") }
    static var loginWithGoogle: String { s("login_with_google") }
    static var loginForgotPassword: String { s("login_forgot_password") }
    static var loginVerifyEmailTitle: String { s("login_verify_email_title") }
    static var loginVerifyEmailResend: String { s("login_verify_email_resend") }
    static var loginVerifyEmailConfirm: String { s("login_verify_email_confirm") }
    static var loginVerifyEmailNotVerified: String { s("login_verify_email_not_verified") }
    static var loginPasswordResetSent: String { s("login_password_reset_sent") }
    static var noAccount: String { s("no_account") }
    static var alreadyHaveAccount: String { s("already_have_account") }
    static var registrationTermsOfUse: String { s("registration_terms_of_use") }
    static var registrationPrivacyPolicy: String { s("registration_privacy_policy") }
    static func registrationDisclaimer(_ terms: String, _ privacy: String) -> String { String(format: s("registration_disclaimer"), terms, privacy) }
    static var loginVerifyEmailText: String { s("login_verify_email_text") }

        // MARK: - App Name
    static var appNameShort: String { s("app_name_short") }

    // MARK: - Settings
    static var settingsTitle: String { s("settings_title") }
    static var settingsProfileTitle: String { s("settings_profile_title") }
    static var settingsProfileDesc: String { s("settings_profile_desc") }
    static var settingsAlarmTitle: String { s("settings_alarm_title") }
    static var settingsAlarmDefault: String { s("settings_alarm_default") }
    static var settingsAlarmPickerTitle: String { s("settings_alarm_picker_title") }
    static var settingsAccountTitle: String { s("settings_account_title") }
    static var settingsJoinCode: String { s("settings_join_code") }
    static func settingsJoinCodeName(_ name: String) -> String { s("settings_join_code", name) }
    static var settingsShareCode: String { s("settings_share_code") }
    static func settingsShareMessage(_ familyName: String, _ code: String) -> String { s("settings_share_message", code, code, familyName) }
    static var settingsLeaveFamily: String { s("settings_leave_family") }
    static var settingsDeleteFamily: String { s("settings_delete_family") }
    static var settingsLogout: String { s("settings_logout") }
    static var settingsDeleteAccount: String { s("settings_delete_account") }
    static var settingsDisplayTitle: String { s("settings_display_title") }
    static var settingsLanguageTitle: String { s("settings_language_title") }
    static var settingsAppearanceTitle: String { s("settings_appearance_title") }
    static var settingsTooltipsTitle: String { s("settings_tooltips_title") }
    static var settingsTooltipsLabel: String { s("settings_tooltips_label") }
    static var settingsTooltipsReset: String { s("settings_tooltips_reset") }
    static var settingsSupportTitle: String { s("settings_support_title") }
    static var settingsBatteryWarningTitle: String { s("settings_battery_warning_title") }
    static var settingsPushTitle: String { s("settings_push_title") }
    static var settingsPushLabel: String { s("settings_push_label") }
    static var settingsPrivacyPolicy: String { s("settings_privacy_policy") }
    static var settingsImprint: String { s("settings_imprint") }
    static var settingsThemeDark: String { s("settings_theme_dark") }
    static var settingsThemeLight: String { s("settings_theme_light") }
    static var settingsLanguageSystem: String { s("settings_language_system") }
    static var settingsLanguageGerman: String { s("settings_language_german") }
    static var settingsLanguageEnglish: String { s("settings_language_english") }
    static var settingsLanguageFrench: String { s("settings_language_french") }
    static var settingsLanguageSpanish: String { s("settings_language_spanish") }
    static var settingsLanguageItalian: String { s("settings_language_italian") }
    static var settingsLanguagePolish: String { s("settings_language_polish") }
    static var settingsLanguageDutch: String { s("settings_language_dutch") }
    static var settingsLanguagePortuguese: String { s("settings_language_portuguese") }
    static var settingsLanguageSchweizerdeutsch: String { s("settings_language_schweizerdeutsch") }
    static var settingsLanguageSchwaebisch: String { s("settings_language_schwaebisch") }
    static var settingsLanguageRuhrpott: String { s("settings_language_ruhrpott") }
    static var settingsFeedbackButton: String { s("settings_feedback_button") }
    static var settingsAlreadyClaimed: String { s("settings_already_claimed") }
    static var settingsNoMembers: String { s("settings_no_members") }
    static var settingsPleaseSelect: String { s("settings_please_select") }
    static var settingsDonateSuccess: String { s("settings_donate_success") }
    private static var currentLang: String {
        let identifier = LanguageManager.shared.currentLocale.identifier
        let parts = identifier.split(separator: "_")
        if let first = parts.first {
            return String(first).lowercased()
        }
        return "en"
    }

    private static func isGermanLang(_ lang: String) -> Bool {
        return lang == "de" || lang == "gsw" || lang == "swg" || lang == "ksh"
    }

    static var settingsTermsOfUseUrl: String {
        let lang = currentLang
        if isGermanLang(lang) { return "https://www.familienwecker.de/terms.html" }
        if lang == "da" { return "https://www.familienwecker.de/terms-da.html" }
        if lang == "es" { return "https://www.familienwecker.de/terms-es.html" }
        if lang == "fr" { return "https://www.familienwecker.de/terms-fr.html" }
        if lang == "it" { return "https://www.familienwecker.de/terms-it.html" }
        if lang == "ja" { return "https://www.familienwecker.de/terms-ja.html" }
        if lang == "nl" { return "https://www.familienwecker.de/terms-nl.html" }
        if lang == "no" || lang == "nb" { return "https://www.familienwecker.de/terms-no.html" }
        if lang == "pl" { return "https://www.familienwecker.de/terms-pl.html" }
        if lang == "pt" { return "https://www.familienwecker.de/terms-pt.html" }
        if lang == "ru" { return "https://www.familienwecker.de/terms-ru.html" }
        if lang == "sv" { return "https://www.familienwecker.de/terms-sv.html" }
        if lang == "uk" { return "https://www.familienwecker.de/terms-uk.html" }
        if lang == "zh" { return "https://www.familienwecker.de/terms-zh-CN.html" }
        if lang == "id" || lang == "in" { return "https://www.familienwecker.de/terms-id.html" }
        return "https://www.familienwecker.de/terms-en.html"
    }

    static var settingsPrivacyPolicyUrl: String {
        let lang = currentLang
        if isGermanLang(lang) { return "https://www.familienwecker.de/privacy-policy.html" }
        if lang == "da" { return "https://www.familienwecker.de/privacy-policy-da.html" }
        if lang == "es" { return "https://www.familienwecker.de/privacy-policy-es.html" }
        if lang == "fr" { return "https://www.familienwecker.de/privacy-policy-fr.html" }
        if lang == "it" { return "https://www.familienwecker.de/privacy-policy-it.html" }
        if lang == "ja" { return "https://www.familienwecker.de/privacy-policy-ja.html" }
        if lang == "nl" { return "https://www.familienwecker.de/privacy-policy-nl.html" }
        if lang == "no" || lang == "nb" { return "https://www.familienwecker.de/privacy-policy-no.html" }
        if lang == "pl" { return "https://www.familienwecker.de/privacy-policy-pl.html" }
        if lang == "pt" { return "https://www.familienwecker.de/privacy-policy-pt.html" }
        if lang == "ru" { return "https://www.familienwecker.de/privacy-policy-ru.html" }
        if lang == "sv" { return "https://www.familienwecker.de/privacy-policy-sv.html" }
        if lang == "uk" { return "https://www.familienwecker.de/privacy-policy-uk.html" }
        if lang == "zh" { return "https://www.familienwecker.de/privacy-policy-zh-CN.html" }
        if lang == "id" || lang == "in" { return "https://www.familienwecker.de/privacy-policy-id.html" }
        return "https://www.familienwecker.de/privacy-policy-en.html"
    }

    static var settingsDeleteAccountUrl: String {
        let lang = currentLang
        if isGermanLang(lang) { return "https://www.familienwecker.de/account-deletion.html" }
        if lang == "da" { return "https://www.familienwecker.de/account-deletion-da.html" }
        if lang == "es" { return "https://www.familienwecker.de/account-deletion-es.html" }
        if lang == "fr" { return "https://www.familienwecker.de/account-deletion-fr.html" }
        if lang == "it" { return "https://www.familienwecker.de/account-deletion-it.html" }
        if lang == "ja" { return "https://www.familienwecker.de/account-deletion-ja.html" }
        if lang == "nl" { return "https://www.familienwecker.de/account-deletion-nl.html" }
        if lang == "no" || lang == "nb" { return "https://www.familienwecker.de/account-deletion-no.html" }
        if lang == "pl" { return "https://www.familienwecker.de/account-deletion-pl.html" }
        if lang == "pt" { return "https://www.familienwecker.de/account-deletion-pt.html" }
        if lang == "ru" { return "https://www.familienwecker.de/account-deletion-ru.html" }
        if lang == "sv" { return "https://www.familienwecker.de/account-deletion-sv.html" }
        if lang == "uk" { return "https://www.familienwecker.de/account-deletion-uk.html" }
        if lang == "zh" { return "https://www.familienwecker.de/account-deletion-zh-CN.html" }
        if lang == "id" || lang == "in" { return "https://www.familienwecker.de/account-deletion-id.html" }
        return "https://www.familienwecker.de/account-deletion-en.html"
    }

    static var settingsImprintUrl: String {
        let lang = currentLang
        if isGermanLang(lang) { return "https://www.familienwecker.de/imprint.html" }
        if lang == "da" { return "https://www.familienwecker.de/imprint-da.html" }
        if lang == "es" { return "https://www.familienwecker.de/imprint-es.html" }
        if lang == "fr" { return "https://www.familienwecker.de/imprint-fr.html" }
        if lang == "it" { return "https://www.familienwecker.de/imprint-it.html" }
        if lang == "ja" { return "https://www.familienwecker.de/imprint-ja.html" }
        if lang == "nl" { return "https://www.familienwecker.de/imprint-nl.html" }
        if lang == "no" || lang == "nb" { return "https://www.familienwecker.de/imprint-no.html" }
        if lang == "pl" { return "https://www.familienwecker.de/imprint-pl.html" }
        if lang == "pt" { return "https://www.familienwecker.de/imprint-pt.html" }
        if lang == "ru" { return "https://www.familienwecker.de/imprint-ru.html" }
        if lang == "sv" { return "https://www.familienwecker.de/imprint-sv.html" }
        if lang == "uk" { return "https://www.familienwecker.de/imprint-uk.html" }
        if lang == "id" || lang == "in" { return "https://www.familienwecker.de/imprint-id.html" }
        return "https://www.familienwecker.de/imprint-en.html"
    }

    // MARK: - FamilySetup
    static var setupCreateTab: String { s("setup_create_tab") }
    static var setupJoinTab: String { s("setup_join_tab") }
    static var setupFamilyName: String { s("setup_family_name") }
    static var setupCreateButton: String { s("setup_create_button") }
    static var setupJoinCodeLabel: String { s("setup_join_code_label") }
    static var setupJoinCodePlaceholder: String { s("setup_join_code_placeholder") }
    static var setupJoinButton: String { s("setup_join_button") }

    // MARK: - Main
    static var mainAlarmEnabled: String { s("main_alarm_enabled") }
    static var mainAlarmDisabled: String { s("main_alarm_disabled") }
    static var mainAlarmEnabledDesc: String { s("main_alarm_enabled_desc") }
    static var mainAlarmDisabledDesc: String { s("main_alarm_disabled_desc") }
    static var mainCurrentSchedule: String { s("main_current_schedule") }
    static var mainOptimalPlan: String { s("main_optimal_plan") }
    static var mainPlanPaused: String { s("main_plan_paused") }
    static var mainNoProfileWarning: String { s("main_no_profile_warning") }
    static var mainNoProfileWarningDesc: String { s("main_no_profile_warning_desc") }
    static func mainSnoozeActive(_ time: String) -> String { s("main_snooze_active", time) }
    static func mainScheduleBathroom(_ start: String, _ end: String) -> String { s("main_schedule_bathroom", start, end) }
    static func mainScheduleLeave(_ time: String) -> String { s("main_schedule_leave", time) }
    static func mainFallbackAlarmActive(_ time: String) -> String { s("main_fallback_alarm_active", time) }
    static func scheduleMessageTimeAdjusted(_ min: Int) -> String { s("schedule_message_time_adjusted", min) }
    static func scheduleMessageBreakfastReduced(_ min: Int) -> String { s("schedule_message_breakfast_reduced", min) }
    static func scheduleMessageBreakfastAndTimeAdjusted(_ min1: Int, _ min2: Int) -> String { s("schedule_message_breakfast_and_time_adjusted", min1, min2) }
    static var scheduleAutoFix: String { s("schedule_auto_fix") }
    static var mainMemberLimitReached: String { s("main_member_limit_reached") }
    static func mainSharedBreakfast(_ time: String) -> String { s("main_shared_breakfast", time) }

    // MARK: - Buffer
    static var bufferAfterBath: String { s("buffer_after_bath") }
    static func bufferBetweenDisplay(_ min: Int) -> String { s("buffer_between_display", min) }
    static func scheduleMessageBufferReduced(_ min1: Int, _ min2: Int) -> String { s("schedule_message_buffer_reduced", min1, min2) }

    // MARK: - Awake
    static var awakeTodayDesc: String { s("awake_today_desc") }
    static var awakeActiveDesc: String { s("awake_active_desc") }

    // MARK: - Member
    static var simpleModeTitle: String { s("simple_mode_title") }
    static var simpleModeDesc: String { s("simple_mode_desc") }
    static var mainFamilyMembers: String { s("main_family_members") }
    static var memberStatusPaused: String { s("member_status_paused") }
    static var addMemberTitleAdd: String { s("add_member_title_add") }
    static var addMemberTitleEdit: String { s("add_member_title_edit") }
    static var addMemberNameLabel: String { s("add_member_name_label") }
    static var addMemberEarliestWake: String { s("add_member_earliest_wake") }
    static var addMemberLatestWake: String { s("add_member_latest_wake") }
    static var addMemberBathroomDuration: String { s("add_member_bathroom_duration") }
    static var addMemberWantsBreakfast: String { s("add_member_wants_breakfast") }
    static var addMemberLeaveHome: String { s("add_member_leave_home") }
    static var addMemberDayActive: String { s("add_member_day_active") }
    static var addMemberDayProfilesTitle: String { s("add_member_day_profiles_title") }
    static func addMemberCopyToDays(_ day: String) -> String { s("add_member_copy_to_days", day) }
    static var addMemberCopyDialogTitle: String { s("add_member_copy_dialog_title") }
    static var addMemberCopyApply: String { s("add_member_copy_apply") }
    static var addMemberSubmit: String { s("add_member_submit") }

    // MARK: - Feedback
    static var feedbackTitle: String { s("feedback_title") }
    static var feedbackIntro: String { s("feedback_intro") }
    static var feedbackCategoryLabel: String { s("feedback_category_label") }
    static var feedbackCategoryBug: String { s("feedback_category_bug") }
    static var feedbackCategoryFeature: String { s("feedback_category_feature") }
    static var feedbackCategoryPraise: String { s("feedback_category_praise") }
    static var feedbackCategoryOther: String { s("feedback_category_other") }
    static var feedbackMessageLabel: String { s("feedback_message_label") }
    static var feedbackMessagePlaceholder: String { s("feedback_message_placeholder") }
    static var feedbackEmailLabel: String { s("feedback_email_label") }
    static var feedbackEmailPlaceholder: String { s("feedback_email_placeholder") }
    static var feedbackSend: String { s("feedback_send") }
    static var feedbackCancel: String { s("feedback_cancel") }
    static var feedbackSuccessTitle: String { s("feedback_success_title") }
    static var feedbackSuccessMessage: String { s("feedback_success_message") }
    static func feedbackAutoVersion(_ v: String) -> String { s("feedback_auto_version", v) }
    static func feedbackAutoDevice(_ d: String) -> String { s("feedback_auto_device", d) }
    static var feedbackAutoInfoTitle: String { s("feedback_auto_info_title") }

    // MARK: - Onboarding
    static var onboardingNext: String { s("onboarding_next") }
    static var onboardingDone: String { s("onboarding_done") }
    static var onboardingSkip: String { s("onboarding_skip") }

    // MARK: - Push Notifications
    static var notifScheduleChangedTitle: String { s("notif_schedule_changed_title") }
    static var notifScheduleChangedBody: String { s("notif_schedule_changed_body") }
    static var notifMemberJoinedTitle: String { s("notif_member_joined_title") }
    static var notifMemberJoinedBody: String { s("notif_member_joined_body") }
    static var notifMemberLeftTitle: String { s("notif_member_left_title") }
    static var notifMemberLeftBody: String { s("notif_member_left_body") }

    // MARK: - Ringing
    static func ringingWakeUp(_ name: String) -> String { s("ringing_wake_up", name) }
    static var ringingSnooze: String { s("ringing_snooze") }
    static var ringingStop: String { s("ringing_stop") }
    static var ringingMessagesArray: String { s("ringing_messages_array") }
    static var snoozeMaxReached: String { s("snooze_max_reached") }
    static func snoozeCounter(_ current: Int, _ max: Int) -> String {
        String(format: s("snooze_counter"), current, max)
    }
    static var notifSnoozeShiftTitle: String { s("notif_snooze_shift_title") }
    static func notifSnoozeShiftBody(_ name: String, _ time: String) -> String {
        String(format: s("notif_snooze_shift_body"), name, time)
    }
    static var scheduleMemberSnoozed: String { s("schedule_member_snoozed") }

    // MARK: - Tooltips
    static var tooltipAwakeButton: String { s("tooltip_awake_button") }
    static var tooltipDragHandle: String { s("tooltip_drag_handle") }
    static var tooltipAlarmSwitch: String { s("tooltip_alarm_switch") }
    static var tooltipInviteCode: String { s("tooltip_invite_code") }
    static var tooltipAlarmSound: String { s("tooltip_alarm_sound") }
    static var tooltipWakeWindow: String { s("tooltip_wake_window") }
    static var tooltipBathroom: String { s("tooltip_bathroom") }
    static var tooltipWeekdays: String { s("tooltip_weekdays") }
    static var tooltipBuffer: String { s("tooltip_buffer") }

    // MARK: - Validation
    static var validationLatestBeforeEarliest: String { s("validation_latest_before_earliest") }
    static var validationLeaveTooEarly: String { s("validation_leave_too_early") }

    // MARK: - Misc
    static var cancelButton: String { s("cancel_button") }
    static var okButton: String { s("ok_button") }
    static var backDesc: String { s("back_desc") }
    static var unsavedChangesTitle: String { s("unsaved_changes_title") }
    static var unsavedChangesMessage: String { s("unsaved_changes_message") }
    static var unsavedChangesDiscard: String { s("unsaved_changes_discard") }
    static var unsavedChangesKeep: String { s("unsaved_changes_keep") }
    static var joinLoadingText: String { s("join_loading_text") }

    // MARK: - Errors
    static var errorGeneric: String { s("add_member_unknown") }
    static var errorFamilyNotFound: String { s("error_family_not_found") }
    static var errorInvalidCode: String { s("error_invalid_code") }
    static var errorProfileTaken: String { s("error_profile_taken") }
    static var errorProfileClaimOffline: String { s("error_profile_claim_offline") }
    static var errorDeleteNotAdmin: String { s("error_delete_not_admin") }
    static var errorWrongPassword: String { s("error_wrong_password") }
    static var errorUserNotFound: String { s("error_user_not_found") }
    static var errorEmailInUse: String { s("error_email_in_use") }
    static var errorWeakPassword: String { s("error_weak_password") }
    static var errorInvalidEmail: String { s("error_invalid_email") }
    static var errorNetwork: String { s("error_network") }
    static var errorAlarmPermission: String { s("error_alarm_permission") }
    static var errorOffline: String { s("error_offline") }
    static var offlineWriteHint: String { s("offline_write_hint") }

    // MARK: - Weekdays
    static func weekday(_ day: Int) -> String { s("weekday_\(day)") }
    static func weekdayShort(_ day: Int) -> String { s("weekday_short_\(day)") }

    // MARK: - Empty State
    static var emptyScheduleTitle: String { s("empty_schedule_title") }
    static var emptyScheduleDescription: String { s("empty_schedule_description") }
    static var emptyMembersTitle: String { s("empty_members_title") }
    static var emptyMembersDescription: String { s("empty_members_description") }

    // MARK: - Delete Family
    static var settingsDeleteFamilyDialogTitle: String { s("settings_delete_family_dialog_title") }
    static var settingsDeleteFamilyDialogText: String { s("settings_delete_family_dialog_text") }
    static var settingsDeleteFamilyDialogConfirm: String { s("settings_delete_family_dialog_confirm") }
    static var settingsDeleteFamilyDialogCancel: String { s("settings_delete_family_dialog_cancel") }
    static var settingsDeleteFamilyWarningTitle: String { s("settings_delete_family_warning_title") }
    static var settingsDeleteFamilyWarningText: String { s("settings_delete_family_warning_text") }
    static var settingsDeleteFamilyWarningConfirm: String { s("settings_delete_family_warning_confirm") }

    // MARK: - Delete Member
    static var settingsDeleteMemberTitle: String { s("delete_member_title") }
    static var settingsDeleteMemberConfirm: String { s("delete_confirm") }

    // MARK: - Help / Tour
    static var settingsHelpTitle: String { s("settings_help_feedback_title") }
    static var settingsRestartTour: String { s("settings_restart_tour") }

    // MARK: - Admin (L14)
    static var settingsAdminTitle: String { s("settings_admin_title") }
    static var settingsTestAlarm: String { s("settings_test_alarm") }

    // MARK: - Einheiten (L15)
    static func minutesSuffix(_ min: Int) -> String { s("minutes_suffix", min) }

    // MARK: - Audit M15/M16
    static var errorTitle: String { s("error_title") }
    static var errorNoMailApp: String { s("error_no_mail_app") }
}
