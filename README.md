# Rooster Android Application Documentation
# Project Overview / Structure

## General

##### BaseApplication
##### Constants
*Constants* is a public class that holds public static final variables, for both configuration and utility. 

The following are examples of constants kept here:
+ Intent actions and extras
+ IntentFilter actions
+ BroadcastReceiver actions
+ Notifications IDs
+ BaseApplication flags

##### Domain
##### Dagger

## UI / UX

### Activity
#### BaseActivity
### Fragment
#### BaseFragment
### Adapter
### Resources

## Backend

### SQL Databases
##### Contract
##### Helper
##### Manager
##### Controller

#### AudioTable.db
#### DeviceAlarmTable.db

### BroadcastReceiver
>Base class for code that receives and handles broadcast intents sent by sendBroadcast(Intent).

>You can either dynamically register an instance of this class with Context.registerReceiver() or statically declare an implementation with the <receiver> tag in your AndroidManifest.xml.

#### BootReceiver
When the Android device is rebooted, any applications that have the *RECEIVE_BOOT_COMPLETED* permission and a receiver that implements the intent filter action *android.intent.action.BOOT_COMPLETED* will invoke the corresponding *BootReceiver* class that extends *BroadCastReceiver*. 

The *BootReceiver* instantiates a *DeviceAlarmController* object and calls the *rebootAlarms()* method. This method checks the persistant *DeviceAlarmTable* SQLite database for any enabled alarms and refreshes them - this recreates the aprropriate AlarmManager pending intents using the *DeviceAlarmController* *setAlarm()* method. 

The *BootReceiver* is also responsible for calling the *BackgroundTaskReceiver's* intent scheduling methods.

#### BackgroundTaskReceiver
The *BackgroundTaskReceiver*, using the BroadCastReceiver *onReceive()* method, receives an intent and starts a service with an attached action. 

The scheduling (and cancelling) of the pending intents is performed using methods of this class. The AlarmManager service is used to create an inexact repeating intent at a set time interval. The action of this intent is sent to the *BackgroundTaskIntentService* for processing.

#### DeviceAlarmReceiver
The *DeviceAlarmReceiver* receives intents set by the *DeviceAlarmController* using the Android AlarmManager. 

The following extras are processed by the receiver:
+ **EXTRA_UID** - the UID of the current alarm set 
+ **EXTRA_RECURRING** - boolean indicates whether alarm intent should be recreated a week later, or set to disabled
+ **EXTRA_VIBRATE** - boolean indicates whether android Vibrator service should be started
+ **EXTRA_TONE** - boolean indicates whether a default alarm tone was selected during alarm creation rather than social or channel content (not currently used)

The following extras are sent to the DeviceAlarmFullScreenActivity:
+ **DEVICE_ALARM_RECEIVER_WAKEFUL_INTENT** - Intent object that allows activity to call *completeWakefulIntent()* method
+ **EXTRA_UID** - the UID of the current alarm set, used to attach appropriate channel content to alarm and to send to *AudioService*

### Service
#### BackgroundTaskIntentService 
As described in the Diagrams section, the *BackgroundTaskIntentService* handles all periodic background tasks. 

General methods exist for managing the reception of intent actions and manual triggering of service tasks:
+ **Start methods** - called during special cases where the task should be run immediately, such as the *handleActionBackgroundDownload()* task after an alarm has been set or edited
+ **onHandleIntent()** - @Override method that receives service intents and processes the attached actions, to trigger one of the action handlers
+ **Action handlers** - call the necessary methods to complete the task
+ **Task methods** - methods called by the action handlers

The background tasks that the BackgroundTaskIntentService handles are:
+ **handleActionBackgroundDownload()** - retrieve channel and social audio data
+ **handleActionDailyTask()** - perform admin tasks run daily such as purging old audio files
+ **handleActionMinuteTask()** - perform update tasks run by the minute such as updating received rooster count and displaying badge notifications (the *FirebaseListenerService* is triggered by this action)

#### AudioService

#### UploadService

## Cloud

### NodeAPI
### Firebase

# Diagrams
![Diagram explainer](DiagramExplainer.png)

## BackgroundTaskIntentService
### Background audio caching task 
![Background audio caching task](BackgroundAudioFetchServices.png)

### Channel story algorithm
![Channel story algorithm](ChannelStoryAlgorithm.png)

## UploadService
![Upload service](UploadService.png)

## AudioService
![Audio service](AudioService.png)




