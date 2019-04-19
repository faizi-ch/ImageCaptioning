An Android application which converts camera feed to natural language captions in real time.
The app uses our customized pre-trained model generated through [image-caption-generator](https://github.com/neural-nuts/image-caption-generator). 
Using this model the app takes **1-2 second(s)** to caption a live camera frame on Huawei Honor 6x.



## Software Pre-Requisites
1. Android-Sdk for > Kitkat
2. Android-Studio
3. Tensorflow Java Library
    - Already provided build #44 in this repository. Latest nightly builds can be obtained frome [here](https://ci.tensorflow.org/view/Nightly/job/nightly-android/)
    - **Warning**: Did not test this app with builds other that #44

## Data Pre-Requisites
1. Trained model 
2. Word IDs to Word map pickle from [image-caption-generator](https://github.com/neural-nuts/image-caption-generator) currently provided in `Application/src/main/assets`

## Instructions
To build this app for your android phone-
1. Clone this repository
2. Download the trained model
3. Add the downloaded pre-trained model to `Application/src/main/assets` folder in the repository.
4. Open the repository in Android Studio
5. Build the app on your device using Android Studio


## Preview
Here is a quick preview of the app which was made by pointing the device camera towards a slideshow running on a screen and some real-life scenes. 
#TO-DO: Create a real preview by testing the app on streets.

<a href="url"><img src="https://github.com/neural-nuts/Cam2Caption/blob/master/preview.gif" align="left" height="600" width="350" ></a>

```

