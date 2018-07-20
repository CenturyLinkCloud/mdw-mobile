//
//  DrawerVisualStateManager.swift
//  mdw
//

import UIKit
import DrawerController

enum DrawerAnimationType: Int {
    case none
    case slide
    case slideAndScale
    case swingingDoor
    case parallax
    case animatedBarButton
}

class DrawerVisualStateManager: NSObject {
    var leftDrawerAnimationType: DrawerAnimationType = .parallax
    var rightDrawerAnimationType: DrawerAnimationType = .parallax
    
    class var sharedManager: DrawerVisualStateManager {
        struct Static {
            static let instance: DrawerVisualStateManager = DrawerVisualStateManager()
        }
        
        return Static.instance
    }
    
    func drawerVisualStateBlock(for drawerSide: DrawerSide) -> DrawerControllerDrawerVisualStateBlock? {
        var animationType: DrawerAnimationType
        
        if drawerSide == DrawerSide.left {
            animationType = self.leftDrawerAnimationType
        } else {
            animationType = self.rightDrawerAnimationType
        }
        
        var visualStateBlock: DrawerControllerDrawerVisualStateBlock?
        
        switch animationType {
        case .slide:
            visualStateBlock = DrawerVisualState.slideVisualStateBlock
        case .slideAndScale:
            visualStateBlock = DrawerVisualState.slideAndScaleVisualStateBlock
        case .parallax:
            visualStateBlock = DrawerVisualState.parallaxVisualStateBlock(parallaxFactor: 2.0)
        case .swingingDoor:
            visualStateBlock = DrawerVisualState.swingingDoorVisualStateBlock
        case .animatedBarButton:
            visualStateBlock = DrawerVisualState.animatedHamburgerButtonVisualStateBlock
        default:
            visualStateBlock = { drawerController, drawerSide, fractionVisible in
                var sideDrawerViewController: UIViewController?
                var transform = CATransform3DIdentity
                var maxDrawerWidth: CGFloat = 0.0
                
                if drawerSide == .left {
                    sideDrawerViewController = drawerController.leftDrawerViewController
                    maxDrawerWidth = drawerController.maximumLeftDrawerWidth
                } else if drawerSide == .right {
                    sideDrawerViewController = drawerController.rightDrawerViewController
                    maxDrawerWidth = drawerController.maximumRightDrawerWidth
                }
                
                if fractionVisible > 1.0 {
                    transform = CATransform3DMakeScale(fractionVisible, 1.0, 1.0)
                    
                    if drawerSide == .left {
                        transform = CATransform3DTranslate(transform, maxDrawerWidth * (fractionVisible - 1.0) / 2, 0.0, 0.0)
                    } else if drawerSide == .right {
                        transform = CATransform3DTranslate(transform, -maxDrawerWidth * (fractionVisible - 1.0) / 2, 0.0, 0.0)
                    }
                }
                
                sideDrawerViewController?.view.layer.transform = transform
            }
        }
        
        return visualStateBlock
    }
}

