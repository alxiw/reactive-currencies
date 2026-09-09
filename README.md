# Reactive Currencies

Android app for converting currencies using official exchange rates from the Central Bank of Russia, covering more than 50 currencies.

The app is built around the Repository pattern, with a Room database used for local storage to keep the app fully functional offline once rates have been fetched. The presentation layer follows MVVM. Users can select any currency as the base and instantly see recalculated exchange values for all other currencies based on the entered amount.

<img src="img/sample.png" alt="drawing" width="200"/>

## Dependencies

* [Kotlin](https://github.com/JetBrains/kotlin) 2.4.20
* [RxJava](https://github.com/ReactiveX/RxJava) 3.1.12
* [Retrofit](https://github.com/square/retrofit) 3.0.0
* [Room](https://developer.android.com/training/data-storage/room) 2.8.5

## License

[MIT](LICENSE) © [alxiw](https://github.com/alxiw)
