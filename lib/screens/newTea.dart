import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:scoped_model/scoped_model.dart';
import 'package:flutter_picker/flutter_picker.dart';
import 'package:leaf_log/models/teaModel.dart';
import 'package:leaf_log/models/tea.dart';

class NewTeaPage extends StatefulWidget {
  @override
  _NewTeaPageState createState() => _NewTeaPageState();
}

class _NewTeaPageState extends State<NewTeaPage> {
  final _formKey = GlobalKey<FormState>();

  final _nameController = TextEditingController();
  final _brandController = TextEditingController();
  final _timeController = TextEditingController();
  final _tempController = TextEditingController();
  final _ratingController = TextEditingController();
  final _notesController = TextEditingController();

  final headerStyle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.bold,
  );

  final List<String> _typeList = [
    'Green',
    'Black',
    'Oolong',
    'White',
    'Herbal',
    'Other'
  ];
  String _selectedType;

  int _minutes, _seconds, _rating;

  void _showTimePicker(BuildContext context) {
    new Picker(
        adapter: NumberPickerAdapter(data: [
          NumberPickerColumn(begin: 0, end: 10),
          NumberPickerColumn(begin: 0, end: 59),
        ]),
        delimiter: [
          PickerDelimiter(
            child: Container(
              width: 20,
              alignment: Alignment.center,
              child: Text(
                ":",
                style: TextStyle(fontSize: 25, fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ],
        hideHeader: true,
        title: Text("Brew Time:"),
        onConfirm: (Picker picker, List value) {
          _minutes = value.first;
          _seconds = value.last;
          setState(() {
            _timeController.text =
                _minutes.toString() + ":" + _seconds.toString().padLeft(2, '0');
          });
        }).showDialog(context);
  }

  void _showRatingPicker(BuildContext context) {
    new Picker(
        adapter: NumberPickerAdapter(data: [
          NumberPickerColumn(begin: 1, end: 10),
        ]),
        hideHeader: true,
        title: Text("Rating:"),
        onConfirm: (Picker picker, List value) {
          print(value);
          _rating = value.single + 1; // I don't know why this is necessary
          setState(() {
            _ratingController.text = _rating.toString();
          });
        }).showDialog(context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Add New Tea"),
      ),
      body: Form(
          key: _formKey,
          child: Container(
            padding: EdgeInsets.fromLTRB(30, 20, 30, 20),
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
                Row(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: <Widget>[
                    Expanded(
                      flex: 3,
                      child: TextFormField(
                        controller: _brandController,
                        decoration: InputDecoration(labelText: "Brand"),
                        validator: (value) {
                          if (value.isEmpty) {
                            return "Please enter some text.";
                          }
                        },
                      ),
                    ),
                    Spacer(
                      flex: 1,
                    ),
                    Expanded(
                        flex: 2,
                        child: DropdownButtonFormField(
                          value: _selectedType,
                          items: _typeList
                              .map<DropdownMenuItem<String>>((String value) {
                            return DropdownMenuItem<String>(
                              value: value,
                              child: Text(value),
                            );
                          }).toList(),
                          onChanged: (String newValue) {
                            setState(() {
                              _selectedType = newValue;
                            });
                          },
                          hint: Text("Type"),
                          validator: (value) {
                            if (value == null) {
                              return "Must choose a type.";
                            }
                          },
                        ))
                  ],
                ),
                Row(
                  children: <Widget>[
                    Expanded(
                      flex: 3,
                      child: GestureDetector(
                        child: AbsorbPointer(
                          child: TextField(
                            controller: _timeController,
                            decoration: InputDecoration(labelText: "Brew Time"),
                          ),
                        ),
                        onTap: () => _showTimePicker(context),
                      ),
                    ),
                    Spacer(),
                    Expanded(
                      flex: 3,
                      child: TextFormField(
                        controller: _tempController,
                        decoration: InputDecoration(labelText: "Temperature"),
                        inputFormatters: [
                          WhitelistingTextInputFormatter.digitsOnly
                        ],
                        validator: (value) {
                          if (value.isEmpty) {
                            return "Please enter some text.";
                          }
                        },
                      ),
                    ),
                    Spacer(),
                    Expanded(
                      flex: 3,
                      child: GestureDetector(
                        child: AbsorbPointer(
                          child: TextField(
                            controller: _ratingController,
                            decoration: InputDecoration(labelText: "Rating"),
                          ),
                        ),
                        onTap: () => _showRatingPicker(context),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding: EdgeInsets.fromLTRB(0, 20, 0, 10),
                  child: Text(
                    "Notes",
                    style: TextStyle(fontSize: 16, color: Colors.black54, decoration: TextDecoration.underline)
                  ),
                ),
                TextFormField(
                  controller: _notesController,
                  decoration: InputDecoration(filled: true),
                  keyboardType: TextInputType.multiline,
                  maxLines: 10,
                ),
                ScopedModelDescendant<TeaModel>(
                    builder: (context, child, teaModel) => Container(
                          padding: EdgeInsets.fromLTRB(0, 40, 0, 0),
                          child: RaisedButton(
                            color: Colors.lightGreen,
                            child: Text("Finish"),
                            onPressed: () {
                              if (_formKey.currentState.validate()) {
                                teaModel.add(Tea(
                                    _nameController.text,
                                    _selectedType,
                                    _brandController.text,
                                    _minutes * 60 + _seconds,
                                    int.parse(_tempController.text),
                                    _rating,
                                    _notesController.text));
                                Navigator.pop(context);
                              }
                            },
                          ),
                        ))
              ],
            ),
          )),
    );
  }
}
