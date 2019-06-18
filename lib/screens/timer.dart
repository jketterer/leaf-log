import 'package:flutter/material.dart';
import 'package:leaf_log/services/timer_service.dart';

class TimerPage extends StatefulWidget {
  @override
  _TimerPageState createState() => _TimerPageState();
}

class _TimerPageState extends State<TimerPage> with TickerProviderStateMixin {
  @override
  Widget build(BuildContext context) {
    // Get access to app-wide timer service
    TimerService timerService = TimerService.of(context);

    TextStyle brewStyle = TextStyle(
        fontSize: 24, fontWeight: FontWeight.bold, fontStyle: FontStyle.italic);

    // Animated builder allows the timer to update on time change
    return Column(
      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
      children: <Widget>[
        AnimatedBuilder(
            // Listen to timerService for updates
            animation: timerService,
            builder: (context, child) {
              return Column(
                children: <Widget>[
                  Card(
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
                      )),
                ],
              );
            }),
        AnimatedBuilder(
          animation: timerService,
          builder: (context, child) {
            return timerService.currentTea != null
                ? Text("Brewing: ${timerService.currentTea.name}",
                    style: brewStyle)
                : Text("", style: brewStyle);
          },
        ),
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
            ),
          ],
        ),
        TimerButtonRow()
      ],
    );
  }
}

class TimerButtonRow extends StatefulWidget {
  @override
  _TimerButtonRowState createState() => _TimerButtonRowState();
}

class _TimerButtonRowState extends State<TimerButtonRow>
    with SingleTickerProviderStateMixin {
  // Controller for animated buttons
  AnimationController controller;
  // Separate animations for buttons
  Animation<Offset> stopButtonAnimation, resetButtonAnimation;

  // Flags to help us determine state of time
  bool isTimerRunning = false;
  bool isTimerPaused = false;

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
    isTimerRunning = TimerService.of(context).isRunning();

    if (isTimerRunning) {
      controller.forward();
    }

    return Stack(
      children: <Widget>[
        SlideTransition(
          position: stopButtonAnimation,
          child: Center(
            child: RaisedButton(
              // Button will change if timer is paused
              child: isTimerPaused ? Text("Resume") : Text("Stop"),
              color: Colors.lightGreen,
              elevation: isTimerRunning ? 8 : 0,
              onPressed: _stopButtonPressed,
            ),
          ),
        ),
        SlideTransition(
            position: resetButtonAnimation,
            child: Center(
              child: RaisedButton(
                child: Text("Reset"),
                color: Colors.lightGreen,
                elevation: isTimerRunning ? 8 : 0,
                onPressed: _resetButtonPressed,
              ),
            )),
        // Start button is only present if timer is running
        !isTimerRunning
            ? Center(
                child: RaisedButton(
                    child: Text("Start"),
                    color: Colors.lightGreen,
                    elevation: 8,
                    onPressed: _startButtonPressed),
              )
            : Container(),
      ],
    );
  }

  // Helper function for start button
  _startButtonPressed() {
    // If there is a time set, start timer
    if (TimerService.of(context).currentDuration != Duration.zero) {
      // Tell timer to start
      TimerService.of(context).start();

      setState(() {
        isTimerRunning = true;
        isTimerPaused = false;
      });

      // Animate buttons
      controller.forward();
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
}
