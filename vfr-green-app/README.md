# Setup

## Node installation
From https://github.com/nodejs/help/wiki/Installation

Unzip the binary archive to any directory you wanna install Node, I use /usr/local/lib/nodejs

```
VERSION=v10.15.0
DISTRO=linux-x64
sudo mkdir -p /usr/local/lib/nodejs
sudo tar -xJvf node-$VERSION-$DISTRO.tar.xz -C /usr/local/lib/nodejs 
```

Set the environment variable ~/.profile, add below to the end
```
# Node
VERSION=v10.15.0
DISTRO=linux-x64
export PATH=/usr/local/lib/nodejs/node-$VERSION-$DISTRO/bin:$PATH
```

Refresh profile
```
. ~/.profile
```
Test installation using
```
node -v
npm version
npx -v
```

the normal output is:
```
➜  node -v
v10.15.1
➜  npm version
{ npm: '6.4.1',
 ares: '1.15.0',
 cldr: '33.1',
 http_parser: '2.8.0',
 icu: '62.1',
 modules: '64',
 napi: '3',
 nghttp2: '1.34.0',
 node: '10.15.1',
 openssl: '1.1.0j',
 tz: '2018e',
 unicode: '11.0',
 uv: '1.23.2',
 v8: '6.8.275.32-node.12',
 zlib: '1.2.11' }
```

## NativeScript installation
```
npm install -g nativescript
sudo apt update && sudo apt install android-sdk
```
https://docs.nativescript.org/start/ns-setup-linux

# Testing locally
Doing the quick preview on the device
```
tns preview
```
Doing the emulator on the computer
```
tns run android
tns run ios
```

# Testing cloud
```
tns login
tns cloud run android
```

# Future note
I need to include something in the output product's licence agreement that prevents them from decompiling the code and that they can be punished by the maximum legal amount.  
https://www.nativescript.org/docs/default-source/default-document-library/general-terms-of-service-for-progress-nativescript-sidekick-1-1-2019.pdf?sfvrsn=27c90dfe_2
