# React Native Sms
![logo](logo.png)

Native module for work with sms on android. Created for working with Telephony sms api as default sms manager.
*_AHTUNG_*: native module is currently unstable, because tests are not written and not all todo features are implemented. Use with caution.

# Features
* Get current status - "is default app"
* Recieve permission for setting current app as default sms app
* Get filtered sms list
* Send sms

## Installation
``` bash
yarn add https://github.com/friktor/react-native-sms
# or 
npm install git+https://github.com/friktor/react-native-sms --save
```

## Linking Module
Surely you would like to just write a command react-native link, and live happily, but ...
You need to edit your build configs and sources on your awesome editor or IDE.

_android/settings.gradle_
``` groovy
# .... another modules
include ':react-native-sms'
project(':react-native-sms').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-sms/android')
```

_android/app/build.gradle_
``` groovy
dependencies: {
  compile project(':react-native-sms')
  # ... other modules
}
```

_android/app/src/main/java/com/%appname%/MainApplication.java_
``` java
import com.friktor.reactnativesms.SmsPackage; // On top need add import module package

// far, far away....

@Override
protected List<ReactPackage> getPackages() {
  return Arrays.<ReactPackage>asList(
      new MainReactPackage(),
        new SmsPackage(), // Need add this line
        // ... your other packages
  );
}
```

## Need usage as default sms app?
[Example AndroidManifest.xml](samples/AndroidManifest.xml)

## Usage
``` js
import Sms from "react-native-sms"

// Get status
let isDefault = await Sms.isDefaultSmsApp()
console.log(isDefault) // Boolean

// Call for set as default sms manager
let callResult = await Sms.setAsDefaultApp()
// Void<>

// Get sms list
let list = await Sms.list({
  box: 'inbox', // 'inbox' (default), 'sent', 'draft', 'outbox', 'failed', 'queued', and ''

  // cursor chunk slice
  maxCount  : 10, // count of SMS to return each time
  indexFrom : 0, // start from index 0
    
  // the next 4 filters should NOT be used together, they are OR-ed so pick one
  address   : '+97433000000', // sender's phone number
  body      : 'Hello', // content
  _id       : 1234, // sms id
  read      : 0, // read stat
})

console.log(list) // Result: Array<Object> with Sms

// Send sms
let isSended = await Sms.send('*number phone*', '*your message*', '*type: sendDirect or sendIndirect*')
console.log(isSended) // Boolean, or promise exception :D
```

# TODO List
- [x] Understand how to write on kotlin for react-native
- [x] Send message
- [x] Sms list
- [x] Status app
- [x] Set app as default app
- [ ] Live Sms broadcast, with background workers
- [ ] Work with big sms db on separate thread
- [ ] Finish writing autolinking native modules written on Kotlin
- [ ] Work with mms (...maybe)
- [ ] UI and API sugar
- [ ] Write tests (shit, but needed)
- [ ] Write short example guide
- [ ] Add example app
- [ ] PROFIT!!!

# Thanks
Inspired by @rhaker [react-native-sms-android](https://github.com/rhaker/react-native-sms-android) and rewritten on Kotlin, with promises and sugar
