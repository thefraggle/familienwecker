import SwiftUI
import Lottie

// MARK: - Onboarding (1:1 Android OnboardingScreen.kt)
// 4 Slides: Panda Lottie → Schedule Mockup → Invite Mockup → WakeUp Lottie
// Background: onboarding_bg.jpg + dark scrim
// Bottom: Page dots, tooltips checkbox, start/next button, skip/login links

struct OnboardingView: View {
    var startAtWelcome: Bool
    var onFinished: (_ tooltipsEnabled: Bool) -> Void
    var onLoginRequested: (() -> Void)?
    var isLoggedIn: Bool

    @State private var currentPage: Int
    @State private var tooltipsEnabled: Bool
    @State private var isStarting = false
    
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    init(startAtWelcome: Bool = false,
         onFinished: @escaping (_ tooltipsEnabled: Bool) -> Void,
         onLoginRequested: (() -> Void)? = nil,
         isLoggedIn: Bool = false) {
        self.startAtWelcome = startAtWelcome
        self.onFinished = onFinished
        self.onLoginRequested = onLoginRequested
        self.isLoggedIn = isLoggedIn
        let count = isLoggedIn ? 3 : 4
        self._currentPage = State(initialValue: startAtWelcome ? count - 1 : 0)
        self._tooltipsEnabled = State(initialValue: UserDefaults.standard.object(forKey: "tooltips_enabled") as? Bool ?? true)
    }

    private var actualSlideCount: Int { isLoggedIn ? 3 : 4 }
    private var isLastPage: Bool { currentPage == actualSlideCount - 1 }

    var body: some View {
        ZStack {
            // Background image from Android
            if let bgImage = UIImage(named: "onboarding_bg") {
                Image(uiImage: bgImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .ignoresSafeArea()
            } else {
                // Fallback gradient if image missing
                LinearGradient(colors: [theme.surface, theme.background],
                              startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
            }

            // Dark scrim for readability
            Color.black.opacity(0.45)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Pager
                TabView(selection: $currentPage) {
                    // Slide 0 – Panda Lottie
                    slideView(
                        titleKey: "onboarding_slide0_title",
                        bodyKey: "onboarding_slide0_body",
                        content: { lottieView("panda") }
                    ).tag(0)

                    // Slide 1 – Schedule Mockup (on-the-fly, localized)
                    slideView(
                        titleKey: "onboarding_slide1_title",
                        bodyKey: "onboarding_slide1_body",
                        content: { ScheduleMockup() }
                    ).tag(1)

                    // Slide 2 – Invite Mockup (on-the-fly, localized)
                    slideView(
                        titleKey: "onboarding_slide3_title",
                        bodyKey: "onboarding_slide3_body",
                        content: { InviteMockup() }
                    ).tag(2)

                    // Slide 3 – WakeUp Lottie (Only if not logged in)
                    if !isLoggedIn {
                        slideView(
                            titleKey: "onboarding_slide5_title",
                            bodyKey: "onboarding_slide5_body",
                            content: { lottieView("wakeup") }
                        ).tag(3)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.3), value: currentPage)
            }
            .safeAreaInset(edge: .bottom) {
                bottomControls
                    .padding(.bottom, 24)
            }
        }
    }

    // MARK: - Slide Template
    @ViewBuilder
    private func slideView(titleKey: String, bodyKey: String, @ViewBuilder content: @escaping () -> some View) -> some View {
        VStack(spacing: 0) {
            Spacer()

            content()
                .frame(maxWidth: 280, maxHeight: 280)

            Spacer().frame(height: 28)

            Text(L.s(titleKey))
                .font(.title2).fontWeight(.bold)
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)

            Spacer().frame(height: 12)

            Text(L.s(bodyKey))
                .font(.body)
                .foregroundStyle(.white.opacity(0.85))
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .frame(maxWidth: .infinity)

            Spacer()
        }
        .padding(.horizontal, 28)
    }

    // MARK: - Lottie View
    @ViewBuilder
    private func lottieView(_ name: String) -> some View {
        LottieView(animation: .named(name))
            .playing(loopMode: .loop)
            .animationSpeed(0.7)
            .frame(width: 260, height: 260)
    }

    // MARK: - Bottom Controls
    @ViewBuilder
    private var bottomControls: some View {
        VStack(spacing: 16) {
            // Page indicators (1:1 Android)
            HStack(spacing: 8) {
                ForEach(0..<actualSlideCount, id: \.self) { index in
                    let isActive = index == currentPage
                    Circle()
                        .fill(isActive ? Color.white : Color.white.opacity(0.4))
                        .frame(width: isActive ? 10 : 7, height: isActive ? 10 : 7)
                }
            }
            .padding(.bottom, 4)
            .animation(.easeInOut(duration: 0.2), value: currentPage)

            // Tooltips checkbox (last page only)
            if isLastPage {
                Button(action: {
                    tooltipsEnabled.toggle()
                    UserDefaults.standard.set(tooltipsEnabled, forKey: "tooltips_enabled")
                }) {
                    HStack(spacing: 8) {
                        Image(systemName: tooltipsEnabled ? "checkmark.square.fill" : "square")
                            .foregroundStyle(.white)
                            .font(.title3)
                        Text(L.s("onboarding_enable_tooltips"))
                            .foregroundStyle(.white)
                            .font(.subheadline)
                    }
                }
                .transition(.opacity)
            }

            // Main button (Next / Start)
            Button(action: {
                if isLastPage {
                    guard !isStarting else { return }
                    isStarting = true
                    onFinished(tooltipsEnabled)
                } else {
                    withAnimation { currentPage += 1 }
                }
            }) {
                Group {
                    if isStarting {
                        ProgressView()
                            .tint(.nightBlue950)
                    } else {
                        Text(isLastPage
                             ? (isLoggedIn ? L.s("close_desc") : L.onboardingDone)
                             : L.onboardingNext)
                            .fontWeight(.bold)
                    }
                }
                .foregroundStyle(Color.nightBlue950)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(Color.white)
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .disabled(isStarting)
            .opacity(isStarting ? 0.6 : 1.0)
            .padding(.horizontal, 24)
            .accessibilityIdentifier("onboarding_start_button")

            // Login / Registrieren Link (last page, not logged in)
            if isLastPage && !isLoggedIn {
                Button(L.s("onboarding_login_create")) {
                    onLoginRequested?()
                }
                .foregroundStyle(.white.opacity(0.9))
                .font(.subheadline).fontWeight(.bold)
                .transition(.opacity)
            }

            // Skip (not last page)
            if !isLastPage {
                Button(L.onboardingSkip) {
                    withAnimation { currentPage = actualSlideCount - 1 }
                }
                .foregroundStyle(.white.opacity(0.7))
                .font(.subheadline)
                .transition(.opacity)
            }
        }
        .animation(.easeInOut(duration: 0.2), value: isLastPage)
    }
}

// MARK: - Schedule Mockup (Slide 1 – on-the-fly, fully localized)

private struct ScheduleMockup: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        MockupCard {
            // Header
            Text(L.s("main_current_schedule"))
                .font(.subheadline).fontWeight(.bold)
                .foregroundStyle(theme.onBackground)

            // Optimal plan banner
            HStack(spacing: 6) {
                Text("✅")
                Text(L.s("main_optimal_plan"))
                    .font(.caption).fontWeight(.semibold)
                    .foregroundStyle(theme.primary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 12).fill(theme.surfaceVariant))

            // Member cards
            MockMemberCard(emoji: "🔔", time: "06:20", name: L.s("onboarding_mock_name1"),
                          bathroom: String(format: L.s("main_schedule_bathroom"), "06:20", "06:40"))

            MockMemberCard(emoji: "🔔", time: "06:40", name: L.s("onboarding_mock_name2"),
                          bathroom: String(format: L.s("main_schedule_bathroom"), "06:40", "07:00"))
        }
    }
}

// MARK: - Invite Mockup (Slide 2 – on-the-fly, fully localized)

private struct InviteMockup: View {
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        MockupCard(height: 190) {
            // Header
            HStack(spacing: 8) {
                Image(systemName: "person.3.fill")
                    .font(.caption).foregroundStyle(theme.primary)
                Text(L.settingsAccountTitle)
                    .font(.caption).fontWeight(.bold)
                    .foregroundStyle(theme.onBackground)
            }

            // Join code label
            Text(L.settingsJoinCodeName(L.s("onboarding_mock_family_name")))
                .font(.caption2).foregroundStyle(theme.onSurfaceVariant)

            // Code
            Text("3KY342")
                .font(.title2).fontWeight(.black)
                .foregroundStyle(theme.primary)
                .frame(maxWidth: .infinity)

            // Share button
            HStack(spacing: 6) {
                Image(systemName: "square.and.arrow.up").font(.caption2)
                Text(L.settingsShareCode).font(.caption).fontWeight(.semibold)
            }
            .foregroundStyle(theme.primary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 10)
            .background(RoundedRectangle(cornerRadius: 12).fill(theme.secondary.opacity(0.18)))
        }
    }
}

// MARK: - Mockup Shared Components

// MARK: - Mockup Shared Components

private struct MockupCard<Content: View>: View {
    var height: CGFloat = 275
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }
    @ViewBuilder var content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            content()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(height: height)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(theme.background)
                .shadow(color: .black.opacity(0.6), radius: 24, x: 0, y: 12)
        )
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(alignment: .bottom) {
            // Bottom fade
            LinearGradient(colors: [.clear, theme.background.opacity(0.85)],
                          startPoint: .top, endPoint: .bottom)
                .frame(height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 20))
        }
    }
}

private struct MockMemberCard: View {
    let emoji: String
    let time: String
    let name: String
    let bathroom: String
    
    @Environment(\.colorScheme) private var colorScheme
    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("\(emoji)  \(time) – \(name)")
                    .font(.caption).fontWeight(.bold)
                    .foregroundStyle(theme.onBackground)
                Text(bathroom)
                    .font(.caption2)
                    .foregroundStyle(theme.onSurfaceVariant)
            }
            Spacer()
            // Drag handle dots
            VStack(spacing: 3) {
                ForEach(0..<3, id: \.self) { _ in
                    HStack(spacing: 3) {
                        ForEach(0..<2, id: \.self) { _ in
                            Circle().fill(theme.outline).frame(width: 3, height: 3)
                        }
                    }
                }
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(theme.surface)
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(theme.outline.opacity(0.3), lineWidth: 1))
        )
    }
}
