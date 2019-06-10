import 'package:flutter/material.dart';
import 'package:leaf_log/services/timerService.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';

class TimerPage extends StatefulWidget {
  @override
  _TimerPageState createState() => _TimerPageState();
}

class _TimerPageState extends State<TimerPage> {
  @override
  Widget build(BuildContext context) {
    TimerService timerService = TimerService.of(context);
    return AnimatedBuilder(
      animation: timerService,
      builder: (context, child) {
        return Column(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Container(
              child: TimeDisplay.styled(
                timerService: timerService,
                style: TextStyle(fontSize: 80, fontWeight: FontWeight.bold),
              ),
            ),
            timerService.currentTea != null
              ? Text("Brewing: ${timerService.currentTea.name}")
              : Text(""),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  child: Text("+10s"),
                  color: Colors.lightGreen,
                  shape: CircleBorder(),
                  padding: EdgeInsets.all(25),
                  elevation: 3,
                  onPressed: () {timerService.addTime(10);},
                ),
                RaisedButton(
                  child: Text("+30s"),
                  color: Colors.lightGreen,
                  shape: CircleBorder(),
                  padding: EdgeInsets.all(25),
                  onPressed: () {timerService.addTime(30);},
                ),
                RaisedButton(
                  child: Text("+60s"),
                  color: Colors.lightGreen,
                  shape: CircleBorder(),
                  padding: EdgeInsets.all(25),
                  onPressed: () {timerService.addTime(60);},
                )
              ],
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: <Widget>[
                RaisedButton(
                  child: Text("Reset"),
                  color: Colors.lightGreen,
                  onPressed: () {timerService.reset();},
                ),
                RaisedButton(
                  child: Text("Start"),
                  color: Colors.lightGreen,
                  onPressed: () {timerService.start();},
                )
              ],
            ),
          ],
        );
      },
    );
  }
}
