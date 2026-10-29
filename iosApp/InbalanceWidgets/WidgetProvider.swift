import AppIntents
import SwiftUI
import WidgetKit
import shared

/// One moment of the widgets: today's snapshot from the shared Kotlin code, or nil when it couldn't be read.
struct InbalanceEntry: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot?
    /// Next up's saved place, "yyyy-MM-dd#index".
    let nextUpPlace: String?
}

enum Shared {
    static let group = "group.com.ideeya.inbalance"
    static let nextUpKey = "next_up"

    static var defaults: UserDefaults { UserDefaults(suiteName: group) ?? .standard }

    @MainActor
    static func load() async -> InbalanceEntry {
        let snapshot = (try? await WidgetBridge.shared.snapshot()) ?? nil
        return InbalanceEntry(date: Date(), snapshot: snapshot, nextUpPlace: defaults.string(forKey: nextUpKey))
    }

    /// The Today widget changes at 8 pm, and every widget at midnight.
    static func nextChange(after now: Date) -> Date {
        let calendar = Calendar.current
        let evening = calendar.date(bySettingHour: 20, minute: 0, second: 5, of: now) ?? now
        if now < evening { return evening }
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: now) ?? now)
        return tomorrow.addingTimeInterval(5)
    }
}

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> InbalanceEntry {
        InbalanceEntry(date: Date(), snapshot: nil, nextUpPlace: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (InbalanceEntry) -> Void) {
        Task { @MainActor in completion(await Shared.load()) }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<InbalanceEntry>) -> Void) {
        Task { @MainActor in
            let entry = await Shared.load()
            completion(Timeline(entries: [entry], policy: .after(Shared.nextChange(after: entry.date))))
        }
    }
}

// The widgets' taps. They run in this extension, against the shared database.

struct ToggleTaskIntent: AppIntent {
    static var title: LocalizedStringResource = "Tick a task"
    static var isDiscoverable = false

    @Parameter(title: "Task") var taskId: Int

    init() {}
    init(taskId: Int64) { self.taskId = Int(taskId) }

    @MainActor
    func perform() async throws -> some IntentResult {
        try await WidgetBridge.shared.toggleTask(taskId: Int64(taskId))
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct AddGlassIntent: AppIntent {
    static var title: LocalizedStringResource = "Add a glass of water"
    static var isDiscoverable = false

    @MainActor
    func perform() async throws -> some IntentResult {
        try await WidgetBridge.shared.addGlass()
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct ToggleMoodIntent: AppIntent {
    static var title: LocalizedStringResource = "Set today's mood"
    static var isDiscoverable = false

    @Parameter(title: "Mood") var icon: String

    init() {}
    init(icon: String) { self.icon = icon }

    @MainActor
    func perform() async throws -> some IntentResult {
        try await WidgetBridge.shared.toggleMood(icon: icon)
        WidgetCenter.shared.reloadAllTimelines()
        return .result()
    }
}

struct StepNextUpIntent: AppIntent {
    static var title: LocalizedStringResource = "Step through today's tasks"
    static var isDiscoverable = false

    @Parameter(title: "Step") var by: Int
    @Parameter(title: "Day") var dateKey: String
    @Parameter(title: "Count") var count: Int

    init() {}
    init(by: Int, dateKey: String, count: Int) {
        self.by = by
        self.dateKey = dateKey
        self.count = count
    }

    func perform() async throws -> some IntentResult {
        let saved = Shared.defaults.string(forKey: Shared.nextUpKey)
        let next = WidgetBridge.shared.nextUpStep(saved: saved, dateKey: dateKey, count: Int32(count), by: Int32(by))
        Shared.defaults.set(next, forKey: Shared.nextUpKey)
        WidgetCenter.shared.reloadTimelines(ofKind: "NextUp")
        return .result()
    }
}
