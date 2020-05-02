[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# CovidWatch Android Minimal App

Android app for clean and easy testing of the [TCN Protocol](https://github.com/TCNCoalition/tcn-client-android). A part of tha family of apps from www.covid-watch.org

## Who are we? What is this app?

This repository is focused on the implementation of a simple Android UI to robustly test integration with the [TCN Protocol](https://github.com/TCNCoalition/tcn-client-android) for the Covid Watch app. Our goals are to:
- Allow users to easily start and stop anonymously recording interactions with others using the [TCN Protocol](https://github.com/TCNCoalition/tcn-client-android).
- View these interactions easily on their screens

## Setup

### Dependencies

- Android Studio 3.6.x
  https://developer.android.com/studio

- TCN Client Android
  https://github.com/TCNCoalition/tcn-client-android

We are tracking this as a submodule for now tracking the develop branch, so don't forget to init and fetch.

First time:

```
$ git submodule update --init --recursive --remote
```

To Update:

```
git submodule update --remote
```

## Looking to contribute?

Reach out to @zssz or @Apisov

## FAQ

What is the anonymous protocol for communication between phones? How does it work and who designed it?

Covid Watch uses Temporary Contact Numbers, a decentralized, privacy-first contact tracing protocol developed by the [TCN Coalition](https://tcn-coalition.org/). This protocol is built to be extensible, with the goal of providing interoperability between contact tracing applications. You can read more about it on their [Github](https://github.com/TCNCoalition/TCN).

What's this repository vs the other repositories in the covid19risk Organization?

This is the repository for development of the front-facing Android mobile app for Covid Watch, including the UX and tie-ins to the TCN Bluetooth Protocol and backend services. Related repos:
- [Android Minimal:](https://github.com/covid19risk/covidwatch-android-minimal) Proof of concept pilot app for testing integrations with the bluetooth protocol.
- [TCN:](https://github.com/TCNCoalition/tcn-client-android) Implementation of bluetooth protocol.

## Contributors

- Zsombor Szabo (@zssz)
- Pavlo (@Apisov)

## Join the cause!

Interested in volunteering with Covid Watch? Check out our [get involved page](https://covid-watch.org/collaborate) and send us an email at contact@covid-watch.org!
