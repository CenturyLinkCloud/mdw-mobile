//
//  AppDelegate.swift
//  mdw
//

import UIKit
import DrawerController

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?
    var drawerController: DrawerController!
    
    	
    func application(_ application: UIApplication, willFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        
        drawerController = window?.rootViewController! as! DrawerController
        drawerController.showsShadows = true
        
        drawerController.restorationIdentifier = "Drawer"
        drawerController.maximumRightDrawerWidth = 200.0
        drawerController.openDrawerGestureModeMask = .all
        drawerController.closeDrawerGestureModeMask = .all
        
        drawerController.drawerVisualStateBlock = { (drawerController, drawerSide, fractionVisible) in
            let block = DrawerVisualStateManager.sharedManager.drawerVisualStateBlock(for: drawerSide)
            block?(drawerController, drawerSide, fractionVisible)
        }
        
        let storyBoard : UIStoryboard = UIStoryboard(name: "Main", bundle:nil)
        window?.rootViewController = storyBoard.instantiateViewController(withIdentifier: "splash")
        NavigationState.instance.load { error -> Void in
            if let err = error {
                logError(err)
                //DispatchQueue.main.sync {
                    self.window?.rootViewController = storyBoard.instantiateViewController(withIdentifier: "login")
                //}
            }
            else {
                //DispatchQueue.main.sync {
                    self.window?.rootViewController = storyBoard.instantiateViewController(withIdentifier: "entry")
                //}
            }
        }
        return true
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool {
        window?.backgroundColor = UIColor.white
        window?.makeKeyAndVisible()
        
        return true
    }
    
    func application(_ application: UIApplication, shouldSaveApplicationState coder: NSCoder) -> Bool {
        return true
    }
    
    func application(_ application: UIApplication, shouldRestoreApplicationState coder: NSCoder) -> Bool {
        return true
    }
    
    func application(_ application: UIApplication, viewControllerWithRestorationIdentifierPath identifierComponents: [Any], coder: NSCoder) -> UIViewController? {
        if (!(window?.rootViewController is LoginViewController)) {
            if let key = identifierComponents.last as? String {
                if key == "Drawer" {
                    return window?.rootViewController
                } else if key == "DrawerController" {
                    return (window?.rootViewController as! DrawerController).leftDrawerViewController
                }
            }
        }
        
        return nil
    }
}

