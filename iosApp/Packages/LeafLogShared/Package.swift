// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LeafLogShared",
    platforms: [.iOS(.v16)],
    products: [.library(name: "LeafLogShared", targets: ["LeafLogShared"])],
    targets: [.target(name: "LeafLogShared")]
)
