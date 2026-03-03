import ActivityKit

public struct TeaTimerAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var remainingSeconds: Double
        public var isPaused: Bool
        /// The wall-clock time when the timer reaches zero (only meaningful when not paused).
        /// Used by the widget to drive a native iOS countdown text that updates automatically
        /// without requiring per-second pushes from the app.
        public var endDate: Date

        public init(remainingSeconds: Double, isPaused: Bool, endDate: Date) {
            self.remainingSeconds = remainingSeconds
            self.isPaused = isPaused
            self.endDate = endDate
        }
    }

    public var teaName: String
    public var steepNumber: Int
    public var totalSeconds: Double
    public var sessionId: String?

    public init(teaName: String, steepNumber: Int, totalSeconds: Double, sessionId: String?) {
        self.teaName = teaName
        self.steepNumber = steepNumber
        self.totalSeconds = totalSeconds
        self.sessionId = sessionId
    }
}
