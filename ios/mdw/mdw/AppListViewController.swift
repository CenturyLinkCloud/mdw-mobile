//
//  AppListViewController.swift
//  mdw
//

import UIKit

protocol AppListDelegate {
    func didSelectEnv(_ env: Env)
}

class AppListViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    @IBOutlet weak var logoImage: UIImageView!
    @IBOutlet weak var appEnvLabel: UILabel!
    @IBOutlet weak var appListTable: UITableView!
    @IBOutlet weak var appListTableTop: NSLayoutConstraint!
    
    var apps: [App]?
    var delegate: AppListDelegate?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        let hubLogo = UIImage(named: "hub_logo")
        logoImage.image = hubLogo
        
        apps = Settings.instance.apps
        
        var msg: String? = nil
        if (apps == nil || apps?.count == 0) {
            msg = "It appears you don't have any apps yet.  Head over to mdw-central.com to create a new app or request access to an existing one."
        }
        else {
            var hasEnv = false
            for app in apps! {
                if ((app.envs?.count)! > 0) {
                    hasEnv = true
                    break
                }
            }
            if (!hasEnv) {
                msg = "No accessible envs found.  Head over to mdw-central.com to create an app env, and make sure to give it a valid MDW endpoint URL."
            }
        }
        
        if (msg != nil) {
            appListTableTop.constant += 85
            let msgView = UITextView(frame: CGRect(x: 20, y: 50, width: 300, height: 100))
            msgView.isEditable = false
            msgView.dataDetectorTypes = .link
            msgView.font = UIFont.systemFont(ofSize: 16)
            msgView.text = msg
            self.view.addSubview(msgView)
            self.view.layoutIfNeeded()
        }

        appListTable.delegate = self
        appListTable.dataSource = self
    }

    func contentSizeDidChange(_ size: String) {
        appListTable.reloadData()
    }
    
    @objc func endEditing() {
        view.endEditing(true)
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        if let appCount = apps?.count {
            return appCount
        }
        else {
            return 0
        }
    }

    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if let envCount = apps?[section].envs?.count {
            return envCount
        }
        else {
            return 0
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cellIdentifier = "appEnvCell"
        
        var cell: UITableViewCell! = tableView.dequeueReusableCell(withIdentifier: cellIdentifier) as UITableViewCell?
        
        if cell == nil {
            cell = UITableViewCell(style: .default, reuseIdentifier: cellIdentifier)
            cell.selectionStyle = .blue
        }
        
        let app = apps![indexPath.section]
        let env = app.envs![indexPath.row]
        cell.textLabel?.text = env.name
        
        if let e = Settings.instance.env {
            if (e.appId == app.id && e.name == env.name) {
                cell.textLabel?.font = UIFont.boldSystemFont(ofSize: cell.textLabel!.font.pointSize)
            }
        }

        return cell
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        return apps![section].id
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return section == 0 ? 40 : 20
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 40
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.selectRow(at: indexPath, animated: false, scrollPosition: .none)
        let env = apps![indexPath.section].envs![indexPath.row]
        logDebug("Selected App Env: \(env.appId)/\(env.name)")
        if (delegate != nil) {
            delegate?.didSelectEnv(env);
        }
    }
}
