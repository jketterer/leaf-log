import 'package:flutter/material.dart';
import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/models/tea.dart';

// class EntryForm extends StatelessWidget {
//   final headerStyle = TextStyle(
//     fontSize: 16,
//     fontWeight: FontWeight.bold,
//   );

//   final String title;
//   final TextEditingController textController;

//   EntryForm({this.title, this.textController});

//   @override
//   Widget build(BuildContext context) {
//     return Container(
//         padding: EdgeInsets.fromLTRB(0, 16, 0, 16),
//         child: Form(
//           child: Column(
//             crossAxisAlignment: CrossAxisAlignment.start,
//             children: <Widget>[
//               Text(
//                 title,
//                 style: headerStyle,
//               ),
//               TextFormField(
//                 controller: textController,
//                 validator: (value) {
//                   if (value.isEmpty) {
//                     return "Please enter some text.";
//                   }
//                 },
//               )
//             ],
//           ),
//         ));
//   }
// }

// // TODO Design attractive new tea page
// class NewTeaPage extends StatefulWidget {
//   @override
//   _NewTeaPageState createState() => _NewTeaPageState();
// }

// class _NewTeaPageState extends State<NewTeaPage> {
//   final _nameController = TextEditingController();
//   final _typeController = TextEditingController();
//   final _brandController = TextEditingController();
//   final _timeController = TextEditingController();
//   final _ratingController = TextEditingController();

//   final _formKey = GlobalKey<FormState>();

//   final headerStyle = TextStyle(
//     fontSize: 16,
//     fontWeight: FontWeight.bold,
//   );

//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(
//         title: Text("Add New Tea"),
//       ),
//       body: Container(
//           padding: EdgeInsets.all(20),
//           child: Form(
//               key: _formKey,
//               child: ListView(
//                 children: <Widget>[
//                   Column(
//                     crossAxisAlignment: CrossAxisAlignment.start,
//                     //mainAxisAlignment: MainAxisAlignment.spaceEvenly,
//                     children: <Widget>[
//                       EntryForm(
//                           title: "Tea Name:", textController: _nameController),
//                       EntryForm(
//                           title: "Type:", textController: _typeController),
//                       EntryForm(
//                           title: "Brand:", textController: _brandController),
//                       EntryForm(
//                           title: "Brew Time:", textController: _timeController),
//                       EntryForm(
//                           title: "Rating:", textController: _ratingController),
//                       ScopedModelDescendant<TeaModel>(
//                         builder: (context, child, teaModel) => RaisedButton(
//                               color: Colors.lightGreen,
//                               child: Text("Finish"),
//                               onPressed: () {
//                                 if (_formKey.currentState.validate()) {
//                                   teaModel.add(Tea(
//                                       _nameController.text,
//                                       _typeController.text,
//                                       _brandController.text,
//                                       int.parse(_timeController.text),
//                                       int.parse(_ratingController.text)));
//                                   Navigator.pop(context);
//                                 }
//                               },
//                             ),
//                       )
//                     ],
//                   ),
//                 ],
//               ))),
//     );
//   }
// }

class NewTeaPage extends StatefulWidget {
  @override
  _NewTeaPageState createState() => _NewTeaPageState();
}

class _NewTeaPageState extends State<NewTeaPage> {
  final _formKey = GlobalKey<FormState>();

  final _nameController = TextEditingController();
  final _typeController = TextEditingController();
  final _brandController = TextEditingController();
  final _timeController = TextEditingController();
  final _ratingController = TextEditingController();

  final headerStyle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.bold,
  );

  String selectedType = "Green";

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Tea"),
      ),
      body: Form(
          key: _formKey,
          child: Container(
            padding: EdgeInsets.all(20),
            child: ListView(
              children: <Widget>[
                TextFormField(
                  controller: _nameController,
                  decoration: InputDecoration(labelText: "Name"),
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Name must not be blank.";
                    }
                  },
                ),
                TextFormField(
                  controller: _brandController,
                  decoration: InputDecoration(labelText: "Brand"),
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Please enter some text.";
                    }
                  },
                ),
                TextFormField(
                  controller: _typeController,
                  decoration: InputDecoration(labelText: "Type"),
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Please enter some text.";
                    }
                  },
                ),
                TextFormField(
                  controller: _timeController,
                  decoration: InputDecoration(labelText: "Brew Time"),
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Please enter some text.";
                    }
                  },
                ),
                TextFormField(
                  controller: _ratingController,
                  decoration: InputDecoration(labelText: "Rating"),
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Please enter some text.";
                    }
                  },
                ),
                ScopedModelDescendant<TeaModel>(
                  builder: (context, child, teaModel) => RaisedButton(
                        color: Colors.lightGreen,
                        child: Text("Finish"),
                        onPressed: () {
                          if (_formKey.currentState.validate()) {
                            teaModel.add(Tea(
                                _nameController.text,
                                _typeController.text,
                                _brandController.text,
                                int.parse(_timeController.text),
                                int.parse(_ratingController.text)));
                            Navigator.pop(context);
                          }
                        },
                      ),
                )
              ],
            ),
          )),
    );
  }
}
