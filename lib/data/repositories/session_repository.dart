import 'package:leaf_log/data/repositories/repository.dart';

import '../../models/brew_session.dart';
import '../sources/api/brew_session_data_source.dart';

class BrewSessionRepository extends Repository {
  BrewSessionRepository({required this.dataSource});

  final BrewSessionDataSource dataSource;
  final List<BrewSession> _sessions = [];

  List<BrewSession> get brewSessions => List.unmodifiable(_sessions);

  Future loadSessions() {
    startLoading();
    return dataSource.loadBrewSessions().then((sessions) {
      _replaceSessions(sessions);
      finishLoading();
    });
  }

  void _replaceSessions(List<BrewSession> newSessions) {
    _sessions.clear();
    _sessions.addAll(newSessions);
  }

  List<BrewSession> getForTea(int teaId) {
    return _sessions.where((session) => session.tea.id == teaId).toList();
  }

  void addSession(BrewSession session) {
    startLoading();
    _sessions.add(session);
    _upsertToDataSource(brewSessions);
  }

  void removeSession(int id) {
    startLoading();
    _sessions.removeWhere((session) => session.id == id);
    _upsertToDataSource(brewSessions);
  }

  void _upsertToDataSource(Iterable<BrewSession> sessions) {
    dataSource
        .upsertBrewSessions(_sessions)
        .then((_) => finishLoading())
        .catchError((err) => "Error saving brew sessions: $err");
  }
}
