import SwiftUI
import Lottie

struct MemberSection: View {
    @EnvironmentObject var familyViewModel: FamilyViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Binding var editMemberId: String?
    @Binding var memberToDelete: FamilyMember?
    @Binding var showDeleteMemberAlert: Bool
    var onAddMember: () -> Void

    private var theme: FamWakeTheme { FamWakeTheme.current(for: colorScheme) }

    var body: some View {
        Group {
            Text(L.mainFamilyMembers)
                .font(.title2).fontWeight(.black)
                .foregroundStyle(theme.onBackground)
                .listRowInsets(EdgeInsets(top: 24, leading: 16, bottom: 4, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            // Global Buffer Stepper (Android style)
            if familyViewModel.members.count > 1 {
                HStack {
                    Image(systemName: "timer")
                        .foregroundStyle(theme.primary)
                    Text(L.bufferAfterBath)
                        .font(.subheadline)
                        .foregroundStyle(theme.onBackground)
                    Spacer()
                    
                    Button(action: {
                        familyViewModel.setGlobalBufferMinutes(max(0, familyViewModel.globalBufferMinutes - 5))
                    }) {
                        Image(systemName: "minus.circle.fill")
                            .foregroundStyle(familyViewModel.globalBufferMinutes > 0 ? theme.primary : theme.outline.opacity(0.3))
                            .font(.title2)
                    }
                    .disabled(familyViewModel.globalBufferMinutes <= 0)
                    .buttonStyle(.plain)
                    
                    Text("\(familyViewModel.globalBufferMinutes) min")
                        .font(.subheadline).fontWeight(.bold)
                        .frame(minWidth: 50, alignment: .center)
                    
                    Button(action: {
                        familyViewModel.setGlobalBufferMinutes(min(15, familyViewModel.globalBufferMinutes + 5))
                    }) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundStyle(familyViewModel.globalBufferMinutes < 15 ? theme.primary : theme.outline.opacity(0.3))
                            .font(.title2)
                    }
                    .disabled(familyViewModel.globalBufferMinutes >= 15)
                    .buttonStyle(.plain)
                }
                .padding()
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .padding(.bottom, 12)
            }

            if familyViewModel.members.count >= 6 {
                Text(L.mainMemberLimitReached)
                    .font(.footnote)
                    .foregroundStyle(theme.error)
                    .padding(.bottom, 8)
            }

            if familyViewModel.members.isEmpty {
                VStack(spacing: 24) {
                    LottieView(animation: .named("family"))
                        .playing(loopMode: .loop)
                        .animationSpeed(0.7)
                        .frame(width: 240, height: 240)
                    
                    VStack(alignment: .leading, spacing: 12) {
                        Text(L.emptyMembersTitle)
                            .font(.title3).fontWeight(.bold)
                            .foregroundStyle(Color(red: 28/255, green: 27/255, blue: 31/255))
                        
                        Text(L.emptyMembersDescription)
                            .font(.subheadline)
                            .foregroundStyle(Color(red: 50/255, green: 49/255, blue: 51/255))
                            .lineSpacing(4)
                    }
                    .padding(24)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(red: 255/255, green: 249/255, blue: 196/255))
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    .shadow(color: .black.opacity(0.15), radius: 6, x: 0, y: 3)
                    .rotationEffect(.degrees(-1.5))
                    .padding(.horizontal, 4)
                }
                .padding(.vertical, 20)
            } else {
                ForEach(familyViewModel.members) { member in
                    MemberCardView(
                        member: member,
                        isMyProfile: member.id == familyViewModel.myMemberId,
                        isAlarmEnabled: familyViewModel.isAlarmEnabled,
                        onEdit: { editMemberId = member.id },
                        onDelete: {
                            memberToDelete = member
                            showDeleteMemberAlert = true
                        },
                        onTogglePause: {
                            familyViewModel.togglePauseMember(member.id)
                        }
                    )
                    .padding(.bottom, 12)
                }
            }
        }
        .listRowBackground(Color.clear)
        .listRowSeparator(.hidden)
        .listRowInsets(EdgeInsets(top: 0, leading: 16, bottom: 0, trailing: 16))
    }
}
