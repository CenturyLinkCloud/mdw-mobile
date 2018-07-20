//
//  Strings.swift
//  mdw
//

import Foundation

private extension OptionSet where Element == Self {
    /// Duplicate the set and insert the given option
    func inserting(_ newMember: Self) -> Self {
        var opts = self
        opts.insert(newMember)
        return opts
    }
    
    /// Duplicate the set and remove the given option
    func removing(_ member: Self) -> Self {
        var opts = self
        opts.remove(member)
        return opts
    }
}

extension String {
    
    var length: Int {
        get {
            return self.count
        }
    }
    
    func substring(_ startIndex: Int, length: Int) -> String {
        let start = self.index(self.startIndex, offsetBy: startIndex)
        let end = self.index(self.startIndex, offsetBy: startIndex + length)
        return String(self[start..<end])
    }
    
    public func indexRaw(of str: String, after: Int = 0, options: String.CompareOptions = .literal, locale: Locale? = nil) -> String.Index? {
        guard str.length > 0 else {
            // Can't look for nothing
            return nil
        }
        guard (str.length + after) <= self.length else {
            // Make sure the string you're searching for will actually fit
            return nil
        }
        
        let startRange = self.index(self.startIndex, offsetBy: after)..<self.endIndex
        return self.range(of: str, options: options.removing(.backwards), range: startRange, locale: locale)?.lowerBound
    }
    
    public func index(of str: String, after: Int = 0, options: String.CompareOptions = .literal, locale: Locale? = nil) -> Int {
        guard let index = indexRaw(of: str, after: after, options: options, locale: locale) else {
            return -1
        }
        return self.distance(from: self.startIndex, to: index)
    }
    
    func endsWith(_ suffix: String) -> Bool {
        return hasSuffix(suffix)
    }
}
