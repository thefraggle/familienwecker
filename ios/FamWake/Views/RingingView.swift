import SwiftUI
import AVFoundation
import UserNotifications
import Lottie

/// RingingView – iOS-Äquivalent zur Android RingingActivity
/// Wird angezeigt wenn die Notification geöffnet wird oder per App-Link
struct RingingView: View {
    let memberId: String
    let memberName: String
    var onStop: () -> Void
    var onSnooze: () -> Void

    @State private var isAnimating = false
    @State private var randomMessage: String = ""

    // Lottie Platzhalter (TODO: echte Lottie-Integration)
    @State private var iconScale: CGFloat = 1.0

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
                        .font(.system(size: 28, weight: .black))
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
                    // Snooze – Glasmorphism
                    Button(action: {
                        AlarmService.shared.stopAlarm()
                        onSnooze()
                    }) {
                        HStack(spacing: 10) {
                            Image(systemName: "zzz")
                                .font(.title3)
                            Text(L.ringingSnooze)
                                .font(.headline).fontWeight(.semibold)
                        }
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 60)
                        .background(Color.white.opacity(0.18))
                        .clipShape(Capsule())
                        .overlay(
                            Capsule()
                                .stroke(Color.white.opacity(0.3), lineWidth: 1)
                        )
                    }
                    .buttonStyle(BounceButtonStyle())

                    // Stop – Solid
                    Button(action: {
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
                        .frame(height: 60)
                        .background(Color.white.opacity(0.92))
                        .clipShape(Capsule())
                    }
                    .buttonStyle(BounceButtonStyle())
                }
                .padding(.horizontal, 28)
                .padding(.bottom, 48)
                .safeAreaPadding(.bottom)
            }
        }
        .onAppear {
            let messages = NSLocalizedString("ringing_messages_array", comment: "").components(separatedBy: "||")
            randomMessage = messages.randomElement() ?? "☀️"
        }
        .statusBarHidden(true)
    }
}
