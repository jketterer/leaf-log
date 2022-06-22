class Vendor {
  final String name;
  final String? description;

  Vendor(this.name, this.description);

  Vendor copyWith({String? name, String? description}) =>
      Vendor(
          name ?? this.name,
          description ?? this.description
      );
}