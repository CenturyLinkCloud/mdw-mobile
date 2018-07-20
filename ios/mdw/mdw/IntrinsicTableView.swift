//
//  IntrinsicTableView.swift
//  mdw
//

import Foundation
import UIKit

class IntrinsicTableView: UITableView {
    
    override var contentSize: CGSize {
        didSet {
            self.invalidateIntrinsicContentSize()
        }
    }
    
    override var intrinsicContentSize: CGSize {
        self.layoutIfNeeded()
        var height = contentSize.height > 400 ? 400 : contentSize.height
        if (height > 50) {
            height = height - 50  // suppress bottom extra
        }
        
        return CGSize(width: UIViewNoIntrinsicMetric, height: height)
    }
    
}
