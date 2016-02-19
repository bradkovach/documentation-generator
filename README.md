# documentation-generator
Generates nice HTML entity documentation from an easy, maintainable JSON descriptor

## Usage
`java -jar docgen.jar <input file json> <output file html>`

example:
 java -jar docgen.jar "c:/users/me/desktop/spec.json" "c:/users/me/desktop/docs.htm"

## Example JSON File
```json
[
{
      "name":"column",
      "description":"Selects a column, by name, from the `src` of data and feeds it to `dst`.",
      "label":"column",
      "attributes":[
         {
            "attribute":"name",
            "required":true,
            "description":"The name of the column in `src` to select."
         },
         {
            "attribute":"rename",
            "required":false,
            "description":"Used when using column shorthand for quick renames.  See example."
         }
      ],
      "children":[
         {
            "label":"operations",
            "description":"A region filled with Operations to perform on the column."
         }
      ],
      "notes":[

      ],
      "examples":[
         {"title": "Renaming Shorthand", "example": "<column name=\"old_name\" rename\"new_name\" />"}
      ],
      "tags":[
         "column"
      ]
   }
]```
