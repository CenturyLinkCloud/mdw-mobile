//
//  AppEnvs.swift
//  mdw
//

import Foundation

/**
 * Model for apps and envs.
 */
class Apps: Codable {
    let apps: [App]
}

class App: Codable {
    let id: String
    let name: String
    var envs: [Env]?
}

class Env: Codable {
    let name: String
    let url: String?
    let appId: String
}

