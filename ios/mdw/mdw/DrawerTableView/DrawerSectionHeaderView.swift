//
//  DrawerSectionHeaderView.swift
//  mdw
//

import UIKit

class DrawerSectionHeaderView: UIView {
    var title: String? {
        didSet {
            self.label?.text = self.title?.uppercased()
        }
    }
    fileprivate var label: UILabel!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.commonSetup()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        self.commonSetup()
    }
    
    func commonSetup() {
        self.backgroundColor = UIColor(red:0.00, green:0.47, blue:0.42, alpha:1.0)
        self.label = UILabel(frame: CGRect(x: 15, y: self.bounds.maxY - 28, width: self.bounds.width - 30, height: 22))
        self.label.font = UIFont.preferredFont(forTextStyle: UIFontTextStyle.caption1)
        self.label.backgroundColor = UIColor.clear
        self.label.textColor = UIColor(red: 203 / 255, green: 206 / 255, blue: 209 / 255, alpha: 1.0)
        self.label.autoresizingMask = [ .flexibleWidth ,.flexibleTopMargin ]
        self.addSubview(self.label)
        
        self.clipsToBounds = false
    }
    
    override func draw(_ rect: CGRect) {
        if let context = UIGraphicsGetCurrentContext() {
            let lineColor = UIColor(red: 94 / 255, green: 97 / 255, blue: 99 / 255, alpha: 1.0)
            context.setStrokeColor(lineColor.cgColor)
            context.setLineWidth(1.0)
            context.move(to: CGPoint(x: self.bounds.minX, y: self.bounds.maxY - 0.5))
            context.addLine(to: CGPoint(x: self.bounds.maxX, y: self.bounds.maxY - 0.5))
            context.strokePath()
        }
    }
}

