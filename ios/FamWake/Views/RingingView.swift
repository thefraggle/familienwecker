import SwiftUI
import AVFoundation
import UserNotifications
import Lottie
import Aptabase

/// RingingView – iOS-Äquivalent zur Android RingingActivity
/// Wird angezeigt wenn die Notification geöffnet wird oder per App-Link
struct RingingView: View {
    let memberId: String
    let memberName: String
    var isGreetingOnly: Bool = false
    @State private var snoozeCount: Int = 0
    var onStop: () -> Void
    var onSnooze: () -> Void

    @State private var isAnimating = false
    @State private var randomMessage: String = ""

    // Dynamic Type: Button-Höhe skaliert mit Systemschriftgröße
    @ScaledMetric(relativeTo: .headline) private var buttonHeight: CGFloat = 56

    var body: some View {
        ZStack {
            // Gradient: Dunkellila → Warm Peach
            LinearGradient(
                colors: [Color.ringingPurpleDark, Color.ringingPurpleMed, Color.ringingPeach],
                startPoint: .top, endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer().frame(height: 56)

                // Panda / Lottie Animation
                LottieView(animation: .named("wakeup"))
                    .playing(loopMode: .loop)
                    .frame(width: 250, height: 250)

                Spacer().frame(height: 40)

                // Texte
                VStack(spacing: 12) {
                    Text(L.ringingWakeUp(memberName))
                        .font(.title.weight(.black))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 28)

                    Text(randomMessage)
                        .font(.body)
                        .foregroundStyle(Color.white.opacity(0.85))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 28)
                }

                Spacer()

                // Buttons
                VStack(spacing: 16) {
                    if isGreetingOnly {
                        // OK Button
                        Button(action: {
                            onStop()
                        }) {
                            HStack(spacing: 10) {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.title3)
                                Text("OK")
                                    .font(.headline).fontWeight(.bold)
                            }
                            .foregroundStyle(Color.ringingPurpleDark)
                            .frame(maxWidth: .infinity)
                            .frame(minHeight: buttonHeight)
                            .background(Color.white.opacity(0.92))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(BounceButtonStyle())
                        .accessibilityLabel(L.s("accessibility_alarm_ok"))
                    } else {
                        // Stop – Solid
                        Button(action: {
                            Aptabase.shared.trackEvent("alarm_dismissed")
                            AlarmService.shared.stopAlarm()
                            onStop()
                        }) {
                            HStack(spacing: 10) {
                                Image(systemName: "alarm")
                                    .font(.title3)
                                Text(L.ringingStop)
                                    .font(.headline).fontWeight(.bold)
                            }
                            .foregroundStyle(Color.ringingPurpleDark)
                            .frame(maxWidth: .infinity)
                            .frame(minHeight: buttonHeight)
                            .background(Color.white.opacity(0.92))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        .buttonStyle(BounceButtonStyle())
                        .accessibilityLabel(L.s("accessibility_alarm_stop"))
                        .accessibilityHint(L.s("accessibility_alarm_stop_hint"))
    
                        // Snooze – Glasmorphism
                        Button(action: {
                            #if DEBUG
                            print("[RingingView] Snooze tapped! snoozeCount=\(snoozeCount), maxSnooze=\(SnoozeConfig.maxSnoozeCount)")
                            #endif
                            Aptabase.shared.trackEvent("alarm_snoozed", with: ["snooze_count": snoozeCount + 1])
                            AlarmService.shared.stopAlarm()
                            onSnooze()
                        }) {
                            HStack(spacing: 10) {
                                Image(systemName: "zzz")
                                    .font(.title3)
                                Text(L.snoozeCounter(snoozeCount + 1, SnoozeConfig.maxSnoozeCount))
                                    .font(.headline).fontWeight(.semibold)
                            }
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .frame(minHeight: buttonHeight)
                            .background(Color.white.opacity(0.18))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(Color.white.opacity(0.3), lineWidth: 1)
                            )
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .disabled(snoozeCount >= SnoozeConfig.maxSnoozeCount)
                        .opacity(snoozeCount >= SnoozeConfig.maxSnoozeCount ? 0.5 : 1.0)
                        .accessibilityLabel(L.s("accessibility_alarm_snooze"))
                        .accessibilityHint(L.s("accessibility_alarm_snooze_hint"))
                    }
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 48)
                .safeAreaPadding(.bottom)
            }
        }
        .onAppear {
            Aptabase.shared.trackEvent("alarm_triggered")
            let messages = L.ringingMessagesArray.components(separatedBy: "||")
            randomMessage = messages.randomElement() ?? "☀️"
            
            // Neuer Alarm-Zyklus: Count zurücksetzen wenn kein aktiver Snooze
            let snoozeUntilTs = UserDefaults.standard.double(forKey: "snooze_until")
            if snoozeUntilTs == 0 || snoozeUntilTs < Date().timeIntervalSince1970 {
                UserDefaults.standard.set(0, forKey: "snooze_count")
            }
            snoozeCount = UserDefaults.standard.integer(forKey: "snooze_count")
        }
        .statusBarHidden(true)
    }
}
