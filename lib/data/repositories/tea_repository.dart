import 'package:collection/collection.dart';
import 'package:leaf_log/data/repositories/repository.dart';

import '../../models/tea.dart';
import '../sources/api/tea_data_source.dart';

class TeaRepository extends Repository {
  TeaRepository({required this.teaDataSource});

  final TeaDataSource teaDataSource;
  final List<Tea> _teas = [];

  List<Tea> get teas => List.unmodifiable(_teas);

  Future loadTeas() {
    startLoading();
    return teaDataSource.loadTeas().then((loadedTeas) {
      _replaceTeas(loadedTeas);
      finishLoading();
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
    startLoading(message: "Saving tea...");
    teaDataSource
        .upsertTea(tea)
        .then((newId) => _teas.add(tea.copyWith(id: newId)))
        // .catchError((err) => print("Error upserting tea: $err")) // TODO?
        .whenComplete(() => finishLoading());
  }

  void removeTea(int id) {
    startLoading(message: "Removing tea...");
    _teas.removeWhere((tea) => tea.id == id);
    teaDataSource
        .deleteTea(id)
        .catchError((err) => print("Error deleting tea: $err"))
        .whenComplete(() => finishLoading());
  }
}
