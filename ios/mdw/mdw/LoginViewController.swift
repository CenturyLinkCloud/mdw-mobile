//
//  LoginViewController.swift
//  mdw
//

import UIKit
import PopupDialog

class LoginViewController: UIViewController, UITextFieldDelegate, AppListDelegate {
    
    @IBOutlet weak var logoImage: UIImageView!
    @IBOutlet weak var signinLabel: UILabel!
    @IBOutlet weak var usernameTextField: UITextField!
    @IBOutlet weak var passwordTextField: UITextField!
    @IBOutlet weak var loginButton: UIButton!
    @IBOutlet weak var hubLogoImage: UIImageView!
    @IBOutlet weak var centralLink: UITextView!
    
    var username: String?
    var password: String?
    
    var appListPopup: PopupDialog?
    
    @IBAction func login(_ sender: UIButton) {
        view.endEditing(true)
        username = usernameTextField.text
        password = passwordTextField.text
        if (!(username ?? "").isEmpty && !(password ?? "").isEmpty) {
            authenticate(username: username!, password: password!)
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let logo = UIImage(named: "logo")
        logoImage.image = logo
        signinLabel.textColor = UIColor(red:0, green:0.3294, blue:0.0431, alpha: 1.0)
        usernameTextField.delegate = self
        usernameTextField.text = Settings.instance.username
        passwordTextField.delegate = self
        loginButton.backgroundColor = UIColor(red:0, green:0.47, blue:0.42, alpha:1.0)
        loginButton.layer.cornerRadius = 5
        loginButton.layer.borderWidth = 1
        let hubLogo = UIImage(named: "hub_logo")
        hubLogoImage.image = hubLogo
        let linkAttributes: [NSAttributedStringKey: Any] = [
            .link: NSURL(string: Settings.instance.centralUrl + "/signin")!,
            .foregroundColor: UIColor(red:0.098, green:0.4627, blue:0.8235,alpha: 1.0)
        ]
        let attributedString = NSMutableAttributedString(string: "Sign up on MDW Central")
        attributedString.setAttributes(linkAttributes, range: NSMakeRange(0, 22))
        centralLink.attributedText = attributedString
        centralLink.text = "Sign up on MDW Central"
        centralLink.font = UIFont(name: "Helvetica", size: 18)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        bottomBorder(textField: usernameTextField)
        bottomBorder(textField: passwordTextField)
    }
    
    func bottomBorder(textField: UITextField) {
        let border = CALayer()
        let width = CGFloat(1.5)
        border.borderColor = UIColor.darkGray.cgColor
        border.frame = CGRect(x: 0, y: textField.frame.size.height - width, width:  textField.frame.size.width, height: textField.frame.size.height)
        border.borderWidth = width
        textField.layer.addSublayer(border)
        textField.layer.masksToBounds = true
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        // hide the keyboard
        textField.resignFirstResponder()
        username = usernameTextField.text
        password = passwordTextField.text
        if (!(username ?? "").isEmpty && !(password ?? "").isEmpty) {
            authenticate(username: username!, password: password!)
        }
        return true
    }
    
    func showAppListPopup() {
        let appListVc = AppListViewController(nibName: "AppListViewController", bundle: nil)
        appListVc.delegate = self
        appListPopup = PopupDialog(viewController: appListVc, buttonAlignment: .horizontal, transitionStyle: .zoomIn, gestureDismissal: true)
        let logoutButton = DefaultButton(title: "LOGOUT", height: 40) {
            self.logout()
        }
        let cancelButton = CancelButton(title: "CANCEL", height: 40) {
            self.password = nil
            self.passwordTextField.text = nil
        }
        appListPopup!.addButtons([logoutButton, cancelButton])
        present(appListPopup!, animated: true, completion: nil)
    }
    
    func authenticate(username: String, password: String) {
        let params = ["user": username, "password": password, "appId": "mdw-mobile"] as Dictionary<String,String>
        
        var request = URLRequest(url: URL(string: Settings.instance.centralUrl + "/api/auth")!)
        request.httpMethod = "POST"
        request.httpBody = try? JSONSerialization.data(withJSONObject: params, options: [])
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(Secrets.MDW_MOBILE_TOKEN, forHTTPHeaderField: "mdw-app-token")
        
        let session = URLSession.shared
        let authTask = session.dataTask(with: request, completionHandler: { data, response, error -> Void in
            do {
                if (response == nil) {
                    throw "No response (are you offline?)"
                }
                var httpResponse = response as! HTTPURLResponse
                if (httpResponse.statusCode == 200) {
                    var json = try JSONSerialization.jsonObject(with: data!) as! Dictionary<String,AnyObject>
                    let userToken = json["mdwauth"] as! String
                    Settings.instance.setToken(userToken, forAppId: "mdw-mobile")
                    Settings.instance.username = username
                    request = URLRequest(url: URL(string: Settings.instance.centralUrl + "/api/apps?user=" + username)!)
                    request.httpMethod = "GET"
                    request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                    request.addValue(Secrets.MDW_MOBILE_TOKEN, forHTTPHeaderField: "mdw-app-token")
                    request.addValue("Bearer \(userToken)", forHTTPHeaderField: "Authorization")
                    let appsTask = session.dataTask(with: request, completionHandler: { data, response, error -> Void in
                        httpResponse = response as! HTTPURLResponse
                        do {
                            logDebug("Apps Response: \(String(data: data!, encoding: .utf8) ?? "(empty)")")
                            guard httpResponse.statusCode == 200 else {
                                throw ApiError.notOk(statusCode: 200, response: String(data: data!, encoding: .utf8))
                            }
                            let apps = try JSONDecoder().decode(Apps.self, from: data!)
                            // clear out envs with no url
                            for app in apps.apps {
                                if (app.envs != nil) {
                                    var envs = [Env]()
                                    for env in app.envs! {
                                        if let url = env.url {
                                            // minimal validation
                                            if (url.starts(with: "http://") || url.starts(with: "https://")) {
                                                envs.append(env)
                                            }
                                        }
                                    }
                                    app.envs = envs
                                }
                            }
                            Settings.instance.apps = apps.apps
                            DispatchQueue.main.sync {
                                self.showAppListPopup()
                            }
                        }
                        catch let error {
                            print("ERROR: \(error.localizedDescription)")
                            Thread.callStackSymbols.forEach{print($0)}
                            self.showToast(message: error.localizedDescription)
                        }
                    })
                    appsTask.resume()
                }
                else {
                    logError("Auth failure (\(httpResponse.statusCode)): ")
                    DispatchQueue.main.sync {
                        self.passwordTextField.text = nil
                    }
                    self.showToast(message: "Authorization failure")
                }
            }
            catch let error {
                logError(error)
                self.showToast(message: error.localizedDescription)
            }
        })
        
        authTask.resume()
    }
    
    func logout() {
        Settings.instance.logout()
        username = nil
        usernameTextField.text = nil
        password = nil
        passwordTextField.text = nil
    }
    
    func didSelectEnv(_ env: Env) {
        appListPopup?.dismiss()
        Settings.instance.env = env
        // authorize for the app
        let params = ["user": username, "password": password, "appId": env.appId] as! Dictionary<String,String>
        var request = URLRequest(url: URL(string: Settings.instance.centralUrl + "/api/auth")!)
        request.httpMethod = "POST"
        request.httpBody = try? JSONSerialization.data(withJSONObject: params, options: [])
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue(Secrets.MDW_MOBILE_TOKEN, forHTTPHeaderField: "mdw-app-token")
        let authTask = URLSession.shared.dataTask(with: request, completionHandler: { data, response, error -> Void in
            do {
                if (error != nil) {
                    throw error!
                }
                let httpResponse = response as! HTTPURLResponse
                guard httpResponse.statusCode == 200 else {
                    throw ApiError.notOk(statusCode: httpResponse.statusCode, response: String(data: data!, encoding: .utf8))
                }
                var json = try JSONSerialization.jsonObject(with: data!) as! Dictionary<String,AnyObject>
                let userToken = json["mdwauth"] as! String
                Settings.instance.setToken(userToken, forAppId: env.appId)
                let storyBoard = UIStoryboard(name: "Main", bundle:nil)
                NavigationState.instance.load { error -> Void in
                    if let err = error {
                        self.password = nil
                        self.passwordTextField.text = nil
                        self.showToast(message: err.localizedDescription)
                    }
                    else {
                        self.present(storyBoard.instantiateViewController(withIdentifier: "entry"), animated: true)
                    }
                }
            }
            catch let error {
                logError(error)
                self.password = nil
                DispatchQueue.main.sync {
                    self.passwordTextField.text = nil
                }
                self.showToast(message: error.localizedDescription)
            }
        })
        authTask.resume()
    }
}

