# AA

Our prototype is written in **Kotlin** with **Jetpack Compose**, with **Android**(API-Level 35) being the target OS.
For being able to build/run this code you have to download and install the latest **Android Studio**(https://developer.android.com/studio).
Then open the project and **sync**(the IDE will hint you to do so) the project, **grade** will (re-)fetch versioned dependencies/packages from defined repositories(e.g. maven central).

For a full list of dependencies see files named: **build.grade.kts**

To actually run/debug the prototype you additionally have to add a 'virtual device' in the 'device manager'(Android Studio).
Specifications of the virtual device:
Whereby a virtual **Pixel 6** and **Pixel 9 Pro** where used for testing, whereby the prior should be preferred.

Further we utilise 2 offline models for segmentation[deeplab_v3.tflite]{https://www.kaggle.com/models/tensorflow/deeplabv3/tfLite} and classification [efficientnet_lite0.tflite]{https://blog.tensorflow.org/2020/03/higher-accuracy-on-vision-models-with-efficientnet-lite.html}, the colour is inferred from the masked image.

A few **restrictions** apply:
Source-Images may neither be too large nor have exotic colour profiles embedded, as this may cause underlying issues in the pipeline.
Certain settings on the OS level may interfere with execution of the app.
Supplied Images must be reasonable sized, else there might be a memory issue!
Further, resolution of images may not be too big/small, else pixelation occurs and classification might have an issue.

Due to the Architecture and to meet expectations regarding UI, the use-cases(which area partially overlapping / complements) are streamlined into a modern User-Experience.

The **DOCUMENTATION** folder contains all created documents merged into a single file
The **VIDEO** folder contains the 'presentation'

