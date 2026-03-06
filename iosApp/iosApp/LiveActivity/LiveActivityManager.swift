import ActivityKit
import ComposeApp
import LeafLogShared

@available(iOS 16.2, *)
final class LiveActivityManager: NSObject, LiveActivityService {
    private var activity: Activity<TeaTimerAttributes>?
    /// Automatically ends the Live Activity when the timer completes, even if the app is
    /// backgrounded or suspended. Task.sleep uses ContinuousClock which advances during
    /// process suspension, so this fires immediately when the app resumes after the timer ends.
    private var completionTask: Task<Void, Never>?

    func start(teaName: String, steepNumber: Int32, totalSeconds: Double,
               remainingSeconds: Double, sessionId: String?) {
        // End any existing activity
        completionTask?.cancel()
        completionTask = nil
        let current = activity
        Task { await current?.end(dismissalPolicy: .immediate) }
        activity = nil

        guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }

        let attributes = TeaTimerAttributes(
            teaName: teaName,
            steepNumber: Int(steepNumber),
            totalSeconds: totalSeconds,
            sessionId: sessionId
        )
        let endDate = Date().addingTimeInterval(ceil(remainingSeconds))
        let state = TeaTimerAttributes.ContentState(remainingSeconds: remainingSeconds, isPaused: false, endDate: endDate)
        let staleDate = Date().addingTimeInterval(remainingSeconds + 60)

        activity = try? Activity.request(
            attributes: attributes,
            content: .init(state: state, staleDate: staleDate),
            pushType: nil
        )

        scheduleCompletion(after: remainingSeconds)
    }

    func update(remainingSeconds: Double, isPaused: Bool) {
        let current = activity
        Task {
            let endDate = isPaused ? Date() : Date().addingTimeInterval(ceil(remainingSeconds))
            let state = TeaTimerAttributes.ContentState(remainingSeconds: remainingSeconds, isPaused: isPaused, endDate: endDate)
            let staleDate = isPaused
                ? Date().addingTimeInterval(300)
                : Date().addingTimeInterval(remainingSeconds + 10)
            await current?.update(ActivityContent(state: state, staleDate: staleDate))
        }

        if isPaused {
            completionTask?.cancel()
            completionTask = nil
        } else {
            scheduleCompletion(after: remainingSeconds)
        }
    }

    func end() {
        completionTask?.cancel()
        completionTask = nil
        let current = activity
        activity = nil
        Task { await current?.end(dismissalPolicy: .immediate) }
    }

    private func scheduleCompletion(after seconds: Double) {
        completionTask?.cancel()
        let activity = self.activity
        completionTask = Task {
            do {
                try await Task.sleep(for: .seconds(ceil(seconds)))
            } catch {
                return // Task was cancelled; do not end the activity.
            }
            await activity?.end(dismissalPolicy: .immediate)
        }
    }
}
