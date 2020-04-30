
# react-native-ssl-pinning

React-Native Ssl pinning using OkHttp 3 in Android, and AFNetworking on iOS. 
## Getting started

`$ npm install react-native-ssl-pin --save`

### Mostly automatic installation

`$ react-native link react-native-ssl-pin`

### Manual installation


#### iOS is not working yet

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-ssl-pinning` and add `RNSslPinning.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNSslPinning.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.hosseinmd.RNSslPinningPackage;` to the imports at the top of the file
  - Add `new RNSslPinningPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-ssl-pin'
  	project(':react-native-ssl-pin').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-ssl-pin/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-ssl-pin')
  	```


## Usage

#### Create the certificates:
1. openssl s_client -showcerts -connect google.com:443 (replace google with your domain)

2. Copy the certificate (Usally the first one in the chain), and paste it using nano or other editor like so , nano mycert.pem
3. convert it to .cer with this command
openssl x509 -in mycert.pem -outform der -out mycert.cer 

#### iOS is not working yet
 - drag mycert.cer to Xcode project, mark your target and 'Copy items if needed'

#### Android
 -  Place your .cer files under src/main/assets/.
```javascript
import {fetch, SSLPinning, cancel, removeCookieByName} from 'react-native-ssl-pin';

 SSLPinning(url, { trust: true })
//or
 SSLPinning(url, { certs: [ "cert1", "cert2" ] })

await fetch(url, {
	method: "POST" ,
	timeoutInterval: communication_timeout,
	body: body,
	headers: {
		Accept: "application/json; charset=utf-8", "Access-Control-Allow-Origin": "*", "e_platform": "mobile",
	}
	tag:"cancel_tag",
})

await cancel("cancel_tag")

removeCookieByName('cookieName')
	.then(res =>{
		    console.log('removeCookieByName');
	})

getCookies('domain')
	.then(cookies => {
		// do what you need with your cookies
	})

```
## Multipart request (FormData)

```javascript
let formData = new FormData()

//You could add a key/value pair to this using #FormData.append:

formData.append('username', 'Chris');

// Adding a file to the request
formData.append('file', {
		name: encodeURIComponent(response.fileName),
		fileName: encodeURIComponent(response.fileName),
		type: this._extractFileType(response.fileName),
		uri: response.uri,
		data: response.data // needed for ios in base64
})

await fetch(url, {
	method: "POST" ,
	timeoutInterval: communication_timeout,
	body: {
		formData,
	},

	headers: {
		'content-type': 'multipart/form-data; charset=UTF-8',
		accept: 'application/json, text/plain, /',
	}
})

```

## License
This project is licensed under the terms of the MIT license.
