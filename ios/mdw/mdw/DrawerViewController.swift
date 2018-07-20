//
//  DrawerViewController.swift
//  mdw
//

import UIKit
import DrawerController

class DrawerViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    var tableView: UITableView!
    var navState: NavigationState!
    
    @objc fileprivate func contentSizeDidChangeNotification(_ notification: Notification) {
        if let userInfo: NSDictionary = (notification as NSNotification).userInfo as NSDictionary? {
            self.contentSizeDidChange(userInfo[UIContentSizeCategoryNewValueKey] as! String)
        }
    }
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        self.restorationIdentifier = "DrawerController"
        self.navState = NavigationState.instance
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.restorationIdentifier = "DrawerController"
        self.navState = NavigationState.instance
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        NotificationCenter.default.addObserver(self, selector: #selector(contentSizeDidChangeNotification(_:)), name: NSNotification.Name.UIContentSizeCategoryDidChange, object: nil)

        self.tableView = UITableView(frame: self.view.bounds, style: .grouped)
        self.tableView.delegate = self
        self.tableView.dataSource = self
        self.view.addSubview(self.tableView)
        self.tableView.autoresizingMask = [ .flexibleWidth, .flexibleHeight ]
        
        // appearance options for drawer when expanded
        self.tableView.backgroundColor = UIColor(red: 110 / 255, green: 113 / 255, blue: 115 / 255, alpha: 1.0)
        self.tableView.separatorStyle = .none
        self.navigationController?.navigationBar.barTintColor = UIColor(red: 161 / 255, green: 164 / 255, blue: 166 / 255, alpha: 1.0)
        self.navigationController?.navigationBar.titleTextAttributes = [NSAttributedStringKey.foregroundColor: UIColor(red: 55 / 255, green: 70 / 255, blue: 77 / 255, alpha: 1.0)]
        self.view.backgroundColor = UIColor.black
        self.navigationController?.toolbar.barTintColor = UIColor(red: 161 / 255, green: 164 / 255, blue: 166 / 255, alpha: 1.0)
        
        let ip = IndexPath(row: 0,section: 0)
        tableView.selectRow(at: ip, animated: false, scrollPosition: .none)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        
        // See https://github.com/sascha/DrawerController/issues/12
        self.navigationController?.view.setNeedsLayout()
        
        let integersRange = NSRange(location: 0, length: self.tableView.numberOfSections - 1)
        self.tableView.reloadSections(IndexSet(integersIn: Range(integersRange) ?? 0..<0), with: .none)
    }
    
    func contentSizeDidChange(_ size: String) {
        // or override in subclass
        self.tableView.reloadData()
    }
    
    // MARK: - UITableViewDataSource
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 2
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        switch section {
        case NavigationState.Section.hub.rawValue:
            return self.navState.navDrawerItems == nil ? 6 : self.navState.navDrawerItems!.count
        case NavigationState.Section.documentation.rawValue:
            return NavigationState.docsNavItems.count
        default:
            return 0
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let CellIdentifier = "Cell"
        
        var cell: UITableViewCell! = tableView.dequeueReusableCell(withIdentifier: CellIdentifier) as UITableViewCell?
        
        if cell == nil {
            cell = DrawerTableViewCell(style: .default, reuseIdentifier: CellIdentifier)
            cell.selectionStyle = .blue
        }
        
        let sect = (indexPath as NSIndexPath).section
        let idx = (indexPath as NSIndexPath).row
        if (sect == NavigationState.Section.hub.rawValue) {
            cell.textLabel?.text = self.navState.navDrawerItems![idx].label
            cell.imageView?.image = UIImage(named: self.navState.navDrawerItems![idx].icon)
            if (self.navState.selectedDrawerItem != nil && self.navState.getDrawerLabels().index(of: self.navState.selectedDrawerItem!.label) == idx) {
                cell.textLabel?.textColor = UIColor(red:0.00, green:0.47, blue:0.42, alpha:1.0)
                cell.textLabel?.font = UIFont.boldSystemFont(ofSize: cell.textLabel!.font.pointSize)
            } else {
                cell.textLabel?.textColor = UIColor.black
                cell.textLabel?.font = UIFont.systemFont(ofSize: cell.textLabel!.font.pointSize)
            }
        }
        else if (sect == NavigationState.Section.documentation.rawValue) {
            cell.textLabel?.text = NavigationState.docsNavItems[idx].label
            cell.imageView?.image = UIImage(named: NavigationState.docsNavItems[idx].icon)
            cell.textLabel?.textColor = UIColor.black
            cell.textLabel?.font = UIFont.systemFont(ofSize: cell.textLabel!.font.pointSize)
        }
        
        return cell
    }
    
    func tableView(_ tableView: UITableView, titleForHeaderInSection section: Int) -> String? {
        switch section {
            case NavigationState.Section.documentation.rawValue:
                return "Documentation"
            default:
                return nil
        }
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let headerView = DrawerSectionHeaderView(frame: CGRect(x: 0, y: 0, width: tableView.bounds.width, height: 56.0))
        headerView.autoresizingMask = [ .flexibleHeight, .flexibleWidth ]
        headerView.title = tableView.dataSource?.tableView?(tableView, titleForHeaderInSection: section)
        if (headerView.title == nil) {
            let logo = UIImage(named: "logo")
            let logoView = UIImageView(image: logo)
            logoView.layoutMargins.top = 20
            logoView.frame = CGRect(x: 10, y: 24, width: 80, height: 65)
            headerView.addSubview(logoView)
        }
        
        return headerView
    }
    
    func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat {
        return section == 0 ? 100 : 50
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 40
    }
    
    func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        return 0
    }
    
    // MARK: - UITableViewDelegate
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let sect = (indexPath as NSIndexPath).section
        let idx = (indexPath as NSIndexPath).row
        var labels = [String]()
        if (sect == NavigationState.Section.hub.rawValue) {
            labels = self.navState.getDrawerLabels()
            self.navState.selectedDrawerItem = self.navState.getDrawerItem(label: labels[idx])
            tableView.selectRow(at: indexPath, animated: false, scrollPosition: .none)
            self.navState.delegate?.load(self.navState.url(), title: self.navState.selectedDrawerItem!.label, closeDrawer: true)
        }
        else if (sect == NavigationState.Section.documentation.rawValue) {
            for docsNavItem in NavigationState.docsNavItems {
                labels.append(docsNavItem.label)
            }
            self.navState.delegate?.link(NavigationState.docsNavItems[idx].url, closeDrawer: true)
        }
    }
}

