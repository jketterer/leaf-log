import 'package:flutter/material.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:scoped_model/scoped_model.dart';

class TeaCard extends StatelessWidget {
  final Tea thisTea;

  TeaCard({Key key, @required this.thisTea}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => DetailPage(thisTea: thisTea)),
        );
      },
      child: Card(
          child: ListTile(
        leading: Icon(
          Icons.local_cafe,
          color: Colors.lightGreen,
        ),
        title: Text(thisTea.name),
        subtitle: Row(
          children: <Widget>[
            Icon(Icons.star, size: 15),
            Icon(Icons.star, size: 15),
            Icon(Icons.star, size: 15),
            Icon(Icons.star_half, size: 15),
            Icon(Icons.star_border, size: 15)
          ],
        ),
      )),
    );
  }
}

class TeaView extends StatefulWidget {
  final List<Tea> teaList;

  TeaView({Key key, @required this.teaList});

  TeaViewState createState() => TeaViewState(teaList: teaList);
}

class TeaViewState extends State<TeaView> {
  List<Tea> teaList;

  TeaViewState({Key key, @required this.teaList});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.all(10),
      child: ListView.builder(
          itemCount: teaList.length,
          itemBuilder: (context, index) {
            return TeaCard(
              thisTea: teaList[index],
            );
          }),
    );
  }
}

/* Main page */
class TeaPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return ScopedModelDescendant<TeaModel>(
        builder: (context, child, teaModel) => TeaView(
              teaList: teaModel.teaList,
            ),
      );
  }
}

// TODO Design attractive detail page
class DetailPage extends StatelessWidget {
  final Tea thisTea;

  DetailPage({Key key, @required this.thisTea}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text("Details"),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text("Name: ${thisTea.name}"),
              Text("Type: ${thisTea.type}"),
              Text("Rating: ${thisTea.rating}"),
              RaisedButton(
                color: Colors.lightGreen,
                child: Text("Back"),
                onPressed: () {
                  Navigator.pop(context);
                },
              )
            ],
          ),
        ));
  }
}

// TODO Design attractive new tea page
class NewTeaPage extends StatelessWidget {
  final nameController = TextEditingController();
  final typeController = TextEditingController();
  final ratingController = TextEditingController();

  final headerStyle = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
  );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Tea"),
      ),
      body: Container(
        padding: EdgeInsets.all(20),
        child: ListView(
          children: <Widget>[Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          //mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: <Widget>[
            Container(
              padding: EdgeInsets.fromLTRB(0, 16, 0, 0),
              child: Text(
                "Tea Name:",
                style: headerStyle,
              ),
            ),
            Container(
                padding: EdgeInsets.fromLTRB(0, 16, 0, 16),
                width: 200,
                child: TextField(
                  controller: nameController,
                )),
            Text(
              "Type:",
              style: headerStyle,
            ),
            Container(
                padding: EdgeInsets.fromLTRB(0, 16, 0, 16),
                width: 200,
                child: TextField(
                  controller: typeController,
                )),
            Text(
              "Rating:",
              style: headerStyle,
            ),
            Container(
                padding: EdgeInsets.fromLTRB(0, 16, 0, 16),
                width: 200,
                child: TextField(
                  controller: ratingController,
                )),
            ScopedModelDescendant<TeaModel>(
              builder: (context, child, teaModel) => RaisedButton(
                    color: Colors.lightGreen,
                    child: Text("Finish"),
                    onPressed: () {
                      teaModel.add(Tea.full(
                          nameController.text,
                          typeController.text,
                          int.parse(ratingController.text)));
                      Navigator.pop(context);
                    },
                  ),
            )
          ],
        ),],
        )
      ),
    );
  }
}
