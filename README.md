# Innometrics android client
### Dashboard part:
Supports some functionality of a dashboard (http://188.130.155.78:8000/#/dashboard/project/all/general/): register & login, profile page, displaying activities and some metrics.  
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/login.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/register.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/metrics.jpg" width="280">
Offline. Shows old local data. To download new data a user has to press sync button in action menu.  
Metrics are listed in Metrics section. The user can see information about any metric pressing downarrow on it. Clicking a metric opens the metric in details:
 - Can display metric data, which value type is an integer, in an interactive graph. The graph takes a full-screen size if it is in landscape mode. 
 - Can display metrics with name "url" in a PieChart in a form of a top visited domain names. The number of slices is hardcoded.  
 <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/int_metrics_portrait.jpg" width="280">
 <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/int_metrics_landscape.jpg" width="498">
 <img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/urls.jpg" width="280">
### Agent part:
Can collect data: package name of currently used app and location.  
Both need special permissions. Can be enabled or disabled in the app in tracking settings. The app asks the user to enable permissions dynamically if there is no need.  
Agent part is realized in Track section. It has:
- "Start" button, which starts tracking services and transforms to "Stop" button if services are running. To stop service press stop or close the app from applications stack (which doesn't work in some android versions and the user has only the first option).
- Right ImageButton is for tracking settings, where the user can enable/disable tracking each type of data.
- Left ImageButton is for jumping to a page, where the user can see collected data. The user can clear collected data or upload and clear. These 2 actions are part of an action bar.
- Action bar has extra button "Info" which shows Dialog with Tracking section explanation.  
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/tracking_fragment.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/apps.jpg" width="280">
<img src="https://github.com/InnopolisUniversity/innometrics-android-agent/blob/master/images/locatins.jpg" width="280">
