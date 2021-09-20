# redex
redacted-export: export table data, and redact fields as you go

## how
Use gradle to build the redex jar
```$ ./gradlew clean jar```

This will create a snapshot jar in `build/libs/redex-SNAPSHOT.jar`

Copy that jar as `redex.jar` nd teh batch file `bin/redex.bat` into the same directory.

Create a json config file containing the tables, and field fields that you would like to react. eg:
```json
{ "name": "myDatabase",
  "jdbcUrl": "jdbc:sqlserver://SomeServer:50435;databaseName=myDB;integratedSecurity=true",
  "tables": [
    {"name":  "People",
     "query": "select top 10 from those_people", 
     "redact": ["Surname", "PhoneNumber"]},
    {"name":  "Places",
     "query":  "places"}
  ]
}
```

The above configuration will create a spreadsheet named `myDatabase.xlsx`, exporting the query for `People` (2 fields redacted), 
and `Places`. The sheets in the workbook will be named the value you give `name`.

```
$ redex.bat config.json c:/mydir
```

## Note
if you use are using MS integrated security, please be sure to copy the `mssql-jdbc_auth-8.4.1.x64.dll` file into the 
same directory as the `redex.bat` file. 