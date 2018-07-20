//
//  DrawerTableViewCell.swift
//  mdw
//

import UIKit

class DrawerTableViewCell: UITableViewCell {
    override init(style: UITableViewCellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        self.commonSetup()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.commonSetup()
    }
    
    func commonSetup() {
        let backgroundView = UIView(frame: self.bounds)
        backgroundView.autoresizingMask = [ .flexibleHeight, .flexibleWidth ]
        backgroundView.backgroundColor = UIColor.white
        
        self.backgroundView = backgroundView
        
        self.textLabel?.backgroundColor = UIColor.clear
        self.textLabel?.textColor = UIColor.black
        self.textLabel?.font = UIFont.preferredFont(forTextStyle: UIFontTextStyle.body)
    }
}

