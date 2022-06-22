class BrewingVessel {
  final String name;
  final VesselType type;
  final String? description;

  BrewingVessel(this.name, this.type, this.description);

  BrewingVessel.mock()
      : name = "DefaultVessel",
        type = VesselType.mug,
        description = "Default vessel";

  BrewingVessel copyWith(
      {String? name, VesselType? type, String? description}) =>
      BrewingVessel(
          name ?? this.name,
          type ?? this.type,
          description ?? this.description
      );
}

enum VesselType {
  teapot,
  gaiwan,
  mug,
  cup,
  other,
}
