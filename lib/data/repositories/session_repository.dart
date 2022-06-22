import 'package:flutter/widgets.dart';

import '../../models/brew_session.dart';
import '../sources/api/brew_session_data_source.dart';

class BrewSessionRepository extends ChangeNotifier {
  BrewSessionRepository({required this.dataSource});

  final BrewSessionDataSource dataSource;

  final List<BrewSession> _sessions = [];

  List<BrewSession> get brewSessions => List.unmodifiable(_sessions);

  bool _isLoading = false;

  get isLoading => _isLoading;

  Future loadSessions() {
    _isLoading = true;
    return dataSource.loadBrewSessions().then((sessions) {
      _replaceSessions(sessions);
      _isLoading = false;
      notifyListeners();
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
    _sessions.add(session);
    notifyListeners();
  }

  void removeSession(int id) {
    _sessions.removeWhere((session) => session.id == id);
    notifyListeners();
  }
}
