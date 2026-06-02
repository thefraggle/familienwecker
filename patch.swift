                let alert: AlarmPresentation.Alert
                if #available(iOS 26.1, *) {
                    alert = AlarmPresentation.Alert(
                        title: LocalizedStringResource(stringLiteral: memberName),
                        secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                        secondaryButtonBehavior: .custom
                    )
                } else {
                    alert = AlarmPresentation.Alert(
                        title: LocalizedStringResource(stringLiteral: memberName),
                        stopButton: AlarmButton(text: "Dismiss", textColor: .white, systemImageName: "stop.circle"),
                        secondaryButton: AlarmButton(text: "Snooze", textColor: .white, systemImageName: "zzz"),
                        secondaryButtonBehavior: .custom
                    )
                }
