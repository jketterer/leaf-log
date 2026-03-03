import ActivityKit
import ComposeApp
import LeafLogShared

@available(iOS 16.2, *)
final class LiveActivityManager: NSObject, LiveActivityService {
    private var activity: Activity<TeaTimerAttributes>?

    func start(teaName: String, steepNumber: Int32, totalSeconds: Double,
               remainingSeconds: Double, sessionId: String?) {
        // End any existing activity
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
        let endDate = Date().addingTimeInterval(remainingSeconds)
        let state = TeaTimerAttributes.ContentState(remainingSeconds: remainingSeconds, isPaused: false, endDate: endDate)
        let staleDate = Date().addingTimeInterval(remainingSeconds + 60)

        activity = try? Activity.request(
            attributes: attributes,
            content: .init(state: state, staleDate: staleDate),
            pushType: nil
        )
    }

    func update(remainingSeconds: Double, isPaused: Bool) {
        let current = activity
        Task {
            let endDate = isPaused ? Date() : Date().addingTimeInterval(remainingSeconds)
            let state = TeaTimerAttributes.ContentState(remainingSeconds: remainingSeconds, isPaused: isPaused, endDate: endDate)
            let staleDate = isPaused
                ? Date().addingTimeInterval(300)
                : Date().addingTimeInterval(remainingSeconds + 10)
            await current?.update(ActivityContent(state: state, staleDate: staleDate))
        }
    }

    func end() {
        let current = activity
        activity = nil
        Task { await current?.end(dismissalPolicy: .immediate) }
    }
}
