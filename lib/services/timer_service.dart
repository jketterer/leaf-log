import 'package:flutter/material.dart';
import 'dart:async';
import 'package:leaf_log/models/tea.dart';
import 'package:audioplayers/audio_cache.dart';

class TimerService extends ChangeNotifier {
  Timer _timer;
  Duration _currentDuration = Duration.zero;
  bool _timerStarted = false;
  bool _timerExpired = false;
  Tea _currentTea;

  static AudioCache player = AudioCache();

  Duration get currentDuration => _currentDuration;
  bool get timerExpired => _timerExpired;
  set timerExpired(bool) => _timerExpired;
  set currentTea(Tea tea) => _currentTea = tea;
  Tea get currentTea => _currentTea;

  bool isRunning() {
    return _timerStarted;
  }

  void _onTick(Timer timer) {
    _currentDuration = _currentDuration - Duration(seconds: 1);
    if (_currentDuration == Duration.zero) {
      stop();
      _timerExpired = true;
      player.play("alarm_sound.mp3");
    }

    notifyListeners();
  }

  void start([Duration duration]) {
    if (duration != null) _currentDuration = duration;
    if (_timer != null || _currentDuration.inSeconds == 0) return;

    _timerStarted = true;
    _timer = Timer.periodic(Duration(seconds: 1), _onTick);

    notifyListeners();
  }

  void stop() {
    if (_timer == null) return;
    _timer.cancel();
    _timer = null;

    notifyListeners();
  }

  void resume() {
    if (_timer != null) return;
    start();
    _timerStarted = true;

    notifyListeners();
  }

  void reset() {
    stop();
    _timerStarted = false;
    _timerExpired = false;
    _currentDuration = Duration.zero;
    _currentTea = null;

    notifyListeners();
  }

  void addTime(int seconds) {
    _currentDuration = _currentDuration + Duration(seconds: seconds);

    notifyListeners();
  }

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
                  color: Colors.lightGreen,
                )
              : null,
        );
      },
    );
  }
}

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

    return minutes.toString() +
        ":" +
        seconds.toString().padLeft(2, '0');
  }

  @override
  Widget build(BuildContext context) {
    return Text(
      formatTime(timerService.currentDuration.inSeconds),
      style: style,
    );
  }
}
