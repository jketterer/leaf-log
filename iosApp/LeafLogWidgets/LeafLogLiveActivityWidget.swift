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
                DynamicIslandExpandedRegion(.leading) {
                    Image(systemName: "leaf.fill")
                        .foregroundStyle(.green)
                        .font(.title2)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(formatTime(context.state.remainingSeconds))
                        .font(.title.monospacedDigit())
                        .foregroundStyle(context.state.isPaused ? .secondary : .primary)
                        .contentTransition(.numericText(countsDown: true))
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
                DynamicIslandExpandedRegion(.bottom) {
                    ProgressBarView(
                        progress: elapsed(context),
                        isPaused: context.state.isPaused
                    )
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                }
            } compactLeading: {
                Image(systemName: "leaf.fill")
                    .foregroundStyle(.green)
                    .font(.caption2)
            } compactTrailing: {
                Text(formatTime(context.state.remainingSeconds))
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(context.state.isPaused ? .secondary : .primary)
                    .contentTransition(.numericText(countsDown: true))
            } minimal: {
                Image(systemName: context.state.isPaused ? "pause.circle.fill" : "leaf.fill")
                    .foregroundStyle(.green)
            }
        }
    }

    private func elapsed(_ ctx: ActivityViewContext<TeaTimerAttributes>) -> Double {
        guard ctx.attributes.totalSeconds > 0 else { return 0 }
        return 1.0 - (ctx.state.remainingSeconds / ctx.attributes.totalSeconds)
    }
}

// MARK: - Lock Screen View

struct LockScreenLiveActivityView: View {
    let attributes: TeaTimerAttributes
    let state: TeaTimerAttributes.ContentState

    private var progress: Double {
        guard attributes.totalSeconds > 0 else { return 0 }
        return 1.0 - (state.remainingSeconds / attributes.totalSeconds)
    }

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(lineWidth: 4)
                    .foregroundStyle(.tertiary)
                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        state.isPaused ? Color.orange : Color.green,
                        style: StrokeStyle(lineWidth: 4, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .animation(.easeInOut, value: progress)
                Image(systemName: state.isPaused ? "pause.fill" : "leaf.fill")
                    .foregroundStyle(state.isPaused ? .orange : .green)
                    .font(.caption)
            }
            .frame(width: 48, height: 48)

            VStack(alignment: .leading, spacing: 2) {
                Text(attributes.teaName)
                    .font(.headline)
                    .lineLimit(1)
                Text("Steep \(attributes.steepNumber) · \(state.isPaused ? "Paused" : "Steeping")")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Text(formatTime(state.remainingSeconds))
                .font(.title.monospacedDigit().bold())
                .foregroundStyle(state.isPaused ? .secondary : .primary)
                .contentTransition(.numericText(countsDown: true))
        }
        .padding()
        .activityBackgroundTint(Color(.systemBackground))
        .activitySystemActionForegroundColor(.primary)
    }
}

// MARK: - Progress Bar (Dynamic Island expanded bottom)

struct ProgressBarView: View {
    let progress: Double
    let isPaused: Bool

    var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .foregroundStyle(.tertiary)
                    .frame(height: 6)
                RoundedRectangle(cornerRadius: 3)
                    .foregroundStyle(isPaused ? Color.orange : Color.green)
                    .frame(width: geo.size.width * max(0, min(1, progress)), height: 6)
                    .animation(.linear(duration: 1), value: progress)
            }
        }
        .frame(height: 6)
    }
}

// MARK: - Helpers

private func formatTime(_ seconds: Double) -> String {
    let total = max(0, Int(ceil(seconds)))
    return String(format: "%d:%02d", total / 60, total % 60)
}
