import 'package:flutter/material.dart';
import 'package:flutter_sidekick/flutter_sidekick.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:leaf_log/services/search_delegate.dart';
import 'package:leaf_log/services/timer_service.dart';
import 'package:leaf_log/screens/tea_widgets.dart';
import 'package:leaf_log/screens/tea_screens.dart';
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

  // Controller for the persistent timer display
  SidekickController controller;
  // Style for the persistent timer display
  TextStyle timerStyle = TextStyle(fontSize: 20, fontWeight: FontWeight.bold);

  void initState() {
    super.initState();
    // Inits the sidekick controller
    controller =
        SidekickController(vsync: this, duration: Duration(seconds: 1));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        // Title is set based on selected page
        title: Text(_tabTitles[_indexSelected]),
        actions: <Widget>[
          // Display search and sort icons only if user is on tea list page
          _indexSelected == 0
              ? IconButton(
                  icon: Icon(Icons.search),
                  onPressed: () {
                    // Opens a search page with functions specified by CustomSearchDelegate
                    showSearch(
                        context: context,
                        delegate: CustomSearchDelegate(
                            callParentFunction: _changeTab));
                  },
                )
              : Container(),
          _indexSelected == 0
              // Displays sort button
              ? PopupMenuButton(
                onSelected: (result) {
                  setState(() {
                    _setSortMethod(result);
                  });
                },
                icon: Icon(Icons.sort),
                itemBuilder: (BuildContext context) => <PopupMenuEntry>[
                  const PopupMenuItem(
                    value: "rating",
                    child: Text("Sort by rating"),
                  ),
                  const PopupMenuItem(
                    value: "type",
                    child: Text("Sort by type"),
                  ),
                  const PopupMenuItem(
                    value: "frequent",
                    child: Text("Sort by most brewed"),
                  ),
                  const PopupMenuItem(
                    value: "recent",
                    child: Text("Sort by recently brewed"),
                  )
                ],
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

      // Displays two icons in the bottom navigation bar; home and timer
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

  // Function actively changes _indexSelected when user changes tabs
  void _changeTab(index) {
    setState(() {
      _indexSelected = index;
    });
  }

  // Sets shared preferences key to passed string
  _setSortMethod(String sortMethod) async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    setState(() {
      prefs.setString("sortMethod", sortMethod);
    });
  }
}
