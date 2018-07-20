//
//  SettingsViewController.swift
//  mdw
//

import UIKit

class SettingsViewController: UIViewController, UITextFieldDelegate {
    
    //MARK: Properties
    @IBOutlet var baseUrlTextField: UITextField!
    @IBOutlet var userTextField: UITextField!
    @IBOutlet var passwordTextField: UITextField!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let userDefaults = UserDefaults.standard
        baseUrlTextField.text = userDefaults.string(forKey: "baseUrl")
        baseUrlTextField.delegate = self
        userTextField.text = userDefaults.string(forKey: "user")
        userTextField.delegate = self
        passwordTextField.text = userDefaults.string(forKey: "password")
        passwordTextField.delegate = self
    }
    
    func textFieldDidEndEditing(_ textField: UITextField) {
        let userDefaults = UserDefaults.standard
        if (textField == baseUrlTextField) {
            userDefaults.set(textField.text, forKey: "baseUrl")
        } else if (textField == userTextField) {
            userDefaults.set(textField.text, forKey: "user")
        } else if (textField == passwordTextField) {
            userDefaults.set(textField.text, forKey: "password")
        }
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        // hide the keyboard
        textField.resignFirstResponder()
        return true
    }
}

