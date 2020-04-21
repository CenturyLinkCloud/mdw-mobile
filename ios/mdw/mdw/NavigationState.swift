//
//  NavigationState.swift
//  mdw
//

import Foundation

protocol NavigationDelegate {
    func load(_ url: String, title: String, closeDrawer: Bool?)
    func link(_ url: String, closeDrawer: Bool?)
}

class NavigationState {

    enum Section: Int {
        case hub
        case documentation
    }
    
    var baseUrl: String?
    var delegate: NavigationDelegate?
    
    var navDrawerItems: [NavDrawerItem]?
    var selectedDrawerItem: NavDrawerItem?

    static let instance = NavigationState()
    
    static let docsNavItems = [
        DocsNavItem(label: "Help", icon: "help.png", url: "https://centurylinkcloud.github.io/mdw/docs/help/"),
        DocsNavItem(label: "MDW Site", icon: "site.png", url: "https://centurylinkcloud.github.io/mdw/")
    ]
    
    func url() -> String {
        return url(path: self.selectedDrawerItem!.navs[0].links[0].href)
    }
    
    func url(path: String) -> String {
        return baseUrl! + "/" + path; // + "?mdwMobile=true"
    }
    
    func load(_ callback: @escaping (Error?) -> Void) {
        
        if let env = Settings.instance.env {
            if (Settings.instance.tokens[env.appId] != nil) {
                self.baseUrl = env.url
                let urlString = self.url(path: "js/nav.json")
                guard let url = URL(string: urlString) else {
                    callback(ApiError.badUrl(url: urlString))
                    return;
                }
                
                URLSession.shared.dataTask(with: url) { (data, response, error) in
                    if error != nil {
                        DispatchQueue.main.sync {
                            callback(error!)
                        }
                        return
                    }
                    
                    guard let data = data else {
                        DispatchQueue.main.sync {
                            callback(ApiError.noData(call: "Nav load"))
                        }
                        return;
                    }
                    
                    logDebug("Apps Response: " + String(data: data, encoding: .utf8)!)
                    do {
                        self.navDrawerItems = try JSONDecoder().decode([NavDrawerItem].self, from: data)
                        self.selectedDrawerItem = self.navDrawerItems![0]
                        DispatchQueue.main.sync {
                            callback(nil)
                        }
                        
                    } catch let jsonError {
                        callback(jsonError)
                    }
                }.resume()
            }
            else {
                callback(AuthError.noToken)
            }
        }
        else {
            callback(AuthError.noToken)
        }
    }
    
    func getDrawerLabels() -> [String] {
        var labels:[String] = []
        if (self.navDrawerItems != nil) {
            for item in self.navDrawerItems! {
                labels.append(item.label)
            }
        }
        return labels
    }
    
    func getDrawerItem(label: String) -> NavDrawerItem {
        for item in self.navDrawerItems! {
            if item.label == label {
                return item
            }
        }
        return self.navDrawerItems![0]
    }
    
    func getLinks(drawer: String) -> [Link] {
        var links:[Link] = []
        let drawerItem = getDrawerItem(label: drawer)
        for nav in drawerItem.navs {
            for link in nav.links {
                links.append(link)
            }
        }
        return links
    }

}

/**
 * Populated from a "tab" object in JSON from server.
 */
class NavDrawerItem: Codable {
    let id: String
    let label: String
    let icon: String
    let url: String
    let condition: String?
    let routes: [String]? = []
    let navs: [Nav]
}
class Nav: Codable {
    let id: String?
    let links: [Link]
    
}
class Link: Codable {
    let label: String
    let path: String
    let href: String
    let priority: Int?
    let target: String?
}

/**
 * Documentation items.
 */
struct DocsNavItem {
    var label: String
    var icon: String
    var url: String
}
