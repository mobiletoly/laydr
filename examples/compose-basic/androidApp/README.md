# Android App

Android entry point for the KMP Compose basic example.

This module owns Android packaging and the launcher activity only. The shared
route tree, generated Laydr graph, navigation state, and Compose UI stay in
`examples:compose-basic:shared`.

Build the debug app from the Laydr root with:

```sh
./gradlew :examples:compose-basic:androidApp:assembleDebug
```
