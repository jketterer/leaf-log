import 'package:flutter/material.dart';
import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/screens/teaPage.dart';
import 'package:leaf_log/screens/timer.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  int _indexSelected = 0;
  final List<Widget> _tabs = [TeaPage(), TimerPage()];
  final List<String> _tabTitles = ["My Teas", "Tea Timer"];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_tabTitles[_indexSelected]),
        actions: <Widget>[
          ScopedModelDescendant<TeaModel>(
            builder: (context, child, teaModel) => IconButton(
            icon: Icon(Icons.sort),
            onPressed: () {
              teaModel.sort();
            },
          ),
          )
        ],
      ),
      body: _tabs[_indexSelected],
      bottomNavigationBar: BottomNavigationBar(
        items: <BottomNavigationBarItem>[
          BottomNavigationBarItem(icon: Icon(Icons.book), title: Text("Home")),
          BottomNavigationBarItem(
            icon: Icon(Icons.timer),
            title: Text("Timer"),
          ),
        ],
        currentIndex: _indexSelected,
        onTap: _onTapped,
      ),
      // Only show FAB if the Tea Page is displayed, otherwise display null
      floatingActionButton: _indexSelected == 0 ? FloatingActionButton(
        child: Icon(Icons.add),
        onPressed: () {
          Navigator.push(
              context, MaterialPageRoute(builder: (context) => NewTeaPage()));
        },
      ) : null,
    );
  }

  void _onTapped(index) {
    setState(() {
      _indexSelected = index;
    });
  }
}
