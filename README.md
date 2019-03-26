# BigJSONParser
Java library for loading data from JSON files that can be Gbs large.

This library loads JSON data on demand allowing to read huge JSON files in ten.s of seconds.

The library is accompanied by a JSON viewer (com.bigjson.gui package) that demonstrates the use of the library:
 - visualize JSON file as a tree (load child nodes and long strings on demand)
 - validate JSON file
 - search for nodes containing a specified string (search is done only withing object names and values of leaf nodes of types string, number, true, false and null).
