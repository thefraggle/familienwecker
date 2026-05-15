import SwiftUI

struct LoadingView: View {
    @State private var isAnimating = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(.systemBackground), Color(.secondarySystemBackground)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 24) {
                // App Logo / Icon
                ZStack {
                    Circle()
                        .fill(Color.accentColor.opacity(0.12))
                        .frame(width: 120, height: 120)
                        .scaleEffect(isAnimating ? 1.08 : 1.0)
                        .animation(.easeInOut(duration: 1.2).repeatForever(autoreverses: true), value: isAnimating)

                    Image(systemName: "alarm.fill")
                        .font(.system(size: 52))
                        .foregroundStyle(Color.accentColor)
                }

                VStack(spacing: 6) {
                    Text("FamWake")
                        .font(.system(size: 32, weight: .black))
                    Text(L.appNameShort)
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }

                ProgressView()
                    .scaleEffect(1.2)
                    .tint(.accentColor)
            }
        }
        .onAppear { isAnimating = true }
    }
}
