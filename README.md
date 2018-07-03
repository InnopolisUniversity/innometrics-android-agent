# Innometrics android client
Generally, Innometrics applications collect data (agent part) and some of them are displayed on the web dashboard (http://188.130.155.78:8000/#/dashboard/project/all/general/). This Android app can do a bit of both. 
### Dashboard part:
Supports some functionality of a dashboard: register & login, profile page, displaying activities and some metrics.  
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/login.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/register.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/metrics.jpg" width="280">
Offline. Shows old local data. To download new data a user has to press sync button in action menu.  
Metrics are listed in Metrics section. The user can see information about any metric pressing downarrow on it. Clicking a metric opens the metric in details:
 - Can display metric data, which value type is an integer, in an interactive graph. The graph takes a full-screen size if it is in landscape mode. 
 - Can display metrics with name "url" in a PieChart in a form of a top visited domain names. The number of slices is hardcoded.  
 <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/int_metrics_portrait.jpg" width="280"> <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/urls.jpg" width="280"> <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/int_metrics_landscape.jpg" width="498">
### Agent part:
Can collect data: package name of a currently used app and location.  
Both need special permissions. Can be enabled or disabled in the app in tracking settings. The app asks the user to enable permissions dynamically if they are off.  
Agent part is realized in Track section. It has:
- "Start" button, which starts tracking services and transforms to "Stop" button if services are running. To stop service press stop or close the app from applications stack (which doesn't work in some android versions and the user has only the first option).
- Right ImageButton is for tracking settings, where the user can enable/disable tracking each type of data.
- Left ImageButton is for jumping to a page, where the user can see collected data. The user can clear collected data or upload and clear. These 2 actions are part of an action bar.
- Action bar has an extra button "Info" which shows Dialog with Tracking section explanation.  
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/tracking_fragment.jpg" width="280"> <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/apps.jpg" width="280"> <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/locatins.jpg" width="280">   

#### More on tracking location
For now, the tracker logs latitude, longitude and logging time. It will log only if an accuracy is more than 20 meters (meaning you are inside a circle with radius 20) and if the user moved from a previous location at least 10 meters or the user pressed start button. The tracker will check location every minute unless you have android 8.0 and higher, which allows logging location few times in an hour in the background. If the data is not available (no GPS and no Internet), the agent will try to get location anyway and consume energy. LocationService class contains all the logic.
#### More on tracking foreground apps
The agent logs package name of an app that is currently in focus and time when it started. The agent checks every second but doesn't log if the previous app was the same. For example, when the user presses start button the first app to log is "com.example.innometrics" and its starting time is exactly when the user pressed the button. The user spends some time on Innometrics then decides to go to another app, then back to Innometrics. Collected data now contains 3 items. To understand how long the user spent on some app, one can subtract its starting time from the starting time of the following app. For this to work correctly the agent logs also the time when the user pressed the stop button and sleeping time (when the screen is on), like if they were just other apps. ForegroundAppService class contains all the logic.
#### Download APK (installer):
<a href="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/assets/innometrics.apk?raw=true" target="_blank">innometrics.apk</a>
