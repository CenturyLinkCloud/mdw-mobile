//
//  SplashViewController.swift
//  mdw
//

import UIKit

class SplashViewController: UIViewController {
    
    @IBOutlet weak var logoImage: UIImageView!
    @IBOutlet weak var loadingImage: UIImageView!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        let logo = UIImage(named: "logo")
        logoImage.image = logo

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
        loadingImage.startAnimating()

    }
}
