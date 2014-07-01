This application visualize feeds from the official site of Radio Blackout,
an autonomous radio located in Turin, Italy.

It is also possible to listen using the streaming, clicking on the play
button in the action bar.

For more related info about the radio look at the site

  http://www.radioblackout.org

CCOMPILING
----------

The dependencies are

  - Android SDK (Level8 API)    <http://developer.android.com>
  - ActionBarSherlock           <http://actionbarsherlock.com/>
  - Crouton                     <https://github.com/keyboardsurfer/Crouton>
  - Android RSS                 <https://github.com/ahorn/android-rss> included inside the project

The app uses the gradle build system and is updated to work with Android Studio (v0.8.1) but it's
possible to compile the app from command line using

    $ ./gradlew build

and install the application with

    $ adb -d install -r ./app/build/outputs/apk/app-debug.apk