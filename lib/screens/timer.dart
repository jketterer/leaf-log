import 'package:flutter/animation.dart';
import 'package:flutter/material.dart';
import 'package:leaf_log/services/timer_service.dart';

class TimerPage extends StatefulWidget {
  @override
  _TimerPageState createState() => _TimerPageState();
}

class _TimerPageState extends State<TimerPage>
    with SingleTickerProviderStateMixin {
  // Controller for animated buttons
  AnimationController controller;
  // Separate animations for buttons
  Animation<Offset> stopButtonAnimation, resetButtonAnimation;

  // Flags to help us determine state of time
  bool isTimerRunning = false;
  bool isTimerPaused = false;

  // Keeps track of tea name after timer service resets
  String teaName = "";

  // Keeps track of original time for circular progress bar
  int initialTime = 1;

  final Map<String, Color> _typeColors = {
    "Green": Colors.lightGreen,
    "Black": Colors.brown[400],
    "Oolong": Colors.lime[500],
    "White": Colors.grey,
    "Herbal": Colors.pink[300],
    "Other": Colors.cyan[200]
  };

  void initState() {
    super.initState();
    // Inits the animation controller
    controller =
        AnimationController(vsync: this, duration: Duration(milliseconds: 200));

    // Creates the button animations
    stopButtonAnimation =
        Tween<Offset>(begin: Offset.zero, end: Offset(-0.2, 0.0))
            .animate(controller);
    resetButtonAnimation =
        Tween<Offset>(begin: Offset.zero, end: Offset(0.2, 0.0))
            .animate(controller);

    // Wait for animation to finish before making start button visible
    stopButtonAnimation.addStatusListener((AnimationStatus status) {
      if (status == AnimationStatus.dismissed) {
        setState(() {
          isTimerRunning = false;
          isTimerPaused = false;
        });
      }
    });
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Get access to app-wide timer service
    TimerService timerService = TimerService.of(context);

    Color mainColor = timerService.currentTea != null
        ? _typeColors[timerService.currentTea.type]
        : Colors.lightGreen;

    isTimerRunning = timerService.isRunning();

    if (isTimerRunning) {
      // Animate stop/reset buttons forward if timer is running
      controller.forward();

      // Set tea name if timer service has a tea
      if (timerService.currentTea != null)
        teaName = timerService.currentTea.name;
    }

    TextStyle buttonText = TextStyle(
        fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white);

    // Animated builder allows the timer to update on time change
    return AnimatedBuilder(
      // Listen to timerService for changes
      animation: timerService,
      builder: (context, child) {
        return Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Flexible(
                flex: 1,
                child: AnimatedOpacity(
                  opacity: timerService.currentTea != null ? 1.0 : 0.0,
                  duration: Duration(milliseconds: 150),
                  child: Container(
                    padding: EdgeInsets.only(left: 10, right: 10),
                    child: Chip(
                      backgroundColor: mainColor,
                      padding: EdgeInsets.all(10),
                      label: RichText(
                        maxLines: 10,
                        textAlign: TextAlign.center,
                        text: TextSpan(children: <TextSpan>[
                          TextSpan(
                              text: "Brewing: ",
                              style: TextStyle(
                                  fontSize: 28, fontWeight: FontWeight.bold)),
                          TextSpan(
                              text: teaName, style: TextStyle(fontSize: 24))
                        ]),
                      ),
                    ),
                  ),
                ),
              ),
              Flexible(
                flex: 2,
                child: CircleAvatar(
                    radius: 120,
                    backgroundColor: Colors.grey[200],
                    child: Stack(
                      children: <Widget>[
                        SizedBox.expand(
                          child: CircularProgressIndicator(
                            strokeWidth: 6,
                            valueColor:
                                AlwaysStoppedAnimation<Color>(mainColor),
                            value: timerService.currentDuration.inSeconds /
                                timerService.initialDuration.inSeconds,
                          ),
                        ),
                        Center(
                            child: TimeDisplay.styled(
                                timerService: timerService,
                                style:
                                    TextStyle(fontSize: 60, color: mainColor)))
                      ],
                    )),
              ),
              Flexible(
                flex: 1,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: <Widget>[
                    RaisedButton(
                      child: Text("+10s", style: buttonText),
                      color: mainColor,
                      shape: CircleBorder(),
                      padding: EdgeInsets.all(25),
                      elevation: 8,
                      onPressed: () {
                        _addTime(10);
                      },
                    ),
                    RaisedButton(
                      child: Text("+30s", style: buttonText),
                      color: mainColor,
                      shape: CircleBorder(),
                      padding: EdgeInsets.all(25),
                      elevation: 8,
                      onPressed: () {
                        _addTime(30);
                      },
                    ),
                    RaisedButton(
                      child: Text("+60s", style: buttonText),
                      color: mainColor,
                      shape: CircleBorder(),
                      padding: EdgeInsets.all(25),
                      elevation: 8,
                      onPressed: () {
                        _addTime(60);
                      },
                    ),
                  ],
                ),
              ),
              Flexible(
                flex: 1,
                child: Stack(
                  children: <Widget>[
                    SlideTransition(
                      position: stopButtonAnimation,
                      child: Center(
                        child: RaisedButton(
                          // Button will change if timer is paused
                          child: isTimerPaused
                              ? Text("Resume", style: buttonText)
                              : Text("Stop", style: buttonText),
                          color: mainColor,
                          elevation: isTimerRunning ? 8 : 0,
                          onPressed: _stopButtonPressed,
                        ),
                      ),
                    ),
                    SlideTransition(
                        position: resetButtonAnimation,
                        child: Center(
                          child: RaisedButton(
                            child: Text("Reset", style: buttonText),
                            color: mainColor,
                            elevation: isTimerRunning ? 8 : 0,
                            onPressed: _resetButtonPressed,
                          ),
                        )),
                    // Start button is only present if timer is running
                    !isTimerRunning
                        ? Center(
                            child: RaisedButton(
                                child: Text("Start", style: buttonText),
                                color: mainColor,
                                elevation: 8,
                                onPressed: _startButtonPressed),
                          )
                        : Container(),
                  ],
                ),
              )
            ],
          ),
        );
      },
    );
  }

  // Helper function for start button
  _startButtonPressed() {
    // If there is a time set, start timer
    if (TimerService.of(context).currentDuration != Duration.zero) {
      // Tell timer to start
      TimerService.of(context).start();

      // Animate buttons
      controller.forward();

      setState(() {
        isTimerRunning = true;
        isTimerPaused = false;
        teaName = TimerService.of(context).currentTea.name;
      });
    }
  }

  // Helper function for stop/resume button
  _stopButtonPressed() {
    // Determine whether or not timer is already paused
    if (!isTimerPaused) {
      // If not, pause the timer
      TimerService.of(context).stop();

      setState(() {
        isTimerPaused = true;
      });
    } else {
      // Otherwise resume it
      TimerService.of(context).start();
      setState(() {
        isTimerPaused = false;
      });
    }
  }

  // Helper function for reset button
  _resetButtonPressed() {
    // Tell timer to reset
    TimerService.of(context).reset();

    // Animate buttons
    controller.reverse();
  }

  _addTime(int time) {
    TimerService.of(context).addTime(time);
  }
}
