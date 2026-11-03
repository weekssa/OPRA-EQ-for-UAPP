import CryptoKit
import Foundation
import IOKit.hid

let vendorID = 0x2972
let productID = 0x0102
let reportID: UInt32 = 0x02
let windowSeconds: TimeInterval = 0.700

struct Request {
    let name: String
    let sequence: UInt16
    let command: UInt8
    let frame: [UInt8]
}

let requests = [
    Request(name: "volume-control", sequence: 0x0001, command: 0x02,
            frame: [0x02, 0xBB, 0x0B, 0x00, 0x01, 0x02, 0x00, 0x3A, 0xEE]),
    Request(name: "j034-replicate-zero-crc-seq-0000", sequence: 0x0000, command: 0x11,
            frame: [0x02, 0xBB, 0x0B, 0x00, 0x00, 0x11, 0x00, 0x00, 0xEE]),
    Request(name: "computed-crc-seq-0000", sequence: 0x0000, command: 0x11,
            frame: [0x02, 0xBB, 0x0B, 0x00, 0x00, 0x11, 0x00, 0x28, 0xEE]),
    Request(name: "computed-crc-seq-0002", sequence: 0x0002, command: 0x11,
            frame: [0x02, 0xBB, 0x0B, 0x00, 0x02, 0x11, 0x00, 0x67, 0xEE]),
]

struct CapturedReport {
    let elapsedMs: Double
    let reportID: UInt32
    let bytes: [UInt8]
}

final class CaptureState {
    var accepting = false
    var reports: [CapturedReport] = []
    var sentAt: TimeInterval = 0
}

func hex(_ bytes: [UInt8]) -> String {
    bytes.map { String(format: "%02X", $0) }.joined(separator: " ")
}

func sha256(_ text: String) -> String {
    SHA256.hash(data: Data(text.utf8)).map { String(format: "%02x", $0) }.joined()
}

func crc8Maxim(_ bytes: [UInt8]) -> UInt8 {
    var value: UInt8 = 0
    for byte in bytes {
        value ^= byte
        for _ in 0..<8 {
            value = (value & 1) != 0 ? (value >> 1) ^ 0x8C : value >> 1
        }
    }
    return value
}

func reportCallback(
    context: UnsafeMutableRawPointer?,
    result: IOReturn,
    sender: UnsafeMutableRawPointer?,
    type: IOHIDReportType,
    reportID: UInt32,
    report: UnsafeMutablePointer<UInt8>,
    reportLength: CFIndex
) {
    guard let context, result == kIOReturnSuccess, type == kIOHIDReportTypeInput else { return }
    let state = Unmanaged<CaptureState>.fromOpaque(context).takeUnretainedValue()
    guard state.accepting, reportID == 0x02 else { return }
    var bytes = Array(UnsafeBufferPointer(start: report, count: Int(reportLength)))
    if reportID != 0 && (bytes.first ?? 0) != UInt8(truncatingIfNeeded: reportID) {
        bytes.insert(UInt8(truncatingIfNeeded: reportID), at: 0)
    }
    state.reports.append(
        CapturedReport(
            elapsedMs: (ProcessInfo.processInfo.systemUptime - state.sentAt) * 1000.0,
            reportID: reportID,
            bytes: bytes
        )
    )
}

func writeJSONLine(_ object: [String: Any], to handle: FileHandle) {
    let data = try! JSONSerialization.data(withJSONObject: object, options: [.sortedKeys])
    handle.write(data)
    handle.write(Data([0x0A]))
    try! handle.synchronize()
}

func writeJSON(_ object: [String: Any], to url: URL) {
    let data = try! JSONSerialization.data(withJSONObject: object, options: [.prettyPrinted, .sortedKeys])
    try! data.write(to: url, options: .atomic)
}

func numberProperty(_ device: IOHIDDevice, _ key: String) -> Int? {
    (IOHIDDeviceGetProperty(device, key as CFString) as? NSNumber)?.intValue
}

func stringProperty(_ device: IOHIDDevice, _ key: String) -> String? {
    IOHIDDeviceGetProperty(device, key as CFString) as? String
}

func discoverDevices() -> ([IOHIDDevice], IOHIDManager) {
    let manager = IOHIDManagerCreate(kCFAllocatorDefault, IOOptionBits(0))
    let matching: [String: Any] = [
        kIOHIDVendorIDKey as String: vendorID,
        kIOHIDProductIDKey as String: productID,
    ]
    IOHIDManagerSetDeviceMatching(manager, matching as CFDictionary)
    IOHIDManagerScheduleWithRunLoop(manager, CFRunLoopGetCurrent(), CFRunLoopMode.defaultMode.rawValue)
    let opened = IOHIDManagerOpen(manager, IOOptionBits(kIOHIDOptionsTypeNone))
    guard opened == kIOReturnSuccess else {
        fatalError("IOHIDManagerOpen failed: 0x\(String(opened, radix: 16))")
    }
    guard let set = IOHIDManagerCopyDevices(manager) as? Set<IOHIDDevice> else {
        fatalError("IOHIDManager returned no device set")
    }
    let matches = set.filter {
        numberProperty($0, kIOHIDVendorIDKey) == vendorID &&
        numberProperty($0, kIOHIDProductIDKey) == productID &&
        stringProperty($0, kIOHIDProductKey) == "JadeAudio JA11"
    }
    return (Array(matches), manager)
}

func identity(_ device: IOHIDDevice) -> [String: Any] {
    [
        "vendor_id": numberProperty(device, kIOHIDVendorIDKey) ?? -1,
        "product_id": numberProperty(device, kIOHIDProductIDKey) ?? -1,
        "location_id": numberProperty(device, kIOHIDLocationIDKey) ?? -1,
        "version_number": numberProperty(device, kIOHIDVersionNumberKey) ?? -1,
        "product": stringProperty(device, kIOHIDProductKey) ?? "",
        "serial_sha256": sha256(stringProperty(device, kIOHIDSerialNumberKey) ?? ""),
    ]
}

func parseResponse(_ raw: [UInt8], request: Request) throws -> (value: UInt8, total: Int) {
    guard raw.count >= 9 else { throw NSError(domain: "probe", code: 1, userInfo: [NSLocalizedDescriptionKey: "incomplete response"]) }
    let declared = Int(raw[6])
    let total = 9 + declared
    guard raw.count >= total else { throw NSError(domain: "probe", code: 2, userInfo: [NSLocalizedDescriptionKey: "short declared response"]) }
    guard raw[total...].allSatisfy({ $0 == 0 }) else { throw NSError(domain: "probe", code: 3, userInfo: [NSLocalizedDescriptionKey: "non-zero trailing bytes"]) }
    guard raw[0] == 0x02, raw[1] == 0xBB, raw[2] == 0x0B else { throw NSError(domain: "probe", code: 4, userInfo: [NSLocalizedDescriptionKey: "unexpected header"]) }
    let sequence = (UInt16(raw[3]) << 8) | UInt16(raw[4])
    guard sequence == request.sequence else { throw NSError(domain: "probe", code: 5, userInfo: [NSLocalizedDescriptionKey: "sequence mismatch"]) }
    guard raw[5] == request.command else { throw NSError(domain: "probe", code: 6, userInfo: [NSLocalizedDescriptionKey: "command mismatch"]) }
    let payload = Array(raw[7..<(7 + declared)])
    let carried = raw[7 + declared]
    let computed = crc8Maxim(Array(raw[3..<(7 + declared)]))
    guard carried == computed else { throw NSError(domain: "probe", code: 7, userInfo: [NSLocalizedDescriptionKey: "CRC mismatch"]) }
    guard raw[8 + declared] == 0xEE else { throw NSError(domain: "probe", code: 8, userInfo: [NSLocalizedDescriptionKey: "terminator mismatch"]) }
    guard let value = payload.first else { throw NSError(domain: "probe", code: 9, userInfo: [NSLocalizedDescriptionKey: "missing value"]) }
    if request.command == 0x02 && !(0...60).contains(value) {
        throw NSError(domain: "probe", code: 10, userInfo: [NSLocalizedDescriptionKey: "volume value outside 0..60"])
    }
    if request.command == 0x11 && !(1...5).contains(value) {
        throw NSError(domain: "probe", code: 11, userInfo: [NSLocalizedDescriptionKey: "filter value outside 1..5"])
    }
    return (value, total)
}

func classify(_ outcomes: [String]) -> String {
    guard outcomes.count == 4 else { return "INCONCLUSIVE: incomplete session" }
    if outcomes[0] != "R" { return "INCONCLUSIVE: volume control failed" }
    if outcomes[1...] == ["S", "S", "S"] { return "BOUNDED_NEGATIVE: all three filter reads silent" }
    return "REOPEN: at least one filter read returned a qualifying value"
}

let args = CommandLine.arguments
guard args.count == 2 && args[1] == "plan" else {
    guard args.count == 4 && args[1] == "run" && args[2] == "owner-approved-B1-2026-11-03-r2" else {
        fputs("usage: ja11_b1_probe plan | run owner-approved-B1-2026-11-03-r2 EVIDENCE_DIR\n", stderr)
        exit(2)
    }
    let evidenceURL = URL(fileURLWithPath: args[3], isDirectory: true)
    if FileManager.default.fileExists(atPath: evidenceURL.path) &&
        !((try? FileManager.default.contentsOfDirectory(atPath: evidenceURL.path)) ?? []).isEmpty {
        fputs("evidence directory must be new or empty\n", stderr)
        exit(2)
    }
    try! FileManager.default.createDirectory(at: evidenceURL, withIntermediateDirectories: true)
    let manifestURL = evidenceURL.appendingPathComponent("manifest.json")
    let journalURL = evidenceURL.appendingPathComponent("session.jsonl")
    FileManager.default.createFile(atPath: journalURL.path, contents: nil)
    let journal = try! FileHandle(forWritingTo: journalURL)

    let (devices, manager) = discoverDevices()
    guard devices.count == 1 else {
        fputs("expected one exact JA11 HID device, found \(devices.count)\n", stderr)
        exit(3)
    }
    let device = devices[0]
    let opened = IOHIDDeviceOpen(device, IOOptionBits(kIOHIDOptionsTypeNone))
    guard opened == kIOReturnSuccess else {
        fputs("IOHIDDeviceOpen failed: 0x\(String(opened, radix: 16))\n", stderr)
        exit(3)
    }

    let openingIdentity = identity(device)
    let state = CaptureState()
    let statePointer = Unmanaged.passRetained(state).toOpaque()
    let inputBuffer = UnsafeMutablePointer<UInt8>.allocate(capacity: 64)
    IOHIDDeviceRegisterInputReportCallback(device, inputBuffer, 64, reportCallback, statePointer)

    var manifest: [String: Any] = [
        "schema": 1,
        "session_approval": args[2],
        "host": "macOS",
        "identity": openingIdentity,
        "plan": requests.map { ["name": $0.name, "frame_hex": hex($0.frame)] },
        "outcomes": [],
        "interpretation": "INCONCLUSIVE: session did not complete",
    ]
    writeJSON(manifest, to: manifestURL)
    writeJSONLine(["event": "session_start", "identity": openingIdentity], to: journal)

    var outcomes: [String] = []
    var results: [[String: Any]] = []
    do {
        for (index, request) in requests.enumerated() {
            if index > 0 && outcomes.first != "R" {
                throw NSError(domain: "probe", code: 20, userInfo: [NSLocalizedDescriptionKey: "volume control failed; no filter request sent"])
            }
            if index > 0 {
                let (currentDevices, currentManager) = discoverDevices()
                let identityUnchanged = currentDevices.count == 1 &&
                    NSDictionary(dictionary: identity(currentDevices[0])).isEqual(to: openingIdentity)
                withExtendedLifetime(currentManager) {}
                guard identityUnchanged else {
                    throw NSError(domain: "probe", code: 21, userInfo: [NSLocalizedDescriptionKey: "device identity changed"])
                }
            }

            state.reports.removeAll()
            state.sentAt = ProcessInfo.processInfo.systemUptime
            state.accepting = true
            writeJSONLine(["event": "send", "request": request.name, "frame_hex": hex(request.frame)], to: journal)

            let result = request.frame.withUnsafeBufferPointer { buffer in
                IOHIDDeviceSetReport(device, kIOHIDReportTypeOutput, CFIndex(reportID), buffer.baseAddress!, buffer.count)
            }
            guard result == kIOReturnSuccess else {
                throw NSError(domain: "probe", code: 22, userInfo: [NSLocalizedDescriptionKey: "output report failed: 0x\(String(result, radix: 16))"])
            }

            let deadline = state.sentAt + windowSeconds
            while ProcessInfo.processInfo.systemUptime < deadline && state.reports.isEmpty {
                RunLoop.current.run(mode: .default, before: Date(timeIntervalSinceNow: 0.02))
            }
            state.accepting = false

            if state.reports.isEmpty {
                outcomes.append("S")
                results.append(["name": request.name, "outcome": "S"])
                writeJSONLine(["event": "silence", "request": request.name], to: journal)
                continue
            }
            guard state.reports.count == 1 else {
                throw NSError(domain: "probe", code: 23, userInfo: [NSLocalizedDescriptionKey: "multiple reports in one window"])
            }
            let report = state.reports[0]
            let parsed: (value: UInt8, total: Int)
            do {
                parsed = try parseResponse(report.bytes, request: request)
            } catch {
                writeJSONLine([
                    "event": "response_parse_error",
                    "request": request.name,
                    "elapsed_ms": report.elapsedMs,
                    "raw_hex": hex(report.bytes),
                    "error": error.localizedDescription,
                ], to: journal)
                throw error
            }
            outcomes.append("R")
            results.append([
                "name": request.name,
                "outcome": "R",
                "elapsed_ms": report.elapsedMs,
                "raw_hex": hex(report.bytes),
                "value": parsed.value,
            ])
            writeJSONLine([
                "event": "response",
                "request": request.name,
                "elapsed_ms": report.elapsedMs,
                "raw_hex": hex(report.bytes),
                "value": parsed.value,
            ], to: journal)
        }
        let interpretation = classify(outcomes)
        manifest["outcomes"] = outcomes
        manifest["requests"] = results
        manifest["interpretation"] = interpretation
        writeJSON(manifest, to: manifestURL)
        writeJSONLine(["event": "session_complete", "outcomes": outcomes, "interpretation": interpretation], to: journal)
        print(interpretation)
        try! journal.close()
    } catch {
        manifest["outcomes"] = outcomes
        manifest["requests"] = results
        manifest["interpretation"] = "INCONCLUSIVE: stopped: \(error.localizedDescription)"
        writeJSON(manifest, to: manifestURL)
        writeJSONLine(["event": "session_stopped", "error": error.localizedDescription], to: journal)
        fputs("error: \(error.localizedDescription)\n", stderr)
        exit(4)
    }
    withExtendedLifetime(manager) {}
    exit(0)
}

for (index, request) in requests.enumerated() {
    print("\(index + 1). \(request.name): \(hex(request.frame))")
}
