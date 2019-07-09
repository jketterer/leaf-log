import 'dart:async';
import 'dart:io';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path_provider/path_provider.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:leaf_log/models/tea.dart';

class DatabaseHelper {
  // Name of actual file on device
  static final _databaseName = "Tea_Database.db";
  // Version should increment when schema changes
  static final _databaseVersion = 1;

  // Name of table in database
  final String teaTable = 'teas';

  // make this a singleton class
  DatabaseHelper._privateConstructor();
  static final DatabaseHelper instance = DatabaseHelper._privateConstructor();

  // Only allow a single open connection to the database
  static Database _database;
  Future<Database> get database async {
    if (_database != null) return _database;
    _database = await _initDatabase();
    return _database;
  }

  // open the database
  _initDatabase() async {
    // The path provider plugin gets the right directory for Android or iOS
    Directory documentsDirectory = await getApplicationDocumentsDirectory();
    String path = join(documentsDirectory.path, _databaseName);
    // Open the database
    return await openDatabase(path,
        version: _databaseVersion, onCreate: _create);
  }

  // Creates database with an SQL string
  Future _create(Database db, int version) async {
    await db.execute("""
            CREATE TABLE $teaTable (
              id INTEGER PRIMARY KEY,
              name TEXT NOT NULL,
              brand TEXT NOT NULL,
              type TEXT NOT NULL,
              rating INTEGER NOT NULL,
              time INTEGER NOT NULL,
              temp INTEGER NOT NULL,
              notes TEXT,
              brewCount INTEGER NOT NULL,
              lastBrewed TEXT NOT NULL
            ) 
          """);

    insertTea(Tea("Green Tea", "Green", "Generic", 210, 175, 5,
        "This is a generic entry for green tea. Green tea is usually brewed at 160-180°F for 2-3 minutes."));
    insertTea(Tea("Black Tea", "Black", "Generic", 300, 212, 5,
        "This is a generic entry for oolong tea. Oolong tea is usually brewed at boiling temp for 3-5 minutes."));
    insertTea(Tea("Oolong Tea", "Oolong", "Generic", 120, 200, 5,
        "This is a generic entry for oolong tea. Oolong tea is usually brewed at 185-200°F for 1-5 minutes."));
  }

  // Inserts a tea into the database and returns the row id
  Future<int> insertTea(Tea tea) async {
    Database db = await database;

    int id = await db.insert(teaTable, tea.toMap());

    return id;
  }

  // Updates a tea entry in the database and returns the row id
  Future<int> updateTea(Tea tea) async {
    Database db = await database;

    return await db
        .update(teaTable, tea.toMap(), where: "id = ?", whereArgs: [tea.id]);
  }

  // Deletes tea from the database and returns the row id
  Future<int> deleteTea(int id) async {
    Database db = await database;

    return await db.delete(teaTable, where: "id = ?", whereArgs: [id]);
  }

  // Queries the database and returns a tea from its row id
  Future<Tea> fetchTea(int id) async {
    Database db = await database;

    List<Map> results = await db.query(teaTable,
        columns: Tea.columns, where: "id = ?", whereArgs: [id]);

    Tea tea = Tea.fromMap(results[0]);
    return tea;
  }

  // Searches tea list for any tea with a name containing query and returns them
  Future<List<Tea>> searchTeaList(String query) async {
    Database db = await database;

    List<Map> results =
        await db.rawQuery("SELECT * FROM $teaTable WHERE name LIKE '%$query%'");

    List<Tea> _teaList = List();
    results.forEach((result) {
      Tea tea = Tea.fromMap(result);
      _teaList.add(tea);
    });

    return _teaList;
  }

  // Queries the database and returns a list of all teas within the database
  Future<List<Tea>> getTeaList() async {
    Database db = await database;

    List<Map> results = await db.query(teaTable);

    List<Tea> _teaList = List();
    results.forEach((result) {
      Tea tea = Tea.fromMap(result);
      _teaList.add(tea);
    });

    // Get sort method from shared preferences
    SharedPreferences prefs = await SharedPreferences.getInstance();
    String sortMethod = prefs.get("sortMethod" ?? "");

    // Sort tea list based on desired sort method
    if (sortMethod != "") {
      if (sortMethod == "type") {
        _teaList = sortByType(_teaList);
      } else if (sortMethod == "rating") {
        _teaList = sortByRating(_teaList);
      } else if (sortMethod == "frequent") {
        _teaList = sortByBrewCount(_teaList);
      } else if (sortMethod == "recent") {
        _teaList = sortByLastBrewed(_teaList);
      }
    }

    return _teaList;
  }

  // Returns a list of all teas sorted by tea type
  List<Tea> sortByType(List<Tea> teaList) {
    List<Tea> returnList = List<Tea>();

    List<String> types = [
      "Green",
      "Black",
      "Oolong",
      "White",
      "Herbal",
      "Other"
    ];

    types.forEach((type) {
      teaList.forEach((tea) {
        if (tea.type == type) {
          returnList.add(tea);
        }
      });
    });

    return returnList;
  }

  // Returns a list of all teas sorted by tea rating
  List<Tea> sortByRating(List<Tea> teaList) {
    List<Tea> returnList = List<Tea>();

    for (int i = 10; i > 0; i--) {
      teaList.forEach((tea) {
        if (tea.rating == i) {
          returnList.add(tea);
        }
      });
    }

    return returnList;
  }

  // Returns a list of all teas sorted by most frequently brewed
  List<Tea> sortByBrewCount(List<Tea> teaList) {
    List<Tea> returnList = List<Tea>();

    // Find the tea with the highest brew count
    Tea highest = teaList[0];
    teaList.forEach((tea) {
      if (tea.brewCount > highest.brewCount) {
        highest = tea;
      }
    });

    // Add teas in descending order of brew count
    for (int i = highest.brewCount; i >= 0; i--) {
      teaList.forEach((tea) {
        if (tea.brewCount == i) {
          returnList.add(tea);
        }
      });
    }

    return returnList;
  }

  // Returns a list of all teas sorted by most recently brewed
  List<Tea> sortByLastBrewed(List<Tea> teaList) {
    List<Tea> returnList = List<Tea>();

    returnList.add(teaList[0]);

    // Shuffle sort
    for (int i = 1; i < teaList.length; i++) {
      for (int j = 0; j < returnList.length; j++) {
        // If tea was brewed more recently, add it before the current tea
        if (teaList[i].lastBrewed.isAfter(returnList[j].lastBrewed)) {
          returnList.insert(j, teaList[i]);
          break;
        }
      }

      // If tea was brewed before all other teas in the list, add it to the end
      if (!returnList.contains(teaList[i])) {
        returnList.add(teaList[i]);
      }
    }

    return returnList;
  }
}
