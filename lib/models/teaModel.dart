import 'package:scoped_model/scoped_model.dart';
import 'package:leaf_log/models/tea.dart';
import 'dart:collection';

class TeaModel extends Model {

  List<Tea> _teaList = [Tea("Test Tea", "Harney", "Green", 100, 90, 5, "")];

  TeaModel() {
    //_teaList.add(Tea("Test Tea", "Harney", "Green", 5, 90, 100, ""));
  }

  UnmodifiableListView<Tea> get teaList {
    //updateTeaList();
    return UnmodifiableListView(_teaList);
  }

  add(Tea tea) async{
    _teaList.add(tea);
    notifyListeners();
  }

  void sort() {
    _teaList.sort();
    notifyListeners();
  }
}