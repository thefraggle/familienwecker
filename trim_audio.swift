import Foundation
import AVFoundation

let inputURL = URL(fileURLWithPath: "/Users/daniel.notthoff/Downloads/lesiakower-oversimplified-alarm-clock-113180.mp3")
let outputURL = URL(fileURLWithPath: FileManager.default.currentDirectoryPath).appendingPathComponent("ios/FamWake/Resources/alarm_default.mp3")

// Remove existing file if present
try? FileManager.default.removeItem(at: outputURL)

let asset = AVAsset(url: inputURL)
let duration = CMTime(seconds: 29.0, preferredTimescale: 600)
let timeRange = CMTimeRange(start: .zero, duration: duration)

let exportSession = AVAssetExportSession(asset: asset, presetName: AVAssetExportPresetAppleM4A)!
exportSession.outputURL = outputURL.deletingPathExtension().appendingPathExtension("m4a")
exportSession.outputFileType = .m4a
exportSession.timeRange = timeRange

let semaphore = DispatchSemaphore(value: 0)

exportSession.exportAsynchronously {
    if exportSession.status == .completed {
        print("Trimmed successfully to 29 seconds")
    } else if let error = exportSession.error {
        print("Export failed: \(error)")
    }
    semaphore.signal()
}

semaphore.wait()
