import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/services/database_helper.dart';
import 'tea_screens.dart';

/* Main page */
class TeaPage extends StatelessWidget {
  // This is required so the app can switch to timer page from anywhere inside TeaPage
  final Function callParentFunction;
  
  // Helper gives us access to the sqflite database
  final DatabaseHelper helper = DatabaseHelper.instance;

  TeaPage({@required this.callParentFunction});

  @override
  Widget build(BuildContext context) {
    // Wrapped in a FutureBuilder to get tea list from sqflite database
    return FutureBuilder<List<Tea>>(
      future: helper.getTeaList(),
      builder: (context, snapshot) {
        if (snapshot.hasError) print(snapshot.error);
        var data = snapshot.data;
        return snapshot.hasData
            ? new TeaView(teaList: data, callParentFunction: callParentFunction,)
            : new Center(child: new CircularProgressIndicator());
      },
    );
  }
}

// Custom listview that displays teas
class TeaView extends StatelessWidget {
  final List<Tea> teaList;
  final Function callParentFunction;

  TeaView({Key key, @required this.teaList, @required this.callParentFunction});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(10),
      child: ListView.builder(
          itemCount: teaList.length,
          itemBuilder: (context, index) {
            return TeaCard(
              tea: teaList[index],
              callParentFunction: callParentFunction,
            );
          }),
    );
  }
}

// Custom card to be displayed in a TeaView
class TeaCard extends StatelessWidget {
  final Tea tea;
  final Function callParentFunction;
  final Function onPressed;

  TeaCard({Key key, @required this.tea, @required this.callParentFunction, this.onPressed})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        if (onPressed != null) {
          onPressed();
        }
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => DetailPage(
                    thisTea: tea,
                    callParentFunction: callParentFunction,
                  )),
        );
      },
      child: Card(
          child: ListTile(
        leading: Icon(
          FontAwesomeIcons.leaf,
          color: Colors.lightGreen,
        ),
        title: Text(tea.name),
        subtitle: Text(tea.brand),
        trailing: Text(
          tea.rating.toString(),
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
          ),
        ),
      )),
    );
  }
}
