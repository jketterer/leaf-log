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
              thisTea: teaList[index],
              callParentFunction: callParentFunction,
            );
          }),
    );
  }
}

// Custom card to be displayed in a TeaView
class TeaCard extends StatelessWidget {
  final Tea thisTea;
  final Function callParentFunction;

  TeaCard({Key key, @required this.thisTea, @required this.callParentFunction})
      : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
              builder: (context) => DetailPage(
                    thisTea: thisTea,
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
        title: Text(thisTea.name),
        subtitle: Text(thisTea.brand),
        trailing: Text(
          thisTea.rating.toString(),
          style: TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 20,
          ),
        ),
      )),
    );
  }
}
