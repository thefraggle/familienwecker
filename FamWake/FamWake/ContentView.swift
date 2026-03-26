//
//  ContentView.swift
//  FamWake
//
//  Created by Daniel.Notthoff on 26.03.26.
//

import shared
import SwiftUI

struct ContentView: View {
    @State private var isOnline: Bool = true
    let monitor = NetworkMonitorKt.createNetworkMonitor(context: nil)

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: isOnline ? "wifi" : "wifi.slash")
                .font(.system(size: 80))
                .foregroundColor(isOnline ? .green : .red)
            
            Text(isOnline ? "FamWake ist Online!" : "FamWake ist Offline!")
                .font(.largeTitle)
                .bold()

            Text("KMP Modul erfolgreich verbunden 🚀")
                .foregroundColor(.secondary)
            
            Button("Status aktualisieren") {
                isOnline = monitor.isOnline.value as? Bool ?? false
            }
            .padding()
            .buttonStyle(.borderedProminent)
            .tint(isOnline ? .green : .blue)
        }
        .padding()
        .onAppear {
            monitor.startMonitoring()
            // Kurze Verzögerung für initiale Prüfung des NWPathMonitor
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                isOnline = monitor.isOnline.value as? Bool ?? false
            }
        }
    }
}

#Preview {
    ContentView()
}
