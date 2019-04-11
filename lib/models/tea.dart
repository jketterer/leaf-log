class Tea implements Comparable {
  final String name;
  final String type;
  final String vendor;
  final int rating;

  Tea(this.name,
      {this.type = "Green", this.vendor = "Harney and Sons", this.rating = 10});

  Tea.full(this.name, this.type, this.vendor, this.rating);

  @override
  int compareTo(other) {
    return other.rating - this.rating;
  }
}
