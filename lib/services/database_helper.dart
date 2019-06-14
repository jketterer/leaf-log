import 'dart:async';
import 'dart:io';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import 'package:path_provider/path_provider.dart';
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
              notes TEXT
            ) 
          """);
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

  Future<List<Tea>> searchTeaList(String query) async {
    Database db = await database;

    List<Map> results = await db.rawQuery("SELECT * FROM $teaTable WHERE name LIKE '%$query%'");

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

    return _teaList;
  }
}
