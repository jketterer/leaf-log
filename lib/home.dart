import 'package:flutter/material.dart';
import 'package:scoped_model/scoped_model.dart';
import 'package:flutter_sidekick/flutter_sidekick.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/services/timerService.dart';
import 'package:leaf_log/screens/teaPage.dart';
import 'package:leaf_log/screens/newTea.dart';
import 'package:leaf_log/screens/timer.dart';

// HomePage is actually the frame that is present in most of the app
class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> with TickerProviderStateMixin {
  // These are for switching between the tea list page and the timer page
  int _indexSelected = 0;
  final List<String> _tabTitles = ["My Teas", "Tea Timer"];

  SidekickController controller;

  TextStyle timerStyle = TextStyle(fontSize: 20, fontWeight: FontWeight.bold);

  void initState() {
    super.initState();
    controller =
        SidekickController(vsync: this, duration: Duration(seconds: 1));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_tabTitles[_indexSelected]),
        actions: <Widget>[
          _indexSelected == 0
              ? IconButton(
                  icon: Icon(Icons.search),
                  onPressed: () {},
                )
              : Container(),
          _indexSelected == 0
              ? ScopedModelDescendant<TeaModel>(
                  builder: (context, child, teaModel) => IconButton(
                        icon: Icon(Icons.sort),
                        onPressed: () {
                          teaModel.sort();
                        },
                      ),
                )
              : Container(),
          // IconButton(
          //   icon: Icon(Icons.settings),
          //   onPressed: () {},
          // ),
        ],
      ),

      body: Stack(
        children: <Widget>[
          // This if statement controls which page is currently being displayed
          _indexSelected == 0
              ? TeaPage(
                  callParentFunction: _changeTab,
                )
              : TimerPage(),

          // This if statement displays the floating timer if user is not on the timer page
          _indexSelected == 0
              ? Hero(
                  tag: "FloatingTimer",
                  child: FloatingTimer(
                    style: timerStyle,
                  ),
                )
              : Container()
        ],
      ),

      bottomNavigationBar: BottomNavigationBar(
        items: <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.book), title: Text("Home")),
          BottomNavigationBarItem(
            icon: Icon(Icons.timer),
            title: Text("Timer"),
          ),
        ],
        currentIndex: _indexSelected,
        onTap: _changeTab,
      ),

      // Only show FAB if the Tea Page is displayed, otherwise display null
      floatingActionButton: _indexSelected == 0
          ? FloatingActionButton(
              child: Icon(Icons.add),
              onPressed: () {
                Navigator.push(context,
                    MaterialPageRoute(builder: (context) => NewTeaPage()));
              },
            )
          : null,
    );
  }

  void _changeTab(index) {
    setState(() {
      _indexSelected = index;
    });
  }
}
