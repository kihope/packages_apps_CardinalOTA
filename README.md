CardinalOTA
-------
A very simple OTA checker with Android Settings look and feel.

How it works
------------
It parses the OTA xml file that you put in your file hosting and compares the version number with the local one.
If the version is newer, it notifies the user for a new ROM update.

How to use
----------
* Prepare the OTA xml file. Use this [template](https://raw.githubusercontent.com/Cardinal-AOSP/packages_apps_CardinalOTA/oreo/examples/ota_oreo.xml).
* Upload it to your file hosting and create a hot link of it
* Copy the [ota_conf template](https://raw.githubusercontent.com/Cardinal-AOSP/packages_apps_CardinalOTA/oreo/examples/ota_conf) to app/src/main/assets folder
  * If you are buiding this app as part of the ROM, you need to copy ota_conf in the android root folder.
  * The Android.mk will pick it up and copy it to app/src/main/assets folder automatically.
* Replace the "ota_url" with your OTA xml hot link
* Define how CardinalOTA should know about the "version". The version must be parseable to a date.
  * Usually, the version is a part of a build name. For example, the 20170919 in the Cardinal-AOSP-5.0-STAGING-OREO-kenzo-UNOFFICIAL-20170917.
* Adjust the OTA configuration according to your build name on how should CardinalOTA parse the version
  * Find a key in build.prop that represents the Cardinal-AOSP-5.0-STAGING-OREO-kenzo-UNOFFICIAL-20170917 and set it in the "version_name"
  * Set the delimiter in "version_delimiter" to "-"
  * Set the date format in "version_format" to "yyyyMMdd"
  * Set the position in "version_position" to "7" (zero based)
* Find a key in build.prop that represents your device name and set it in the "device_name"
  * CardinalOTA will search this device name in the OTA xml file

How to build
------------
* As part of the ROM
  * [Clone this repo]
  * `git clone https://github.com/Cardinal-AOSP/packages_apps_CardinalOTA -b oreo packages/apps/CardinalOTA`
  * [Include this app in the build process]
  * on device.mk add `PRODUCT_PACKAGES += CardinalOTA`

* As a standalone app
  * With Android.mk: make CardinalOTA

* With Android Studio: Import this repo to your Android Studio and build it from there
  
Credits
-------
* [Slim team](http://slimroms.net/)
  * For the original idea of the SlimCenter and app icon
* [CommonsWare Android Components](https://github.com/commonsguy/cwac-wakeful)
  * For the wakeful intent service that is used in this app
