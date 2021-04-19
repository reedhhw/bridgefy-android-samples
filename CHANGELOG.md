![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=http%3A%2F%2F104.196.228.98%3A8081%2Fartifactory%2Flibs-release-local%2Fcom%2Fbridgefy%2Fandroid-sdk%2Fmaven-metadata.xml)

# Changelog
All notable changes to this project will be documented in this file.

## [2.0.1] - 2021-01-13
### Changed
- Data transfer with maximum 2M, **only in direct messages**
- Minor bug fixes and improvements

## [2.0.0] - 2020-11-20
### Changed
- Implementation of Signal Protocol
- Encrypt all the payload using Signal
- Implemented a certificate mechanism to encrypt all mesh communications

#### * 2.0.0 is not compatible with previous versions

---

## [1.1.28] - 2020‑12‑10
### Changed
- bug fixed: SocketTimeoutException cannot be cast to BridgefyException

## [1.1.27] - 2020‑11‑09
### Changed
- bug fixes: sending mesh messages on automatic mode & retry configuration SDK

## [1.1.26] - 2020‑10‑13
### Changed
- Encrypt all content with AES algorithm

## [1.1.25] - 2020‑04‑28
### Changed
- Now you can use the automatic or on-demand mode
- Connectivity algorithm optimization

## [1.1.24] - 2019‑06‑20
### Changed
- Bridgefy reach profile

## [1.1.23] - 2019‑06‑03
### Changed
- Fixed a initialization issue

## [1.1.22] - 2019‑04‑08
### Changed
- Licence preloaded

## [1.1.21] - 2018-12-04
### Changed
- Minor bug fixes and improvements

## [1.1.20] - 2018-11-29
### Changed
- Fixed a concurrency issue

## [1.1.19] - 2018-11-7
### Changed
- Migrated internal dependencies to AndroidX

## [1.1.18] - 2018-10-25
### Changed
- Minor bug fixes and improvements

## [1.1.17] - 2018-10-24
### Changed
- Fixes a bug related to the integration of RX-Java subscriptions

## [1.1.16] - 2018-10-23
### Changed
- Minor bug fixes and improvements

## [1.1.15] - 2018-10-18
### Changed
- Fixed a bug that would cause a periodic crash under rare circumstances

## [1.1.14] - 2018-10-17
### Changed
- Minor bug fixes and improvements

## [1.1.13] - 2018-10-12
### Changed
- Fixed a crash that involved resetting Bluetooth
- Fixed a bug with the connection decision algorithm
### Added
- A new callback on StateListener that warns when a device has been permanently blacklisted from connections


## [1.1.12] - 2018-09-17
### Changed
- Fixed a crash that involved stopping Bridgefy while connections were active


## [1.1.11] - 2018-07-09
### Changed
- Added a new profile BFConfigProfileShortReach
- onMessageSent(Message) has been deprecated in favor of onMessageSent(String)


## [1.1.10] - 2018-06-04
### Changed
- Fixed an connectivity issue involving Android Things and the Raspberry Pi


## [1.1.9] - 2018-04-22
### Changed
- Added hops field to Message.
- Fixed a threading issue.


## [1.1.8] - 2018-02-28
### Changed
- Added a new profile for messages.


## [1.1.7] - 2018-03-05
### Changed
- Solved a bug that would crash devices with Bluetooth off under certain circumstances


## [1.1.6] - 2018-02-28
### Changed
- Devices running Android Things are properly started now.


## [1.1.5] - 2018-01-22
### Changed
- Solved a bug that caused errors when receiving binary data from iOS devices

## [1.1.4] - 2018-01-22
### Changed
- Solved a bug that caused errors when sending binary data between Android and iOS

## [1.1.3] - 2018-01-16
### Added
- Added the new BFBleProfile class to handle Bluetooth 5 compatibility


## [1.1.2] - 2018-01-08
### Added
- Added the new engine profile BFConfigProfileNoFowarding

### Changed
- Methods for sending message in the Bridgefy class won't throw exceptions in case Bridgefy hasn't been initialized.
They will now return null.
- Fixed a bug that caused messages to be duplicated under certain circumstances.


## [1.1.1] - 2017-12-12
### Added
- Fixed a connectivity issue in Bluetooth LE mode


## [1.1.0] - 2017-12-06
### Added
- In addition to Bluetooth LE, there is now a Bluetooth Classic mode
- Deprecated Bridgefy.createMessage() methods. Use Message.Builder class to create Message Objects
- Deprecated ConnectionType enum. Use Config.Antenna instead


