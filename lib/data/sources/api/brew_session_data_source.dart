import '../../../models/brew_session.dart';

abstract class BrewSessionDataSource {
  Future<List<BrewSession>> loadBrewSessions();

  Future upsertBrewSessions(Iterable<BrewSession> sessions);
}
