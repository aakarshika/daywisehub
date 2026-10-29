import AppIntents
import SwiftUI
import WidgetKit
import shared

@main
struct InbalanceWidgets: WidgetBundle {
    var body: some Widget {
        TodayWidget()
        WaterMoodWidget()
        NextUpWidget()
        StickersWidget()
        WeekAheadWidget()
        DiaryWidget()
        MonthWidget()
        LockWidget()
    }
}

// The look of docs/widgets/mockups.html.

extension Color {
    init(hex: UInt32) {
        self.init(red: Double((hex >> 16) & 0xFF) / 255, green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255)
    }

    static let paper = Color(hex: 0xFFF8E6)
    static let restPaper = Color(hex: 0xF1EFE8)
    static let ink = Color(hex: 0x2A2A2A)
    static let dark = Color(hex: 0x3B3226)
    static let gray = Color(hex: 0x6E6E6E)
    static let faint = Color(hex: 0xB4B2A9)
    static let rule = Color(hex: 0xEDE6D6)
    static let blue = Color(hex: 0x2F5677)
    static let peach = Color(hex: 0xF9B98F)
    static let sticky = Color(hex: 0xFDF0C6)
    static let doneGreen = Color(hex: 0xA6CC76)
    static let barYellow = Color(hex: 0xFAE0A0)
    static let restBar = Color(hex: 0xD3D1C7)
    static let todayOutline = Color(hex: 0x7A6E94)

    /// AppColors.pillarInk.
    static func pillarInk(_ pillar: PillarType?) -> Color {
        guard let pillar else { return .gray }
        switch pillar {
        case .health: return Color(hex: 0x6B3D7A)
        case .wealth: return Color(hex: 0x7A4A2C)
        case .love: return Color(hex: 0x7A3341)
        case .life: return Color(hex: 0x2F5677)
        default: return .gray
        }
    }
}

/// The app's art, drawn by the shared Kotlin code into PNGs at 3 pixels per point.
struct Art: View {
    let data: Data
    let width: CGFloat
    var height: CGFloat? = nil

    var body: some View {
        if let image = UIImage(data: data) {
            artImage(Image(uiImage: image).resizable())
                .frame(width: width, height: height ?? width)
        } else {
            Color.clear.frame(width: width, height: height ?? width)
        }
    }

    /// In the tinted look the friends and stickers take the tint, so they stay legible.
    @ViewBuilder
    private func artImage(_ image: Image) -> some View {
        if #available(iOS 18.0, *) {
            image.widgetAccentedRenderingMode(.accentedDesaturated)
        } else {
            image
        }
    }
}

struct Kicker: View {
    let text: String
    var body: some View {
        Text(text).font(.system(size: 11)).kerning(0.6).foregroundStyle(Color.gray).lineLimit(1)
    }
}

struct Pill: View {
    let text: String
    var filled = false
    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundStyle(Color.ink)
            .padding(.horizontal, 10).padding(.vertical, 3)
            .background(RoundedRectangle(cornerRadius: 10).fill(filled ? Color.peach : Color.clear))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.dark, lineWidth: 1.5))
            .widgetAccentable()
    }
}

struct MoodButton: View {
    let mood: WidgetMood
    let size: CGFloat
    var body: some View {
        Button(intent: ToggleMoodIntent(icon: mood.icon)) {
            Art(data: WidgetBridge.shared.mood(mood: mood, size: Float(size)), width: size)
        }
        .buttonStyle(.plain)
    }
}

/// When the snapshot couldn't be read.
struct OpenOnce: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Kicker(text: "INBALANCE")
            Spacer()
            Text("Open Inbalance").font(.system(size: 13, weight: .semibold)).foregroundStyle(Color.ink)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }
}

extension View {
    func card(_ color: Color = .paper) -> some View {
        containerBackground(for: .widget) { color }
    }
}

func dayURL(_ dateKey: String?) -> URL {
    URL(string: "inbalance://day/\(dateKey ?? "today")")!
}

let metricsURL = URL(string: "inbalance://metrics")!

func homeWidget<V: View>(kind: String, name: String, description: String, families: [WidgetFamily], @ViewBuilder content: @escaping (InbalanceEntry) -> V) -> some WidgetConfiguration {
    StaticConfiguration(kind: kind, provider: Provider()) { entry in
        content(entry)
    }
    .configurationDisplayName(name)
    .description(description)
    .supportedFamilies(families)
}

// 1. Today

struct TodayWidget: Widget {
    static let kind = "Today"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Today", description: "Your week's balance. Switches to all done, rest, evening mood and plan-my-day by itself.", families: [.systemSmall]) { entry in
            TodayView(entry: entry)
        }
    }
}

struct TodayView: View {
    let entry: InbalanceEntry
    var body: some View {
        if let s = entry.snapshot {
            content(s).widgetURL(dayURL(nil))
        } else {
            OpenOnce().card()
        }
    }

    @ViewBuilder
    private func content(_ s: WidgetSnapshot) -> some View {
        switch s.mode {
        case .allDone:
            VStack(spacing: 4) {
                Kicker(text: "TODAY").frame(maxWidth: .infinity, alignment: .leading)
                Spacer()
                Art(data: WidgetBridge.shared.cheer(), width: 120, height: 46)
                Spacer()
                Text("All done. \(s.verdict)").font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.ink).widgetAccentable()
            }.card()
        case .rest:
            VStack(spacing: 4) {
                Kicker(text: "RECHARGING DAY").frame(maxWidth: .infinity, alignment: .leading)
                Spacer()
                Art(data: WidgetBridge.shared.rest(size: 48), width: 48)
                Spacer()
                Text("Nothing due. Rest counts").font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.ink).multilineTextAlignment(.center)
            }.card(.restPaper)
        case .evening:
            VStack(alignment: .leading, spacing: 6) {
                Kicker(text: s.timeLabel)
                Spacer()
                Text("How was today?").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color.ink).widgetAccentable()
                HStack {
                    ForEach(Array(s.moods.prefix(3)), id: \.icon) { mood in
                        MoodButton(mood: mood, size: 34)
                        if mood.icon != s.moods.prefix(3).last?.icon { Spacer() }
                    }
                }
                Spacer()
                Text("\(s.doneCount) of \(s.totalCount) done").font(.system(size: 11)).foregroundStyle(Color.gray)
            }.card()
        case .empty:
            VStack(alignment: .leading, spacing: 6) {
                Kicker(text: "TODAY")
                Spacer()
                Text("Nothing planned yet").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color.ink)
                Pill(text: "Plan my day")
                Spacer()
                Text("Opens Inbalance").font(.system(size: 11)).foregroundStyle(Color.gray)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .card()
        default:
            VStack(spacing: 4) {
                Kicker(text: "THIS WEEK").frame(maxWidth: .infinity, alignment: .leading)
                Spacer()
                Art(data: WidgetBridge.shared.seeSaw(snapshot: s), width: 126, height: 64)
                Spacer()
                Text(s.verdict).font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.pillarInk(s.verdictPillar))
                    .frame(maxWidth: .infinity, alignment: .leading).widgetAccentable()
            }.card()
        }
    }
}

// 2. Water and mood

struct WaterMoodWidget: Widget {
    static let kind = "WaterMood"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Water and mood", description: "Add a glass of water and tap today's mood.", families: [.systemSmall]) { entry in
            if let s = entry.snapshot {
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Kicker(text: "WATER")
                        Spacer()
                        Text("\(s.water) / \(s.waterGoal)").font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.ink)
                    }
                    Spacer(minLength: 0)
                    HStack(spacing: 1) {
                        ForEach(0..<Int(s.waterGoal), id: \.self) { i in
                            Art(data: WidgetBridge.shared.glass(index: Int32(i), filled: i < Int(s.water), size: 15), width: 15)
                        }
                    }
                    Spacer(minLength: 0)
                    Button(intent: AddGlassIntent()) { Pill(text: "+ glass") }.buttonStyle(.plain)
                    Spacer(minLength: 0)
                    HStack {
                        ForEach(s.moods, id: \.icon) { mood in
                            MoodButton(mood: mood, size: 22)
                            if mood.icon != s.moods.last?.icon { Spacer(minLength: 0) }
                        }
                    }
                }
                .card()
                .widgetURL(dayURL(nil))
            } else {
                OpenOnce().card()
            }
        }
    }
}

// 3. Next up

struct NextUpWidget: Widget {
    static let kind = "NextUp"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Next up", description: "One open task with a Done button. Arrows step through today's list.", families: [.systemSmall]) { entry in
            if let s = entry.snapshot {
                NextUpView(s: s, place: entry.nextUpPlace).card().widgetURL(dayURL(s.dateKey))
            } else {
                OpenOnce().card()
            }
        }
    }
}

struct NextUpView: View {
    let s: WidgetSnapshot
    let place: String?

    var body: some View {
        let open = s.openTasks
        let index = Int(WidgetBridge.shared.nextUpIndex(saved: place, dateKey: s.dateKey, count: Int32(open.count)))
        VStack(alignment: .leading, spacing: 6) {
            if open.isEmpty {
                Kicker(text: "NEXT UP")
                Spacer()
                Text(s.totalCount == 0 ? "Nothing planned yet" : "All clear for today").font(.system(size: 14, weight: .semibold)).foregroundStyle(Color.ink)
                Spacer()
                Text("Opens Inbalance").font(.system(size: 11)).foregroundStyle(Color.gray)
            } else {
                let task = open[min(index, open.count - 1)]
                let share = s.pillars.first(where: { $0.pillar == task.pillar })?.share ?? 0.5
                Kicker(text: "NEXT UP · \(index + 1) OF \(open.count)")
                Spacer(minLength: 0)
                HStack(spacing: 6) {
                    Art(data: WidgetBridge.shared.taskFriend(pillar: task.pillar, share: share, size: 24), width: 24)
                    Text(task.title).font(.system(size: 14, weight: .semibold)).foregroundStyle(Color.ink).lineLimit(2)
                }
                Spacer(minLength: 0)
                Button(intent: ToggleTaskIntent(taskId: task.taskId)) { Pill(text: "Done", filled: true) }.buttonStyle(.plain)
                Spacer(minLength: 0)
                HStack {
                    arrow("‹", by: -1, count: open.count)
                    Spacer()
                    Text(task.timeLabel).font(.system(size: 11)).foregroundStyle(Color.gray)
                    Spacer()
                    arrow("›", by: 1, count: open.count)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func arrow(_ text: String, by: Int, count: Int) -> some View {
        Button(intent: StepNextUpIntent(by: by, dateKey: s.dateKey, count: count)) {
            Text(text).font(.system(size: 14, weight: .semibold)).foregroundStyle(Color.ink)
                .frame(width: 26, height: 26)
                .overlay(Circle().stroke(Color.dark, lineWidth: 1.5))
        }
        .buttonStyle(.plain)
    }
}

// 4. This week's stickers

struct StickersWidget: Widget {
    static let kind = "Stickers"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "This week's stickers", description: "This week's day stickers and your run.", families: [.systemSmall]) { entry in
            if let s = entry.snapshot {
                VStack(alignment: .leading, spacing: 6) {
                    Kicker(text: "THIS WEEK'S STICKERS")
                    Spacer(minLength: 0)
                    ForEach([Array(s.weekStickers.prefix(4)), Array(s.weekStickers.dropFirst(4))], id: \.first?.dateKey) { row in
                        HStack(spacing: 5) {
                            ForEach(row, id: \.dateKey) { sticker in
                                if let badge = sticker.badge {
                                    Art(data: WidgetBridge.shared.sticker(badge: badge, dateKey: sticker.dateKey, size: 26), width: 26)
                                } else {
                                    Art(data: WidgetBridge.shared.emptySlot(size: 26), width: 26)
                                }
                            }
                        }
                    }
                    Spacer(minLength: 0)
                    Text(s.streakLine).font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.ink).widgetAccentable()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .card()
                .widgetURL(metricsURL)
            } else {
                OpenOnce().card()
            }
        }
    }
}

// 5. Week ahead

struct WeekAheadWidget: Widget {
    static let kind = "WeekAhead"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Week ahead", description: "The next seven days: big, slow or rest, and the light day.", families: [.systemMedium]) { entry in
            if let s = entry.snapshot {
                WeekAheadView(s: s).card().widgetURL(dayURL(s.lightDateKey))
            } else {
                OpenOnce().card()
            }
        }
    }
}

struct WeekAheadView: View {
    let s: WidgetSnapshot
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Kicker(text: "WEEK AHEAD")
                Spacer()
                Text("Big, slow or rest").font(.system(size: 11)).foregroundStyle(Color.gray)
            }
            HStack(alignment: .bottom, spacing: 4) {
                ForEach(s.weekAhead, id: \.dateKey) { day in
                    Link(destination: dayURL(day.dateKey)) {
                        VStack(spacing: 3) {
                            Spacer(minLength: 0)
                            RoundedRectangle(cornerRadius: 4)
                                .fill(color(day))
                                .frame(width: 16, height: height(day.dayType))
                                .overlay(day.light ? RoundedRectangle(cornerRadius: 4).stroke(Color.blue, style: StrokeStyle(lineWidth: 1.5, dash: [3, 2])).padding(-2) : nil)
                                .widgetAccentable(day.isToday)
                            Text(day.dayType.label).font(.system(size: 11)).foregroundStyle(Color.gray)
                            Text(day.letter).font(.system(size: 11, weight: .semibold)).foregroundStyle(day.light ? Color.blue : Color.ink)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 2)
                        .background(day.isToday ? RoundedRectangle(cornerRadius: 8).fill(Color.sticky) : nil)
                    }
                }
            }
            .frame(maxHeight: .infinity)
            HStack(spacing: 4) {
                Text(s.lightLine ?? "No light day ahead.").font(.system(size: 12)).foregroundStyle(Color.ink)
                if let suggestion = s.suggestion {
                    Text(suggestion).font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.blue).widgetAccentable()
                }
            }
            .lineLimit(1)
        }
    }

    private func height(_ type: DayType) -> CGFloat {
        switch type {
        case .big: return 34
        case .slow: return 20
        default: return 8
        }
    }

    private func color(_ day: WidgetDay) -> Color {
        if day.dayType == .rest { return .restBar }
        return day.done ? .doneGreen : .barYellow
    }
}

// 6. Mini diary

struct DiaryWidget: Widget {
    static let kind = "Diary"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Mini diary", description: "Today's page: pillars, tasks you can tick, water and mood.", families: [.systemLarge]) { entry in
            if let s = entry.snapshot {
                DiaryView(s: s).card().widgetURL(dayURL(s.dateKey))
            } else {
                OpenOnce().card()
            }
        }
    }
}

struct DiaryView: View {
    let s: WidgetSnapshot
    var body: some View {
        let tasks = s.tasks
        let shown = tasks.count > 5 ? Array(tasks.prefix(4)) : tasks
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text(s.dateTitle).font(.system(size: 15, weight: .semibold)).foregroundStyle(Color.ink).widgetAccentable()
                Spacer()
                Kicker(text: "\(s.dayType.label.uppercased()) DAY")
            }
            HStack(spacing: 6) {
                ForEach(s.pillars, id: \.pillar.name) { pillar in
                    HStack(spacing: 2) {
                        Art(data: WidgetBridge.shared.friend(pillar: pillar, size: 26), width: 26)
                        Text(pillar.pillar.title).font(.system(size: 11)).foregroundStyle(Color.ink).lineLimit(1)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            .padding(.vertical, 6)
            if tasks.isEmpty {
                Text("Nothing planned yet. Tap to plan your day.").font(.system(size: 12)).foregroundStyle(Color.gray)
            }
            ForEach(shown, id: \.taskId) { task in
                HStack(spacing: 7) {
                    Button(intent: ToggleTaskIntent(taskId: task.taskId)) {
                        RoundedRectangle(cornerRadius: 4)
                            .fill(task.done ? Color.peach : Color.clear)
                            .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.dark, lineWidth: 1.5))
                            .frame(width: 15, height: 15)
                            .frame(width: 26, height: 26)
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    Link(destination: dayURL(s.dateKey)) {
                        HStack {
                            Text(task.title)
                                .font(.system(size: 12))
                                .strikethrough(task.done && !task.partial)
                                .foregroundStyle(task.done && !task.partial ? Color.gray : Color.ink)
                                .lineLimit(1)
                            Spacer()
                            if task.partial {
                                Text("\(task.progress)%").font(.system(size: 12)).foregroundStyle(Color.pillarInk(task.pillar))
                            }
                        }
                    }
                }
                .padding(.vertical, 1)
                Rectangle().fill(Color.rule).frame(height: 0.5)
            }
            if tasks.count > shown.count {
                Text("+ \(tasks.count - shown.count) more").font(.system(size: 12)).foregroundStyle(Color.gray).padding(.leading, 33).padding(.top, 4)
            }
            Spacer(minLength: 0)
            Rectangle().fill(Color.rule).frame(height: 0.5)
            HStack {
                Text("Water \(s.water)/\(s.waterGoal)").font(.system(size: 12)).foregroundStyle(Color.ink)
                Button(intent: AddGlassIntent()) { Pill(text: "+") }.buttonStyle(.plain)
                Spacer()
                ForEach(Array(s.moods.prefix(3)), id: \.icon) { mood in
                    MoodButton(mood: mood, size: 24)
                }
            }
            .padding(.top, 8)
        }
    }
}

// 7. Month of stickers

struct MonthWidget: Widget {
    static let kind = "Month"
    var body: some WidgetConfiguration {
        homeWidget(kind: Self.kind, name: "Month of stickers", description: "The month's stickers and crowns, as on Metrics.", families: [.systemLarge]) { entry in
            if let s = entry.snapshot {
                MonthView(month: s.month).card().widgetURL(metricsURL)
            } else {
                OpenOnce().card()
            }
        }
    }
}

struct MonthView: View {
    let month: WidgetMonth
    var body: some View {
        let crown = WidgetBridge.shared.crown(size: 14)
        VStack(spacing: 2) {
            HStack(alignment: .firstTextBaseline) {
                Text(month.title).font(.system(size: 15, weight: .semibold)).foregroundStyle(Color.ink).widgetAccentable()
                Spacer()
                Kicker(text: month.crowns == 1 ? "1 CROWN" : "\(month.crowns) CROWNS")
            }
            .padding(.bottom, 4)
            HStack(spacing: 0) {
                Color.clear.frame(width: 16, height: 1)
                ForEach(Array(["M", "T", "W", "T", "F", "S", "S"].enumerated()), id: \.offset) { _, letter in
                    Text(letter).font(.system(size: 11)).foregroundStyle(Color.gray).frame(maxWidth: .infinity)
                }
            }
            ForEach(month.weeks, id: \.days.first?.dateKey) { week in
                HStack(spacing: 0) {
                    Group {
                        if week.crowned { Art(data: crown, width: 14, height: 10.5) } else { Color.clear }
                    }
                    .frame(width: 16)
                    ForEach(week.days, id: \.dateKey) { day in
                        VStack(spacing: 1) {
                            Text("\(day.day)").font(.system(size: 11, weight: day.isToday ? .bold : .regular))
                                .foregroundStyle(day.inMonth ? Color.ink : Color.faint)
                            if let badge = day.badge, day.inMonth {
                                Art(data: WidgetBridge.shared.sticker(badge: badge, dateKey: day.dateKey, size: 17), width: 17)
                            } else {
                                Color.clear.frame(width: 17, height: 17)
                            }
                        }
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .overlay(day.isToday ? RoundedRectangle(cornerRadius: 6).stroke(Color.todayOutline, lineWidth: 1.5) : nil)
                    }
                }
            }
        }
    }
}

// 8. Lock Screen, single colour

struct LockWidget: Widget {
    static let kind = "Lock"
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: Self.kind, provider: Provider()) { entry in
            LockView(entry: entry)
        }
        .configurationDisplayName("Inbalance")
        .description("Your balance and today's tasks at a glance.")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

struct LockView: View {
    @Environment(\.widgetFamily) private var family
    let entry: InbalanceEntry

    var body: some View {
        Group {
            if let s = entry.snapshot {
                switch family {
                case .accessoryCircular: ring(s)
                case .accessoryRectangular: rectangle(s)
                default: Text("\(s.shortDate) · \(s.doneCount) of \(s.totalCount) done · water \(s.water)/\(s.waterGoal)")
                }
            } else {
                Text("Open Inbalance")
            }
        }
        .containerBackground(for: .widget) { Color.clear }
        .widgetURL(dayURL(nil))
    }

    /// Four arcs, one per pillar, each filled by its share of the week; done/total in the middle.
    private func ring(_ s: WidgetSnapshot) -> some View {
        ZStack {
            AccessoryWidgetBackground()
            ForEach(Array(s.pillars.enumerated()), id: \.offset) { i, pillar in
                let start = Double(i) * 0.25 + 0.011
                let length = 0.228
                Circle().trim(from: start, to: start + length)
                    .stroke(style: StrokeStyle(lineWidth: 4.5, lineCap: .round)).opacity(0.3)
                Circle().trim(from: start, to: start + length * Double(min(pillar.share, 1)))
                    .stroke(style: StrokeStyle(lineWidth: 4.5, lineCap: .round))
            }
            .rotationEffect(.degrees(-90))
            .padding(4)
            Text("\(s.doneCount)/\(s.totalCount)").font(.system(size: 13, weight: .semibold))
        }
    }

    private func rectangle(_ s: WidgetSnapshot) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(s.verdict).font(.headline).widgetAccentable()
            Text("\(s.doneCount) of \(s.totalCount) done · \(s.water)/\(s.waterGoal) water").font(.caption)
            if let next = s.openTasks.first {
                Text("Next: \(next.title)").font(.caption)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}
