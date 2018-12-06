### MDW for Devices

#### Android
TODO

#### iOS
 - Install cocoapods
   ```
   sudo gem install cocoapods
   pod setup
   ```
 - Install mdw dependencies
   ```
   cd mdw-mobile/ios/mdw
   pod install
   (ignore post-install errors)
   ```

#### Layout
Max Nav Links (same for Android and iOS)

  Device          | Portrait       |Landscape       |
  ----------------|:---------------|:---------------|
  Phone           | 3              | 5              |
  Tablet          | 5              | 7              |

In nav.json, do not declare more than three links with `"priority": 1`,
nor more than five links with `"priority": 2` for a good UX.
