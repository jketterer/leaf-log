import 'package:leaf_log/services/database_helper.dart';

class Tea {
  int id, rating, brewTime, temperature;
  String name, type, brand, notes;
  DateTime lastBrewed = DateTime(1900);
  int brewCount = 0;

  // Database columns
  static final columns = [
    "id",
    "name",
    "brand",
    "type",
    "rating",
    "time",
    "temp",
    "notes",
    "brewCount",
    "lastBrewed"
  ];

  // Basic constructor
  Tea(this.name, this.type, this.brand, this.brewTime, this.temperature,
      this.rating, this.notes);

  // Constructor for generating teas from database tables
  Tea.fromTable(
      this.id,
      this.name,
      this.brand,
      this.type,
      this.rating,
      this.brewTime,
      this.temperature,
      this.notes,
      this.brewCount,
      this.lastBrewed);

  // Updates brewCount and lastBrewed when user brews the tea
  void brew() {
    brewCount++;
    lastBrewed = DateTime.now();
    
    DatabaseHelper helper = DatabaseHelper.instance;
    helper.updateTea(this);
  }

  // Converts Tea object to map
  Map toMap() {
    Map map = <String, dynamic>{
      "id": id,
      "name": name,
      "brand": brand,
      "type": type,
      "rating": rating,
      "time": brewTime,
      "temp": temperature,
      "notes": notes,
      "brewCount": brewCount,
      "lastBrewed": lastBrewed.toIso8601String()
    };

    if (id != null) {
      map["id"] = id;
    }

    return map;
  }

  // Converts map to Tea object
  static fromMap(Map<String, dynamic> map) {
    Tea tea = new Tea.fromTable(
        map["id"],
        map["name"],
        map["brand"],
        map["type"],
        map["rating"],
        map["time"],
        map["temp"],
        map["notes"],
        map["brewCount"],
        DateTime.parse(map["lastBrewed"]));

    return tea;
  }

  // Setters for editing existing teas
  void setName(String newName) {
    this.name = newName;
  }

  void setType(String newType) {
    this.type = newType;
  }

  void setBrand(String newBrand) {
    this.brand = newBrand;
  }

  void setRating(int newRating) {
    this.rating = newRating;
  }

  void setBrewTime(int newBrewTime) {
    this.brewTime = newBrewTime;
  }

  void setTemperature(int newTemp) {
    this.temperature = newTemp;
  }

  void setNotes(String newNotes) {
    this.notes = newNotes;
  }
}
