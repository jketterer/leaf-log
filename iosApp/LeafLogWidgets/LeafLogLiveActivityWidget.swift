import WidgetKit
import SwiftUI
import ActivityKit
import LeafLogShared

struct LeafLogLiveActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TeaTimerAttributes.self) { context in
            LockScreenLiveActivityView(
                attributes: context.attributes,
                state: context.state
            )
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.trailing) {
                    if context.state.isPaused {
                        Text(formatTime(context.state.remainingSeconds))
                            .font(.title.monospacedDigit())
                            .foregroundStyle(.secondary)
                    } else {
                        Text(context.state.endDate, style: .timer)
                            .font(.title.monospacedDigit())
                            .foregroundStyle(.primary)
                            .contentTransition(.numericText(countsDown: true))
                    }
                }
                DynamicIslandExpandedRegion(.center) {
                    VStack(spacing: 2) {
                        Text(context.attributes.teaName)
                            .font(.headline)
                            .lineLimit(1)
                        Text("Steep \(context.attributes.steepNumber)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            } compactLeading: {
                Image(systemName: "leaf.fill")
                    .foregroundStyle(.green)
                    .font(.caption2)
            } compactTrailing: {
                if context.state.isPaused {
                    Text(formatTime(context.state.remainingSeconds))
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)
                } else {
                    Text(context.state.endDate, style: .timer)
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.primary)
                        .contentTransition(.numericText(countsDown: true))
                }
            } minimal: {
                Image(systemName: context.state.isPaused ? "pause.circle.fill" : "leaf.fill")
                    .foregroundStyle(.green)
            }
        }
    }
}

// MARK: - Lock Screen View

struct LockScreenLiveActivityView: View {
    let attributes: TeaTimerAttributes
    let state: TeaTimerAttributes.ContentState

    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 2) {
                Text(attributes.teaName)
                    .font(.headline)
                    .lineLimit(1)
                Text("Steep \(attributes.steepNumber) · \(state.isPaused ? "Paused" : "Steeping")")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if state.isPaused {
                Text(formatTime(state.remainingSeconds))
                    .font(.title.monospacedDigit().bold())
                    .foregroundStyle(.secondary)
            } else {
                Text(state.endDate, style: .timer)
                    .font(.title.monospacedDigit().bold())
                    .foregroundStyle(.primary)
                    .contentTransition(.numericText(countsDown: true))
            }
        }
        .padding()
        .activityBackgroundTint(Color(.systemBackground))
        .activitySystemActionForegroundColor(.primary)
    }
}

// MARK: - Helpers

private func formatTime(_ seconds: Double) -> String {
    let total = max(0, Int(ceil(seconds)))
    return String(format: "%d:%02d", total / 60, total % 60)
}
