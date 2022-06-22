import '../../../data/sources/api/brew_session_data_source.dart';
import '../../../models/brew_session.dart';
import '../../../models/tea.dart';
import '../../../models/tea_type.dart';

class MockBrewSessionDataSource implements BrewSessionDataSource {
  final List<BrewSession> _sessions = [
    BrewSession.simple(
      Tea.mock(id: 1, name: "Imperial Mojiang Pure Bud Black Tea"),
    ),
    BrewSession.simple(
      Tea.mock(id: 1, name: "Imperial Mojiang Pure Bud Black Tea"),
    ),
    BrewSession.simple(
      Tea.mock(id: 1, name: "Imperial Mojiang Pure Bud Black Tea"),
    ),
    BrewSession.simple(
      Tea.mock(id: 2, name: "Jasmine", type: TeaType.green()),
    ),
    BrewSession.simple(
      Tea.mock(id: 2, name: "Jasmine", type: TeaType.green()),
    ),
  ];

  @override
  Future<List<BrewSession>> loadBrewSessions() async {
    return Future.delayed(
        Duration(seconds: 3), () => List.unmodifiable(_sessions));
  }

  @override
  Future upsertBrewSessions(Iterable<BrewSession> sessions) async {
    _sessions.clear();
    _sessions.addAll(sessions);
  }
}
