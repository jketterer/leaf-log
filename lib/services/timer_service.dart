import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:leaf_log/models/color_maps.dart';
import 'dart:async';
import 'package:leaf_log/models/tea.dart';
import 'package:audioplayers/audio_cache.dart';
import 'package:preferences/preferences.dart';
import 'package:screen/screen.dart';

class TimerService extends ChangeNotifier {
  Timer _timer;
  Duration _currentDuration = Duration.zero;
  Duration _initialDuration = Duration(seconds: 1);
  bool _timerStarted = false;
  bool _timerExpired = false;
  Tea _currentTea;

  FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin;

  static AudioCache player = AudioCache();

  TimerService() {
    flutterLocalNotificationsPlugin = new FlutterLocalNotificationsPlugin();
    // initialise the plugin. app_icon needs to be a added as a drawable resource to the Android head project
    var initializationSettingsAndroid =
        new AndroidInitializationSettings('app_icon');
    var initializationSettingsIOS = new IOSInitializationSettings(
        onDidReceiveLocalNotification: onDidReceiveLocalNotification);
    var initializationSettings = new InitializationSettings(
        initializationSettingsAndroid, initializationSettingsIOS);
    flutterLocalNotificationsPlugin.initialize(initializationSettings,
        onSelectNotification: onSelectNotification);
  }

  Duration get currentDuration => _currentDuration;
  bool get timerExpired => _timerExpired;
  set timerExpired(bool) => _timerExpired;
  set initialDuration(duration) => _initialDuration;
  Duration get initialDuration => _initialDuration;
  Tea get currentTea => _currentTea;

  // This setter needs to be explicit to avoid flutter throwing a "problem"
  void setCurrentTea(Tea tea) {
    _currentTea = tea;
  }

  bool isRunning() {
    return _timerStarted;
  }

  // Called each second timer is running
  void _onTick(Timer timer) {
    _currentDuration = _currentDuration - Duration(seconds: 1);
    if (_currentDuration == Duration.zero) {
      stop();
      _timerExpired = true;
      player.play("alarm_sound.mp3");
      //_showNotification();
    }

    notifyListeners();
  }

  // Start the timer
  void start() {
    // Don't start the timer if duration is 0
    if (_timer != null || _currentDuration.inSeconds == 0) return;

    _timerStarted = true;
    _timer = Timer.periodic(Duration(seconds: 1), _onTick);
    _scheduleNotification(_currentDuration);

    Screen.keepOn(true);

    notifyListeners();
  }

  // Pause/stop the timer
  void stop() {
    if (_timer == null) return;
    _timer.cancel();
    _timer = null;

    // Clear scheduled notification if timer is paused
    if (_currentDuration.inSeconds > 0) _clearNotification();

    Screen.keepOn(false);

    notifyListeners();
  }

  // Resume the timer
  void resume() {
    if (_timer != null) return;
    start();

    notifyListeners();
  }

  // Reset the timer
  void reset() {
    stop();
    _timerStarted = false;
    _timerExpired = false;
    _currentDuration = Duration.zero;
    _initialDuration = Duration(seconds: 1);
    _currentTea = null;

    _clearNotification();

    notifyListeners();
  }

  // Add time to the timer
  void addTime(int seconds) {
    _currentDuration += Duration(seconds: seconds);

    // Don't let duration increase above 60 minutes
    if (_currentDuration > Duration(minutes: 60))
      _currentDuration = Duration(minutes: 60);

    // Set or add time to initial duration
    if (_initialDuration.inSeconds == 1)
      _initialDuration = Duration(seconds: seconds);
    else if (_currentDuration < Duration(minutes: 60))
      _initialDuration += Duration(seconds: seconds);

    notifyListeners();
  }

  // Reduce time from the timer
  void reduceTime(int seconds) {
    _currentDuration -= Duration(seconds: seconds);

    // Don't let duration decrease below 0
    if (_currentDuration < Duration.zero) {
      // If user tries to reduce time below 0 while timer is running, cancel the action
      if (isRunning()) {
        _currentDuration += Duration(seconds: seconds);
        return;
      }

      _currentDuration = Duration.zero;
    }

    // Reset or reduce time from initial duration
    if (_initialDuration - Duration(seconds: seconds) <= Duration.zero)
      _initialDuration = Duration(seconds: 1);
    else if (!isRunning()) // Only reduce initial duration if timer is not running
      _initialDuration -= Duration(seconds: seconds);

    // Schedule notification for new time
    _scheduleNotification(_currentDuration);

    notifyListeners();
  }

  // iOS specific
  Future<void> onDidReceiveLocalNotification(
      int i, String s1, String s2, String s3) {
    return null;
  }

  // Called when user taps notification
  Future<dynamic> onSelectNotification(String payload) {
    reset();
    return null;
  }

  // Schedule a notification to go off in the future
  Future<void> _scheduleNotification(Duration duration) async {
    var scheduledNotificationDateTime = DateTime.now().add(duration);
    var androidPlatformChannelSpecifics = AndroidNotificationDetails(
        'channel-leaf-log', 'channel-leaf-log', 'Notifications from Leaf Log',
        importance: Importance.High, priority: Priority.High, ticker: 'ticker');
    var iOSPlatformChannelSpecifics = IOSNotificationDetails();
    var platformChannelSpecifics = NotificationDetails(
        androidPlatformChannelSpecifics, iOSPlatformChannelSpecifics);
    await flutterLocalNotificationsPlugin.schedule(
        0, 'Tea Timer', 'Your tea is done brewing! Tap to reset timer.', scheduledNotificationDateTime, platformChannelSpecifics);
  }

  Future <void> _clearNotification() async {
    await flutterLocalNotificationsPlugin.cancel(0);
  }

  // Provide class to entire widget tree
  static TimerService of(BuildContext context) {
    var provider = context.inheritFromWidgetOfExactType(TimerServiceProvider)
        as TimerServiceProvider;
    return provider.service;
  }
}

class TimerServiceProvider extends InheritedWidget {
  const TimerServiceProvider({Key key, this.service, Widget child})
      : super(key: key, child: child);

  final TimerService service;

  @override
  bool updateShouldNotify(TimerServiceProvider old) => service != old.service;
}

// Widget displays a small timer
class FloatingTimer extends StatelessWidget {
  FloatingTimer({this.style});
  final TextStyle style;

  @override
  Widget build(BuildContext context) {
    TimerService timerService = TimerService.of(context);

    return AnimatedBuilder(
      animation: timerService,
      builder: (context, child) {
        return Container(
          alignment: Alignment.bottomCenter,
          padding: EdgeInsets.all(10),
          child: timerService.isRunning()
              ? Card(
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(15, 10, 15, 10),
                    child: TimeDisplay(
                      timerService: timerService,
                      style: style,
                    ),
                  ),
                  color: timerService.currentTea != null
                      ? ColorMaps.getTypeColor(timerService.currentTea.type)
                      : ColorMaps
                          .themeColors[PrefService.getString("theme_color")])
              : null,
        );
      },
    );
  }
}

// Widget displays the current time in a readable format
class TimeDisplay extends StatelessWidget {
  TimeDisplay({this.timerService, this.style = const TextStyle()});
  TimeDisplay.styled({this.timerService, this.style});
  final TimerService timerService;
  final TextStyle style;

  String formatTime(int seconds) {
    if (timerService._timerExpired) {
      return "Done!";
    }

    int minutes = 0;

    // Logic to convert seconds to a M:SS format
    while (seconds >= 60) {
      seconds -= 60;
      minutes++;
    }

    return minutes.toString() + ":" + seconds.toString().padLeft(2, '0');
  }

  @override
  Widget build(BuildContext context) {
    return Text(
      formatTime(timerService.currentDuration.inSeconds),
      style: style,
    );
  }
}
