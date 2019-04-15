import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
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
              Text("brand: ${thisTea.brand}"),
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
