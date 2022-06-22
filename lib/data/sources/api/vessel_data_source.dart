import '../../../models/brewing_vessel.dart';

abstract class BrewingVesselDataSource {
  List<BrewingVessel> loadBrewingVessels();

  void upsertBrewingVessels(Iterable<BrewingVessel> vessels);
}
