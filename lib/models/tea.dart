class Tea {
  final String name;
  final String type;
  final int rating;

  Tea(this.name, {this.type = "Green", this.rating = 10});

  Tea.full(this.name, this.type, this.rating);
}