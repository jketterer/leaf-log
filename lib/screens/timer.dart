import 'package:flutter/material.dart';
import 'package:flutter_sidekick/flutter_sidekick.dart';
import 'package:leaf_log/services/timer_service.dart';

class TimerPage extends StatefulWidget {
  @override
  _TimerPageState createState() => _TimerPageState();
}

class _TimerPageState extends State<TimerPage> with TickerProviderStateMixin {
// Controller for animated buttons
  AnimationController controller;

  bool isTimerRunning = false;
  bool isTimerPaused = false;

  void initState() {
    super.initState();
    // Inits the sidekick controller
    controller =
        AnimationController(vsync: this, duration: Duration(milliseconds: 500));
  }

  @override
  Widget build(BuildContext context) {
    // Get access to app-wide timer service
    TimerService timerService = TimerService.of(context);

    // Animated builder allows the timer to update on time change
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: <Widget>[
        AnimatedBuilder(
            // Listen to timerService for updates
            animation: timerService,
            builder: (context, child) {
              return Card(
                  elevation: 8,
                  color: Colors.lightGreen,
                  child: Padding(
                    padding: EdgeInsets.fromLTRB(35, 10, 35, 10),
                    child: TimeDisplay.styled(
                        timerService: timerService,
                        style: TextStyle(
                          fontSize: 80,
                          fontWeight: FontWeight.bold,
                        )),
                  ));
            }),
        timerService.currentTea != null
            ? Text(
                "Brewing: ${timerService.currentTea.name}",
                style: TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    fontStyle: FontStyle.italic),
              )
            : Text(""),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            RaisedButton(
              child: Text("+10s"),
              color: Colors.lightGreen,
              shape: CircleBorder(),
              padding: EdgeInsets.all(25),
              elevation: 8,
              onPressed: () {
                timerService.addTime(10);
              },
            ),
            RaisedButton(
              child: Text("+30s"),
              color: Colors.lightGreen,
              shape: CircleBorder(),
              padding: EdgeInsets.all(25),
              elevation: 8,
              onPressed: () {
                timerService.addTime(30);
              },
            ),
            RaisedButton(
              child: Text("+60s"),
              color: Colors.lightGreen,
              shape: CircleBorder(),
              padding: EdgeInsets.all(25),
              elevation: 8,
              onPressed: () {
                timerService.addTime(60);
              },
            )
          ],
        ),
        AnimatedBuilder(
          animation: controller,
          builder: (context, child) {
            return Stack(
              //mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                Positioned(
                  left: 80,
                  child: Sidekick(
                    tag: "Stop",
                    child: isTimerRunning
                        ? RaisedButton(
                            child: Text("Stop"),
                            color: Colors.lightGreen,
                            elevation: 8,
                            onPressed: () {
                              timerService.stop();
                            },
                          )
                        : Container(),
                  ),
                ),
                Center(
                  child: Sidekick(
                    tag: "Start",
                    targetTag: "Stop",
                    child: RaisedButton(
                        child: Text("Start"),
                        color: Colors.lightGreen,
                        elevation: 8,
                        onPressed: !isTimerRunning && !isTimerPaused
                            ? _startButtonPressed
                            : null),
                  ),
                ),
                Positioned(
                  left: 280,
                  child: Sidekick(
                    tag: "Reset",
                    child: isTimerRunning
                        ? RaisedButton(
                            child: Text("Reset"),
                            color: Colors.lightGreen,
                            elevation: 8,
                            onPressed: () {
                              _resetButtonPressed();
                            },
                          )
                        : Container(),
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  _startButtonPressed() {
    TimerService.of(context).start();
    isTimerRunning = true;
  }

  _resetButtonPressed() {
    TimerService.of(context).start();
    isTimerRunning = false;
    isTimerPaused = false;
  }
}
