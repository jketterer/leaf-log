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
      controller.forward();
    }

    TextStyle buttonText = TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.white);

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
              timerService.currentTea != null
                  ? Chip(
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
                              text: "${timerService.currentTea.name}",
                              style: TextStyle(fontSize: 24))
                        ]),
                      ),
                    )
                  : Container(),
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
              Stack(
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
              )
              // timerService.currentTea != null
              //     ? Chip(
              //         backgroundColor: _typeColors[timerService.currentTea.type],
              //         padding: EdgeInsets.all(10),
              //         label: RichText(
              //           maxLines: 10,
              //           text: TextSpan(children: <TextSpan>[
              //             TextSpan(
              //                 text: "Brewing: ",
              //                 style: TextStyle(
              //                     fontSize: 28, fontWeight: FontWeight.bold)),
              //             TextSpan(
              //                 text: "${timerService.currentTea.name}",
              //                 style: TextStyle(fontSize: 24))
              //           ]),
              //         ),
              //       )
              //     : Text("", style: brewStyle),
              // Card(
              //     color: timerService.currentTea != null
              //         ? _typeColors[timerService.currentTea.type]
              //         : Colors.lightGreen,
              //     margin: EdgeInsets.only(left: 30, right: 30),
              //     child: Padding(
              //         padding: EdgeInsets.fromLTRB(10, 30, 10, 30),
              //         child: Column(
              //           //mainAxisAlignment: MainAxisAlignment.spaceBetween,
              //           children: <Widget>[
              //             Row(
              //               mainAxisAlignment: MainAxisAlignment.spaceBetween,
              //               children: <Widget>[
              //                 RaisedButton(
              //                   child: Text("+10s"),
              //                   color: Colors.grey[200],
              //                   shape: CircleBorder(),
              //                   padding: EdgeInsets.all(25),
              //                   elevation: 8,
              //                   onPressed: () {
              //                     _addTime(10);
              //                   },
              //                 ),
              //                 RaisedButton(
              //                   child: Text("+30s"),
              //                   color: Colors.grey[200],
              //                   shape: CircleBorder(),
              //                   padding: EdgeInsets.all(25),
              //                   elevation: 8,
              //                   onPressed: () {
              //                     _addTime(30);
              //                   },
              //                 ),
              //                 RaisedButton(
              //                   child: Text("+60s"),
              //                   color: Colors.grey[200],
              //                   shape: CircleBorder(),
              //                   padding: EdgeInsets.all(25),
              //                   elevation: 8,
              //                   onPressed: () {
              //                     _addTime(60);
              //                   },
              //                 ),
              //               ],
              //             ),
              //             Padding(
              //               padding: EdgeInsets.all(20),
              //             ),
              //             Stack(
              //               children: <Widget>[
              //                 SlideTransition(
              //                   position: stopButtonAnimation,
              //                   child: Center(
              //                     child: RaisedButton(
              //                       // Button will change if timer is paused
              //                       child: isTimerPaused
              //                           ? Text("Resume")
              //                           : Text("Stop"),
              //                       color: Colors.grey[200],
              //                       elevation: isTimerRunning ? 8 : 0,
              //                       onPressed: _stopButtonPressed,
              //                     ),
              //                   ),
              //                 ),
              //                 SlideTransition(
              //                     position: resetButtonAnimation,
              //                     child: Center(
              //                       child: RaisedButton(
              //                         child: Text("Reset"),
              //                         color: Colors.grey[200],
              //                         elevation: isTimerRunning ? 8 : 0,
              //                         onPressed: _resetButtonPressed,
              //                       ),
              //                     )),
              //                 // Start button is only present if timer is running
              //                 !isTimerRunning
              //                     ? Center(
              //                         child: RaisedButton(
              //                             child: Text("Start"),
              //                             color: Colors.grey[200],
              //                             elevation: 8,
              //                             onPressed: _startButtonPressed),
              //                       )
              //                     : Container(),
              //               ],
              //             )
              //           ],
              //         ))),
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

  _addTime(int time) {
    TimerService.of(context).addTime(time);
  }
}
