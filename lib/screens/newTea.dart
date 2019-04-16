import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/models/tea.dart';

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
                  inputFormatters: [WhitelistingTextInputFormatter.digitsOnly],
                  validator: (value) {
                    if (value.isEmpty) {
                      return "Please enter some text.";
                    }
                  },
                ),
                TextFormField(
                  controller: _ratingController,
                  decoration: InputDecoration(labelText: "Rating"),
                  inputFormatters: [WhitelistingTextInputFormatter.digitsOnly],
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
