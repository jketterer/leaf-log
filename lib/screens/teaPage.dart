import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/services/databaseHelper.dart';
import 'package:leaf_log/services/timerService.dart';

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

/* Main page */
class TeaPage extends StatelessWidget {
  final Function callParentFunction;
  final DatabaseHelper helper = DatabaseHelper.instance;

  TeaPage({@required this.callParentFunction});

  @override
  Widget build(BuildContext context) {
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

// TODO Design attractive detail page
class DetailPage extends StatelessWidget {
  final Tea thisTea;
  final Function callParentFunction;

  DetailPage(
      {Key key, @required this.thisTea, @required this.callParentFunction})
      : super(key: key);

  final TextStyle headerStyle =
      TextStyle(fontSize: 26, fontWeight: FontWeight.bold);

  final TextStyle labelStyle = TextStyle(
    fontSize: 22,
  );

  final TextStyle timerStyle =
      TextStyle(fontSize: 20, fontWeight: FontWeight.bold);

  @override
  Widget build(BuildContext context) {
    TimerService timerService = TimerService.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text("Details"),
        actions: <Widget>[
          IconButton(
            icon: Icon(Icons.edit),
            onPressed: () {},
          ),
        ],
      ),
      body: Stack(
        children: <Widget>[
          Column(
            //mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Padding(
                padding: EdgeInsets.fromLTRB(0, 15, 0, 15),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: <Widget>[
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: <Widget>[
                        Text(
                          "Name: ",
                          style: headerStyle,
                        ),
                        Text(
                          "${thisTea.name}",
                          style: labelStyle,
                        )
                      ],
                    ),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: <Widget>[
                        Text(
                          "Type: ",
                          style: headerStyle,
                        ),
                        Text(
                          "${thisTea.type}",
                          style: labelStyle,
                        )
                      ],
                    )
                  ],
                ),
              ),
              Text("Brand: ${thisTea.brand}"),
              Text("Rating: ${thisTea.rating}"),
              Text("Notes: \n\n${thisTea.notes}"),
              RaisedButton(
                color: Colors.lightGreen,
                child: Text("Start Brewing"),
                onPressed: () {
                  callParentFunction(1);
                  timerService.reset();
                  timerService.start(Duration(seconds: thisTea.brewTime));
                  timerService.currentTea = thisTea;
                  Navigator.pop(context);
                },
              ),
              RaisedButton(
                color: Colors.lightGreen,
                child: Text("Delete"),
                onPressed: () {
                  DatabaseHelper helper = DatabaseHelper.instance;
                  helper.deleteTea(thisTea.id);
                  Navigator.pop(context);
                },
              ),
              RaisedButton(
                color: Colors.lightGreen,
                child: Text("Back"),
                onPressed: () {
                  Navigator.pop(context);
                },
              )
            ],
          ),
          Hero(
            tag: "FloatingTimer",
            child: FloatingTimer(
              style: timerStyle,
            ),
          )
        ],
      ),
    );
  }
}
