//
//  Errors.swift
//  mdw
//

import UIKit
import os.log

// TODO: error reporting

extension String: LocalizedError {
    public var errorDescription: String? { return self }
}
    
enum ApiError: Error {
    case notOk(statusCode: Int, response: String?)
    case badUrl(url: String)
    case noData(call: String)
    case unparseableResponse(response: String)
}
extension ApiError: LocalizedError {
    public var errorDescription: String? {
        switch self {
            case .notOk(statusCode: let statusCode, response: let response):
                return "ApiError: (\(statusCode))" + (response  == nil ? "" : "\nResponse:" + response!)
            case .badUrl(url: let url):
                return "Bad URL: \(url)"
            case .noData(call: let call):
                return "No response data from: \(call)"
        case .unparseableResponse:
                return "Bad response"
        }
    }
}

enum AuthError: Error {
    case noToken
}
extension AuthError: LocalizedError {
    public var errorDescription: String? {
        switch self {
            case .noToken:
                return "Not authorized"
        }
    }
}

func logError(_ message: String, report: Bool = true) {
    os_log("%@", type: .error, message)
    if (report) {
        reportError(message)
    }
}
func logError(_ error: Error, report: Bool = true) {
    os_log("%@", type: .error, error.localizedDescription)
    var stackTrace = ""
    for symbol: String in Thread.callStackSymbols {
        os_log("%@", type: .error, symbol)
        stackTrace += symbol + "\n"
    }
    if (report) {
        reportError(error.localizedDescription, stackTrace: stackTrace)
    }
}
func logInfo(_ message: String) {
    os_log("%@", type: .info, message)
}
func logDebug(_ message: String) {
    os_log("%@", type: .debug, message)
}

func reportError(_ message: String, stackTrace: String? = nil) {
    let source = "mdw-mobile v" + (Bundle.main.infoDictionary!["CFBundleShortVersionString"] as! String) + "(ios \(UIDevice.current.systemVersion))"
    var info = ["source": source, "message": message]
    if let st = stackTrace {
        info["stackTrace"] = st
    }
    let err = ["error": info]
    
    var request = URLRequest(url: URL(string: Settings.instance.centralUrl + "/api/errors")!)
    request.httpMethod = "POST"
    request.httpBody = try? JSONSerialization.data(withJSONObject: err, options: [])
    request.addValue("application/json", forHTTPHeaderField: "Content-Type")
    request.addValue(Secrets.MDW_MOBILE_TOKEN, forHTTPHeaderField: "mdw-app-token")
    let reporterTask = URLSession.shared.dataTask(with: request, completionHandler: { data, response, error -> Void in
        if (error != nil) {
            logError(error!, report: false)
        }
        if (response is HTTPURLResponse) {
            let httpResponse = response as! HTTPURLResponse
            if (httpResponse.statusCode > 201) {
                var msg = "Bad response from reporter"
                if let respData = data {
                    msg += ": " + String(data: respData, encoding: .utf8)!
                }
                logError(msg, report: false)
            }
            
        }
    })
    reporterTask.resume()
}
