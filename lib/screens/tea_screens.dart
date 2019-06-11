import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:leaf_log/services/database_helper.dart';
import 'package:flutter_picker/flutter_picker.dart';
import 'package:leaf_log/models/tea.dart';
import 'package:leaf_log/services/timer_service.dart';

class NewTeaPage extends StatefulWidget {
  @override
  _NewTeaPageState createState() => _NewTeaPageState();
}

class _NewTeaPageState extends State<NewTeaPage> {
  final _formKey = GlobalKey<FormState>();

  // Controllers for the text entry fields
  final _nameController = TextEditingController();
  final _brandController = TextEditingController();
  final _timeController = TextEditingController();
  final _tempController = TextEditingController();
  final _ratingController = TextEditingController();
  final _notesController = TextEditingController();

  // Provides access to the sqflite database
  DatabaseHelper helper = DatabaseHelper.instance;

  // Textstlye for headers
  final headerStyle = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.bold,
  );

  // List of tea types to be chosen from
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

  // Function shows a number picker for brew time
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

  // Function shows a number picker for rating
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
                    Container(
                          padding: EdgeInsets.fromLTRB(0, 40, 0, 0),
                          child: RaisedButton(
                            color: Colors.lightGreen,
                            child: Text("Finish"),
                            onPressed: () {
                              if (_formKey.currentState.validate()) {
                                helper.insertTea(Tea(
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
                        )
              ],
            ),
          )),
    );
  }
}

// TODO Design attractive detail page
// Page that displays details about tea when TeaCard is tapped
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