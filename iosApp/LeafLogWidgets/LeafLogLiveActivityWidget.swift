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
            let isComplete = context.state.remainingSeconds <= 0
                || (!context.state.isPaused && context.state.endDate <= Date.now)
            return DynamicIsland {
                DynamicIslandExpandedRegion(.trailing) {
                    if isComplete {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.title)
                            .foregroundStyle(.green)
                    } else if context.state.isPaused {
                        Text(formatTime(context.state.remainingSeconds))
                            .font(.title.monospacedDigit())
                            .foregroundStyle(.secondary)
                    } else {
                        Text(
                            timerInterval: countdownRange(context),
                            countsDown: true,
                            showsHours: false
                        )
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
                Image(systemName: isComplete ? "checkmark.circle.fill" : "leaf.fill")
                    .foregroundStyle(.green)
                    .font(.caption2)
            } compactTrailing: {
                if isComplete {
                    Text("Done")
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.green)
                } else if context.state.isPaused {
                    Text(formatTime(context.state.remainingSeconds))
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.secondary)
                } else {
                    Text(
                        timerInterval: countdownRange(context),
                        countsDown: true,
                        showsHours: false
                    )
                        .font(.caption.monospacedDigit())
                        .foregroundStyle(.primary)
                        .contentTransition(.numericText(countsDown: true))
                }
            } minimal: {
                Image(systemName: isComplete ? "checkmark.circle.fill"
                    : context.state.isPaused ? "pause.circle.fill" : "leaf.fill")
                    .foregroundStyle(.green)
            }
        }
    }
}

// MARK: - Lock Screen View

struct LockScreenLiveActivityView: View {
    let attributes: TeaTimerAttributes
    let state: TeaTimerAttributes.ContentState

    private var isComplete: Bool {
        state.remainingSeconds <= 0 || (!state.isPaused && state.endDate <= Date.now)
    }

    private var statusText: String {
        if isComplete {
            return "Complete"
        } else if state.isPaused {
            return "Paused"
        } else {
            return "Steeping"
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(attributes.teaName)
                    .font(.headline)
                    .lineLimit(1)
                Text("Steep \(attributes.steepNumber) · \(statusText)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            if isComplete {
                Image(systemName: "checkmark.circle.fill")
                    .font(.title)
                    .foregroundStyle(.green)
            } else if state.isPaused {
                Text(formatTime(state.remainingSeconds))
                    .font(.title2.monospacedDigit().bold())
                    .foregroundStyle(.secondary)
                    .frame(minWidth: 56, alignment: .trailing)
            } else {
                Text(
                    timerInterval: countdownRange(endDate: state.endDate,
                                                  totalSeconds: attributes.totalSeconds),
                    countsDown: true,
                    showsHours: false
                )
                    .font(.title2.monospacedDigit().bold())
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.trailing)
                    .frame(minWidth: 56, alignment: .trailing)
            }
        }
        .padding(.leading, 24)
        .padding(.trailing, 20)
        .padding(.vertical, 14)
        .activityBackgroundTint(.black)
        .activitySystemActionForegroundColor(.white)
    }
}

// MARK: - Helpers

private func formatTime(_ seconds: Double) -> String {
    let total = max(0, Int(ceil(seconds)))
    return String(format: "%d:%02d", total / 60, total % 60)
}

/// Bounds the countdown to the brew so it holds at 0:00 once the end date passes.
/// `Text(_:style:.timer)` counts upward past its date instead of stopping, and the
/// activity's body is not guaranteed to re-render at the moment the timer elapses,
/// so a render-time completion check cannot be relied on to hide it.
private func countdownRange(endDate: Date, totalSeconds: Double) -> ClosedRange<Date> {
    endDate.addingTimeInterval(-max(totalSeconds, 1))...endDate
}

private func countdownRange(_ context: ActivityViewContext<TeaTimerAttributes>) -> ClosedRange<Date> {
    countdownRange(endDate: context.state.endDate,
                   totalSeconds: context.attributes.totalSeconds)
}
