import SwiftUI
import WidgetKit
import shared

@main
struct iOSApp: App {
	@Environment(\.scenePhase) private var scenePhase

	init() {
		// The app's own changes reload the widgets.
		WidgetBridge.shared.onChange { WidgetCenter.shared.reloadAllTimelines() }
	}

	var body: some Scene {
		WindowGroup {
			ContentView()
				// A widget tap: inbalance://day/yyyy-MM-dd or inbalance://metrics.
				.onOpenURL { url in WidgetBridge.shared.open(url: url.absoluteString) }
		}
		.onChange(of: scenePhase) { phase in
			guard phase == .active else { return }
			// The widgets write from their own process; this shows what they changed, and refreshes them.
			Task { @MainActor in
				try? await WidgetBridge.shared.touchToday()
				WidgetCenter.shared.reloadAllTimelines()
			}
		}
	}
}
