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
  TextEditingController _nameController;
  TextEditingController _brandController;
  TextEditingController _timeController;
  TextEditingController _tempController;
  TextEditingController _ratingController;
  TextEditingController _notesController;

  // Provides access to the sqflite database
  DatabaseHelper helper = DatabaseHelper.instance;

  // Initialize text controllers
  @override
  void initState() {
    super.initState();

    _nameController = TextEditingController();
    _brandController = TextEditingController();
    _timeController = TextEditingController();
    _tempController = TextEditingController();
    _ratingController = TextEditingController();
    _notesController = TextEditingController();
  }

  // Free up resources by disposing controllers
  @override
  void dispose() {
    _nameController.dispose();
    _brandController.dispose();
    _timeController.dispose();
    _tempController.dispose();
    _ratingController.dispose();
    _notesController.dispose();

    super.dispose();
  }

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
                  inputFormatters: [
                    LengthLimitingTextInputFormatter(25),
                  ],
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
                        inputFormatters: [
                          LengthLimitingTextInputFormatter(25),
                        ],
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
                          child: TextFormField(
                            controller: _timeController,
                            decoration: InputDecoration(labelText: "Brew Time"),
                            validator: (value) {
                              if (value.isEmpty) {
                                return "Required";
                              }
                            },
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
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            WhitelistingTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(3)
                          ],
                          validator: (value) {
                            if (value.isEmpty) {
                              return "Required";
                            }
                          }),
                    ),
                    Spacer(),
                    Expanded(
                      flex: 3,
                      child: GestureDetector(
                        child: AbsorbPointer(
                          child: TextFormField(
                              controller: _ratingController,
                              decoration: InputDecoration(labelText: "Rating"),
                              validator: (value) {
                                if (value.isEmpty) {
                                  return "Required";
                                }
                              }),
                        ),
                        onTap: () => _showRatingPicker(context),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding: EdgeInsets.fromLTRB(0, 20, 0, 10),
                  child: Text("Notes",
                      style: TextStyle(
                          fontSize: 16,
                          color: Colors.black54,
                          decoration: TextDecoration.underline)),
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
                      // Create new tea if form validates
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

class EditTeaPage extends StatefulWidget {
  final Tea tea;

  EditTeaPage({@required this.tea});

  @override
  _EditTeaPageState createState() => _EditTeaPageState(tea);
}

class _EditTeaPageState extends State<EditTeaPage> {
  final _formKey = GlobalKey<FormState>();

  Tea tea;
  _EditTeaPageState(this.tea);

  // Controllers for the text entry fields
  TextEditingController _nameController;
  TextEditingController _brandController;
  TextEditingController _timeController;
  TextEditingController _tempController;
  TextEditingController _ratingController;
  TextEditingController _notesController;

  // Provides access to the sqflite database
  DatabaseHelper helper = DatabaseHelper.instance;

  // Initialize text controllers and set fields to current values
  @override
  void initState() {
    super.initState();

    _nameController = TextEditingController();
    _nameController.text = tea.name;

    _brandController = TextEditingController();
    _brandController.text = tea.brand;

    _selectedType = tea.type;

    _timeController = TextEditingController();
    _minutes = (tea.brewTime / 60).floor();
    _seconds = (tea.brewTime % 60);
    _timeController.text =
        _minutes.toString() + ":" + _seconds.toString().padLeft(2, '0');

    _tempController = TextEditingController();
    _tempController.text = tea.temperature.toString();

    _ratingController = TextEditingController();
    _rating = tea.rating;
    _ratingController.text = tea.rating.toString();

    _notesController = TextEditingController();
    _notesController.text = tea.notes;
  }

  // Free up resources by disposing controllers
  @override
  void dispose() {
    _nameController.dispose();
    _brandController.dispose();
    _timeController.dispose();
    _tempController.dispose();
    _ratingController.dispose();
    _notesController.dispose();

    super.dispose();
  }

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
        title: Text("Edit Tea"),
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
                  inputFormatters: [
                    LengthLimitingTextInputFormatter(25),
                  ],
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
                        inputFormatters: [
                          LengthLimitingTextInputFormatter(25),
                        ],
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
                          child: TextFormField(
                            controller: _timeController,
                            decoration: InputDecoration(labelText: "Brew Time"),
                            validator: (value) {
                              if (value.isEmpty) {
                                return "Required";
                              }
                            },
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
                          keyboardType: TextInputType.number,
                          inputFormatters: [
                            WhitelistingTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(3)
                          ],
                          validator: (value) {
                            if (value.isEmpty) {
                              return "Required";
                            }
                          }),
                    ),
                    Spacer(),
                    Expanded(
                      flex: 3,
                      child: GestureDetector(
                        child: AbsorbPointer(
                          child: TextFormField(
                              controller: _ratingController,
                              decoration: InputDecoration(labelText: "Rating"),
                              validator: (value) {
                                if (value.isEmpty) {
                                  return "Required";
                                }
                              }),
                        ),
                        onTap: () => _showRatingPicker(context),
                      ),
                    ),
                  ],
                ),
                Padding(
                  padding: EdgeInsets.fromLTRB(0, 20, 0, 10),
                  child: Text("Notes",
                      style: TextStyle(
                          fontSize: 16,
                          color: Colors.black54,
                          decoration: TextDecoration.underline)),
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
                      // Set new tea values and update database if form validates
                      if (_formKey.currentState.validate()) {
                        tea.setName(_nameController.text);
                        tea.setType(_selectedType);
                        tea.setBrand(_brandController.text);
                        tea.setBrewTime(_minutes * 60 + _seconds);
                        tea.setTemperature(int.parse(_tempController.text));
                        tea.setRating(_rating);
                        tea.setNotes(_notesController.text);

                        helper.updateTea(tea);
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

// Page that displays details about tea when TeaCard is tapped
class DetailPage extends StatelessWidget {
  final Tea thisTea;
  final Function callParentFunction;

  DetailPage(
      {Key key, @required this.thisTea, @required this.callParentFunction})
      : super(key: key);

  final TextStyle labelStyle =
      TextStyle(fontSize: 24, fontWeight: FontWeight.bold);

  final TextStyle infoStyle = TextStyle(fontSize: 22);

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
            onPressed: () {
              Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (context) => EditTeaPage(tea: thisTea)));
            },
          ),
          IconButton(
            icon: Icon(Icons.delete),
            onPressed: () {
              DatabaseHelper helper = DatabaseHelper.instance;
              helper.deleteTea(thisTea.id);
              Navigator.pop(context);
            },
          ),
        ],
      ),
      body: ListView(
        children: <Widget>[
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              Padding(
                  padding: EdgeInsets.fromLTRB(20, 15, 0, 0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: <Widget>[
                      Flexible(
                        flex: 3,
                        child: Text(
                          thisTea.name,
                          style: TextStyle(
                            fontSize: 40,
                            //fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Flexible(
                          flex: 1,
                          child: Row(
                            children: <Widget>[
                              Text(
                                "${thisTea.rating}",
                                style: TextStyle(
                                    fontSize: 45, fontWeight: FontWeight.bold),
                              ),
                              Text(
                                "/10",
                                style: TextStyle(fontSize: 22),
                              ),
                            ],
                          )),
                    ],
                  )),
              Padding(
                padding: EdgeInsets.fromLTRB(40, 0, 0, 0),
                child: Text(
                  thisTea.brand,
                  style: TextStyle(
                      fontSize: 18,
                      color: Colors.grey[500],
                      fontStyle: FontStyle.italic,
                      fontWeight: FontWeight.bold),
                ),
              ),
              Padding(
                padding: EdgeInsets.fromLTRB(0, 15, 0, 15),
                child: Container(
                  height: 3,
                  color: Colors.grey[300],
                ),
              ),
              Padding(
                padding: EdgeInsets.fromLTRB(20, 0, 20, 0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: <Widget>[
                        Text("Type: ", style: labelStyle),
                        Text(thisTea.type, style: infoStyle)
                      ],
                    ),
                    Padding(padding: EdgeInsets.all(5)),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: <Widget>[
                        Text("Brew Temperature: ", style: labelStyle),
                        Text("${thisTea.temperature}°", style: infoStyle)
                      ],
                    ),
                    Padding(padding: EdgeInsets.all(5)),
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: <Widget>[
                        Text("Brew Time: ", style: labelStyle),
                        Text(_convertTime(thisTea.brewTime), style: infoStyle)
                      ],
                    ),
                    Padding(padding: EdgeInsets.all(10)),
                    Text("Notes:", style: labelStyle),
                    Padding(padding: EdgeInsets.all(5)),
                  ],
                ),
              ),
              Padding(
                  padding: EdgeInsets.fromLTRB(20, 0, 20, 20),
                  child: Container(
                    height: 220,
                    width: 1000,
                    padding: EdgeInsets.all(10),
                    color: Colors.grey[200],
                    child: SingleChildScrollView(
                      child: Text(thisTea.notes),
                    ),
                  )),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: <Widget>[
                  RaisedButton(
                    color: Colors.lightGreen,
                    child: Text("Start Brewing"),
                    onPressed: () {
                      callParentFunction(1);
                      timerService.reset();
                      timerService.start(Duration(seconds: thisTea.brewTime));
                      timerService.currentTea = thisTea;
                      timerService.currentTea.brew();
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

  // Function converts time in seconds to a readable format
  String _convertTime(int time) {
    int minutes = (time / 60).floor();
    int seconds = time % 60;

    return minutes.toString() + ":" + seconds.toString().padLeft(2, '0');
  }
}
