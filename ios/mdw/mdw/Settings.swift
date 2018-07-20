//
//  Settings.swift
//  mdw
//

import UIKit

struct SettingNames {
    let CENTRAL_URL = "central_url"
    let DEFAULT_CENTRAL_URL = "https://mdw-central.com"
    
    let USERNAME = "username"
    let APPS = "apps"
    let APP_TOKENS = "app_tokens"
    let ENV = "env"
}

class Settings {
    
    static let instance = Settings()
    
    private init() {
    }
    
    let names = SettingNames()
    
    var username: String? {
        get {
            return UserDefaults.standard.string(forKey: names.USERNAME)
        }
        set {
            UserDefaults.standard.set(newValue, forKey: names.USERNAME)
        }
    }
    
    var centralUrl: String {
        get {
            var centralUrl = UserDefaults.standard.string(forKey: names.CENTRAL_URL)
            if (centralUrl == nil) {
                centralUrl = names.DEFAULT_CENTRAL_URL
            }
            return centralUrl!
        }
        set {
            UserDefaults.standard.set(newValue, forKey: names.CENTRAL_URL)
        }
    }
    
    var apps: [App]? {
        get {
            if let json = UserDefaults.standard.string(forKey: names.APPS) {
                return try! JSONDecoder().decode([App].self, from: json.data(using: .utf8)!)
            }
            else {
                return nil
            }
        }
        set {
            if (newValue == nil) {
                UserDefaults.standard.removeObject(forKey: names.APPS)
            }
            else {
                let jsonBytes = try! JSONEncoder().encode(newValue)
                UserDefaults.standard.set(String(data: jsonBytes, encoding: .utf8), forKey: names.APPS)
            }
        }
    }
    
    var env: Env? {
        get {
            if let envName = UserDefaults.standard.string(forKey: names.ENV) {
                if (apps != nil) {
                    for app: App in apps! {
                        if (app.envs != nil) {
                            for env: Env in app.envs! {
                                if (env.name == envName) {
                                    return env
                                }
                            }
                        }
                    }
                }
            }
            return nil
        }
        set {
            UserDefaults.standard.set(newValue == nil ? nil : newValue!.name, forKey: names.ENV)
        }
    }
    
    var authHeader: String? {
        get {
            if let env = self.env {
                let appId = env.appId
                if let token = self.tokens[appId] {
                    return "Bearer " + token
                }
            }
            return nil
        }
    }

    var tokens: Dictionary<String,String> {
        get {
            if let tokens = UserDefaults.standard.dictionary(forKey: names.APP_TOKENS) {
                return tokens as! Dictionary<String,String>
            }
            else {
                return [:]
            }
        }
    }
    
    func setToken(_ token: String, forAppId: String) {
        var tokens: Dictionary<String,String> = [:]
        if let t = UserDefaults.standard.dictionary(forKey: names.APP_TOKENS) {
            tokens = t as! Dictionary<String,String>
        }
        tokens[forAppId] = token
        UserDefaults.standard.set(tokens, forKey: names.APP_TOKENS)
    }
    
    func clearTokens() {
        UserDefaults.standard.removeObject(forKey: names.APP_TOKENS)
    }
    
    func logout() {
        clearTokens()
        username = nil
        apps = nil
        env = nil
    }
    
    func isTablet() -> Bool {
        return UIDevice.current.userInterfaceIdiom == .pad
    }
    
    func isLandscape() -> Bool {
        return UIDevice.current.orientation.isValidInterfaceOrientation
            ? UIDevice.current.orientation.isLandscape
            : UIApplication.shared.statusBarOrientation.isLandscape
    }
}


