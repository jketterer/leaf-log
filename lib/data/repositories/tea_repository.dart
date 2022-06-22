import 'package:collection/collection.dart';
import 'package:flutter/widgets.dart';

import '../../models/tea.dart';
import '../sources/api/tea_data_source.dart';

class TeaRepository extends ChangeNotifier {
  TeaRepository({required this.teaDataSource});

  final TeaDataSource teaDataSource;

  final List<Tea> _teas = [];

  List<Tea> get teas => List.unmodifiable(_teas);

  bool _isLoading = false;

  bool get isLoading => _isLoading;

  Future loadTeas() {
    _isLoading = true;
    return teaDataSource.loadTeas().then((loadedTeas) {
      _replaceTeas(loadedTeas);
      _isLoading = false;
      notifyListeners();
    });
  }

  void _replaceTeas(List<Tea> newTeas) {
    _teas.clear();
    _teas.addAll(newTeas);
  }

  Tea? getById(int id) {
    return _teas.singleWhereOrNull((tea) => tea.id == id);
  }

  List<Tea> getByIds(List<int> ids) {
    return _teas.where((tea) => ids.contains(tea.id)).toList();
  }

  void addTea(Tea tea) {
    _teas.add(tea);
    notifyListeners();
  }

  void removeTea(int id) {
    _teas.removeWhere((tea) => tea.id == id);
    notifyListeners();
  }
}
