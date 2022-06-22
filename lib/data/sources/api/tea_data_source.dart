import '../../../models/tea.dart';

abstract class TeaDataSource {
  Future<List<Tea>> loadTeas();

  Future upsertTeas(Iterable<Tea> teas);
}
