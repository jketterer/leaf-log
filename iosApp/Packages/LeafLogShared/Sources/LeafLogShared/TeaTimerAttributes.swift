import ActivityKit

public struct TeaTimerAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var remainingSeconds: Double
        public var isPaused: Bool

        public init(remainingSeconds: Double, isPaused: Bool) {
            self.remainingSeconds = remainingSeconds
            self.isPaused = isPaused
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
