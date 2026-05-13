import SwiftUI

// MARK: - Slide Model

private struct OnboardingSlide {
    let titleKey: String
    let bodyKey: String
    let gradient: [Color]
    let icon: String     // SF Symbol für alle Slides
    let isLottie: Bool
}

private let slides: [OnboardingSlide] = [
    OnboardingSlide(
        titleKey: "onboarding_slide0_title",
        bodyKey:  "onboarding_slide0_body",
        gradient: [Color(hex: "#FF6B6B"), Color(hex: "#FF8E53")],
        icon: "alarm.fill",
        isLottie: true
    ),
    OnboardingSlide(
        titleKey: "onboarding_slide1_title",
        bodyKey:  "onboarding_slide1_body",
        gradient: [Color(hex: "#4776E6"), Color(hex: "#8E54E9")],
        icon: "person.3.fill",
        isLottie: false
    ),
    OnboardingSlide(
        titleKey: "onboarding_slide2_title",
        bodyKey:  "onboarding_slide2_body",
        gradient: [Color(hex: "#11998e"), Color(hex: "#38ef7d")],
        icon: "clock.fill",
        isLottie: false
    ),
    OnboardingSlide(
        titleKey: "onboarding_slide3_title",
        bodyKey:  "onboarding_slide3_body",
        gradient: [Color(hex: "#373B44"), Color(hex: "#4286f4")],
        icon: "calendar.badge.clock",
        isLottie: false
    ),
    OnboardingSlide(
        titleKey: "onboarding_slide4_title",
        bodyKey:  "onboarding_slide4_body",
        gradient: [Color(hex: "#f953c6"), Color(hex: "#b91d73")],
        icon: "sun.max.fill",
        isLottie: false
    )
]

// MARK: - OnboardingView

struct OnboardingView: View {
    var onFinished: () -> Void
    @State private var currentPage = 0
    @State private var dragOffset: CGFloat = 0

    var body: some View {
        ZStack {
            // Farbiger Hintergrund aus aktuellem Slide
            LinearGradient(
                colors: slides[currentPage].gradient,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            .animation(.easeInOut(duration: 0.5), value: currentPage)

            // Subtiles Muster
            Circle()
                .fill(Color.white.opacity(0.06))
                .frame(width: 320, height: 320)
                .offset(x: 120, y: -200)
            Circle()
                .fill(Color.white.opacity(0.04))
                .frame(width: 240, height: 240)
                .offset(x: -130, y: 180)

            VStack(spacing: 0) {
                // Slide-Pager (keine TabView um Scroll-Clipping zu vermeiden)
                TabView(selection: $currentPage) {
                    ForEach(0..<slides.count, id: \.self) { index in
                        OnboardingSlideView(slide: slides[index])
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(maxHeight: .infinity)

                // Bottom Controls
                VStack(spacing: 14) {
                    // Dots
                    HStack(spacing: 8) {
                        ForEach(0..<slides.count, id: \.self) { i in
                            Capsule()
                                .fill(Color.white.opacity(i == currentPage ? 1.0 : 0.35))
                                .frame(width: i == currentPage ? 24 : 8, height: 8)
                                .animation(.spring(response: 0.3, dampingFraction: 0.7), value: currentPage)
                        }
                    }
                    .padding(.bottom, 4)

                    // Weiter / Los
                    Button(action: {
                        withAnimation {
                            if currentPage < slides.count - 1 {
                                currentPage += 1
                            } else {
                                onFinished()
                            }
                        }
                    }) {
                        Text(currentPage == slides.count - 1 ? L.onboardingDone : L.onboardingNext)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(slides[currentPage].gradient.first ?? .blue)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(Color.white)
                            .clipShape(RoundedRectangle(cornerRadius: 18))
                            .shadow(color: .black.opacity(0.15), radius: 12, x: 0, y: 6)
                    }
                    .buttonStyle(BounceButtonStyle())
                    .padding(.horizontal, 24)
                    .animation(.easeInOut(duration: 0.3), value: currentPage)

                    // Überspringen
                    if currentPage < slides.count - 1 {
                        Button(L.onboardingSkip, action: onFinished)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(Color.white.opacity(0.75))
                            .padding(.vertical, 6)
                    } else {
                        Spacer().frame(height: 30)
                    }
                }
                .padding(.bottom, 40)
            }
        }
    }
}

// MARK: - Single Slide

private struct OnboardingSlideView: View {
    let slide: OnboardingSlide

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            // Icon-Illustration
            if slide.isLottie {
                AnimatedAlarmView()
                    .frame(width: 200, height: 200)
            } else {
                ZStack {
                    Circle()
                        .fill(Color.white.opacity(0.15))
                        .frame(width: 180, height: 180)
                    Circle()
                        .fill(Color.white.opacity(0.1))
                        .frame(width: 140, height: 140)
                    Image(systemName: slide.icon)
                        .font(.system(size: 72, weight: .medium))
                        .foregroundStyle(Color.white.opacity(0.95))
                        .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
                }
                .frame(width: 200, height: 200)
            }

            Spacer().frame(height: 48)

            // Text
            VStack(spacing: 14) {
                Text(L.s(slide.titleKey))
                    .font(.system(size: 26, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(.horizontal, 28)

                Text(L.s(slide.bodyKey))
                    .font(.system(size: 16))
                    .foregroundStyle(Color.white.opacity(0.85))
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                    .lineSpacing(5)
                    .padding(.horizontal, 32)
            }

            Spacer()
        }
        .padding(.bottom, 80) // Platz für Bottom-Controls
    }
}

// MARK: - Animated Alarm (Lottie-Ersatz)

private struct AnimatedAlarmView: View {
    @State private var swing: Double = 0
    @State private var glow: Bool = false
    @State private var sparkle: Bool = false

    var body: some View {
        ZStack {
            // Äußerer Glow-Ring
            Circle()
                .stroke(Color.white.opacity(glow ? 0.3 : 0.05), lineWidth: 2)
                .frame(width: 170, height: 170)
                .scaleEffect(glow ? 1.1 : 1.0)
                .animation(.easeInOut(duration: 1.8).repeatForever(autoreverses: true), value: glow)

            // Innerer Kreis
            Circle()
                .fill(Color.white.opacity(glow ? 0.15 : 0.05))
                .frame(width: 140, height: 140)
                .animation(.easeInOut(duration: 2.2).repeatForever(autoreverses: true), value: glow)

            // Funken
            ForEach(0..<6) { i in
                let angle = Double(i) * 60.0
                Image(systemName: "sparkle")
                    .font(.system(size: 9))
                    .foregroundStyle(Color.white.opacity(sparkle ? 0.85 : 0.1))
                    .offset(
                        x: sparkle ? cos(angle * .pi / 180) * 72 : cos(angle * .pi / 180) * 50,
                        y: sparkle ? sin(angle * .pi / 180) * 72 : sin(angle * .pi / 180) * 50
                    )
                    .scaleEffect(sparkle ? 1.1 : 0.4)
                    .animation(.easeOut(duration: 1.3).repeatForever(autoreverses: true).delay(Double(i) * 0.2), value: sparkle)
            }

            // Glocke
            Image(systemName: "alarm.fill")
                .font(.system(size: 86, weight: .medium))
                .foregroundStyle(Color.white.opacity(0.95))
                .rotationEffect(.degrees(swing))
                .shadow(color: .black.opacity(0.25), radius: 12, x: 0, y: 4)
                .animation(
                    .easeInOut(duration: 0.1)
                    .repeatCount(8, autoreverses: true)
                    .delay(1.2)
                    .repeatForever(autoreverses: false),
                    value: swing
                )
        }
        .onAppear {
            glow = true
            sparkle = true
            swing = 7
        }
    }
}
