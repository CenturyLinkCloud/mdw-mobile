//
//  WebViewController.swift
//  mdw
//

import UIKit
import WebKit
import DrawerController
import PopupDialog

class AuthWebView: WKWebView {
    override func load(_ request: URLRequest) -> WKNavigation? {
        if let authHeader = Settings.instance.authHeader {
            let mutableRequest = ((request as NSURLRequest).mutableCopy() as? NSMutableURLRequest)!
            mutableRequest.setValue(authHeader, forHTTPHeaderField: "Authorization")
            return super.load(mutableRequest as URLRequest)
        }
        else {
            return super.load(request)
        }
    }
}

class WebViewController: UIViewController, NavigationDelegate, WKNavigationDelegate, UITabBarDelegate, AppListDelegate {
    
    var navState: NavigationState!
    
    var appListPopup: PopupDialog?
    
    @IBOutlet var webView: WKWebView!
    @IBOutlet var tabBar: UITabBar!

    var drawerButton: DrawerBarButtonItem?
    var loadingImage = UIImageView()
    
    var initializationTimer: Timer?
    var initializationSeconds: Float = 0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        NotificationCenter.default.addObserver(self, selector: #selector(WebViewController.orientationWillChange), name: NSNotification.Name.UIDeviceOrientationDidChange, object: nil)
        
        self.navState = NavigationState.instance
        self.navigationItem.title = "Workflow"

        self.navigationController?.navigationBar.barStyle = UIBarStyle.black
        self.navigationController?.navigationBar.barTintColor = UIColor(red:0.00, green:0.47, blue:0.42, alpha:1.0)
        self.navigationController?.navigationBar.tintColor = UIColor.white // for titles, buttons, etc.
        
        // drawer button
        drawerButton = DrawerBarButtonItem(target: self, action: #selector(drawerButtonPress(_:)), menuIconColor: UIColor.white)
        self.navigationItem.setLeftBarButton(drawerButton, animated: true)
        
        // exit button
        let exitButton = UIBarButtonItem(image: UIImage(named: "exit"), style: .plain, target: self, action: #selector(exit(_:)))
        self.navigationItem.rightBarButtonItem = exitButton

        if (tabBar != nil) {
            tabBar.delegate = self
            let appearance = UITabBarItem.appearance()
            let attributes = [NSAttributedStringKey.font : UIFont.systemFont(ofSize: 16)]
            appearance.setTitleTextAttributes(attributes, for: .normal)
        }
        
        webView.navigationDelegate = self
        
        // loading image animation
        showLoading(false)
        let hubLogo = UIImage(named: "hub_logo")
        loadingImage.image = hubLogo
        // animate the logo image
        var loadingImages = [UIImage]()
        for countValue in 0...4 {
            let imageName : String = "loading_\(countValue).png"
            loadingImages.append(UIImage(named: imageName)!)
        }
        loadingImage.animationImages = loadingImages
        loadingImage.animationDuration = 0.2
        loadingImage.frame = CGRect(x: (view.frame.width - 48) / 2, y: 200, width: 48, height: 48)
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        NavigationState.instance.delegate = self
        self.initializationTimer = Timer.scheduledTimer(timeInterval: 0.2, target: self, selector: #selector(loadInitial), userInfo: nil, repeats: true)
    }
    
    @objc func loadInitial() {
        if (self.initializationSeconds < 5) {
            self.initializationSeconds += 0.2
            if (self.navState.selectedDrawerItem != nil) {
                self.initializationTimer?.invalidate()
                self.initializationSeconds = 0
                showLoading(true)
                self.load(self.navState.url(), title: self.navState.selectedDrawerItem!.label);
                logDebug("Web view initialized in: \(self.initializationSeconds)s")
            }
        }
        else {
            logDebug("Initialization timeout after: \(self.initializationSeconds)s")
        }
    }
    
    @objc func drawerButtonPress(_ sender: AnyObject?) {
        self.evo_drawerController?.toggleDrawerSide(.left, animated: true, completion: nil)
    }
    
    @objc func exit(_ sender: AnyObject?) {
        let appListVc = AppListViewController(nibName: "AppListViewController", bundle: nil)
        appListVc.delegate = self
        appListPopup = PopupDialog(viewController: appListVc, buttonAlignment: .horizontal, transitionStyle: .zoomIn, gestureDismissal: true)
        let logoutButton = DefaultButton(title: "LOGOUT", height: 40) {
            self.logout()
        }
        let cancelButton = CancelButton(title: "CANCEL", height: 40) {
        }
        appListPopup!.addButtons([logoutButton, cancelButton])
        present(appListPopup!, animated: true, completion: nil)
    }
    
    func logout() {
        Settings.instance.logout()
        self.present(UIStoryboard(name: "Main", bundle:nil).instantiateViewController(withIdentifier: "login"), animated: true)
    }
    
    func didSelectEnv(_ env: Env) {
        appListPopup?.dismiss()
        Settings.instance.env = env
        webView.load(URLRequest(url: URL(string:"about:blank")!))
        NavigationState.instance.load { error -> Void in
            if let err = error {
                logError(err)
                self.present(UIStoryboard(name: "Main", bundle:nil).instantiateViewController(withIdentifier: "login"), animated: true)
            }
            else {
                self.load(self.navState.url(), title: self.navState.selectedDrawerItem!.label);
            }
        }
    }
    
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        for link in self.navState.getLinks(drawer: self.navState.selectedDrawerItem!.label) {
            if item.title == link.label {
                if let url = URL(string: self.navState.url(path: link.href)) {
                    logDebug("Loading: " + link.href)
                    webView.load(URLRequest(url: url))
                }
            }
        }
    }
    
    func load(_ url: String, title: String, closeDrawer: Bool? = false) {
        if (closeDrawer! && drawerButton != nil) {
            (drawerButton!.customView as! UIButton).sendActions(for: .touchUpInside)
        }
        if let realUrl = URL(string: url) {
            var label = title
            if let env = Settings.instance.env {
                // include env for non-prod
                if (!env.name.starts(with: "prod") && !env.name.starts(with: "Prod")) {
                    label += " (\(env.name))"
                }
            }
            self.navigationItem.title = label
            logInfo("Loading: " + url)
            webView.load(URLRequest(url: realUrl))
            self.initTabs(title)
        }
    }
    
    func link(_ url: String, closeDrawer: Bool? = false) {
        if (closeDrawer! && drawerButton != nil) {
            (drawerButton!.customView as! UIButton).sendActions(for: .touchUpInside)
        }
        UIApplication.shared.open(URL(string: url)!, options: [:], completionHandler: nil)
    }

    func initTabs(_ drawerLabel: String) {
        var max = 3
        if (Settings.instance.isLandscape() || Settings.instance.isTablet()) {
            max = 5
        }
        if (Settings.instance.isLandscape() && Settings.instance.isTablet()) {
            max = 7
        }
        
        var sorted = self.navState.getLinks(drawer: drawerLabel)
        sorted.sort {nav1, nav2 in
            var p1 = nav1.priority
            if (p1 == nil) {
                p1 = 0
            }
            var p2 = nav2.priority
            if (p2 == nil) {
                p2 = 0
            }
            if (p1 == p2 || p2! == 0) {
                return true
            }
            else if (p1 == 0) {
                return false
            }
            else {
                return p1! > p2!
            }
        }

        var items: [UITabBarItem] = []
        for link in self.navState.getLinks(drawer: drawerLabel) {
            if (items.count < max) {
                items.append(self.tabBarItem(link))
            }
        }
        self.tabBar.items = items
        tabBar.selectedItem = items[0]
    }
    
    func tabBarItem(_ link: Link) -> UITabBarItem {
        let tabBarItem = UITabBarItem(title: link.label, image: nil, selectedImage: nil)
        if (!Settings.instance.isTablet()) {
            tabBarItem.titlePositionAdjustment = UIOffset(horizontal: 0, vertical: -12)
        }
        return tabBarItem
    }
    
    @objc func orientationWillChange() {
        initTabs(self.navState.selectedDrawerItem!.label)
    }
    
    func showLoading(_ loading: Bool) {
        if (loading) {
            view.addSubview(loadingImage)
            loadingImage.startAnimating()
        }
        else {
            loadingImage.stopAnimating()
            loadingImage.removeFromSuperview()
        }
    }
    
    func isLogin(_ urlStr: String) -> Bool {
        var url = urlStr
        let hash = url.index(of: "#")
        if (hash > 0) {
            url = url.substring(0, length: hash)
        }
        let q = url.index(of: "?");
        if (q > 0) {
            url = url.substring(0, length: q)
        }
        return url.endsWith("/login")
    }
    
    // webview navigation
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url?.absoluteString {
            if (isLogin(url)) {
                decisionHandler(.cancel)
                Settings.instance.logout()
                self.present(UIStoryboard(name: "Main", bundle:nil).instantiateViewController(withIdentifier: "login"), animated: true)
            }
            else {
                decisionHandler(.allow)
            }
        }
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        logInfo("Loaded: " + webView.url!.absoluteString)
        showLoading(false)
    }
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        logError(error)
        showLoading(false)
    }
    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        logError(error)
        showLoading(false)
    }
}

