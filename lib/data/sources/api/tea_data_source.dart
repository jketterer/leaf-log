import '../../../models/tea.dart';

abstract class TeaDataSource {
  Future<List<Tea>> loadTeas();

  Future<int> upsertTea(Tea tea);

  Future deleteTea(int id);
}
